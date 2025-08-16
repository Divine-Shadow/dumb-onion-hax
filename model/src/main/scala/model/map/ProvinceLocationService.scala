package com.crib.bills.dom6maps
package model.map

import cats.effect.kernel.Concurrent
import cats.syntax.all.*
import fs2.Stream
import model.ProvinceId

final case class XCell(value: Int) extends AnyVal
final case class YCell(value: Int) extends AnyVal
final case class ProvinceLocation(x: XCell, y: YCell)

object ProvinceLocationService:
  private final case class Acc(
      count: Long,
      sumX: Long,
      sumY: Long,
      sumCosX: Double,
      sumSinX: Double,
      sumCosY: Double,
      sumSinY: Double
  )
  private val emptyAcc = Acc(0L, 0L, 0L, 0.0, 0.0, 0.0, 0.0)

  private final case class State(
      width: Int,
      height: Int,
      wrapH: Boolean,
      wrapV: Boolean,
      accs: Map[ProvinceId, Acc]
  )
  private val emptyState = State(0, 0, false, false, Map.empty)

  def derive[F[_]: Concurrent](directives: Stream[F, MapDirective]): F[Map[ProvinceId, ProvinceLocation]] =
    directives.compile.fold(emptyState)(accumulate).map(finalize)

  def deriveLocationIndex[F[_]: Concurrent](
      directives: Stream[F, MapDirective]
  ): F[Map[ProvinceLocation, ProvinceId]] =
    derive(directives).map { idToLocation =>
      idToLocation.map { case (provinceId, provinceLocation) =>
        provinceLocation -> provinceId
      }
    }

  private def accumulate(state: State, directive: MapDirective): State =
    directive match
      case MapSizePixels(w, h) => state.copy(width = w.value, height = h.value)
      case WrapAround          => state.copy(wrapH = true, wrapV = true)
      case HWrapAround         => state.copy(wrapH = true)
      case VWrapAround         => state.copy(wrapV = true)
      case NoWrapAround        => state.copy(wrapH = false, wrapV = false)
      case ProvincePixels(x, y, len, p) => accumulatePixels(state, x, y, len, p)
      case Pb(x, y, len, p)            => accumulatePixels(state, x, y, len, p)
      case _ => state

  private def updateAcc(acc: Acc, x: Int, y: Int, len: Int, state: State): Acc =
    val count   = acc.count + len
    val sumX    = acc.sumX + len.toLong * x + (len.toLong - 1L) * len.toLong / 2L
    val sumY    = acc.sumY + len.toLong * y
    val centerX = x + (len - 1) / 2.0
    val angleX  = 2.0 * math.Pi * centerX / state.width.toDouble
    val weight  = len.toDouble
    val (cosX, sinX) =
      if state.wrapH then
        (acc.sumCosX + weight * math.cos(angleX), acc.sumSinX + weight * math.sin(angleX))
      else (acc.sumCosX, acc.sumSinX)
    val angleY = 2.0 * math.Pi * y.toDouble / state.height.toDouble
    val (cosY, sinY) =
      if state.wrapV then
        (acc.sumCosY + weight * math.cos(angleY), acc.sumSinY + weight * math.sin(angleY))
      else (acc.sumCosY, acc.sumSinY)
    Acc(count, sumX, sumY, cosX, sinX, cosY, sinY)

  private def accumulatePixels(state: State, x: Int, y: Int, len: Int, p: ProvinceId): State =
    val current = state.accs.getOrElse(p, emptyAcc)
    val updated = updateAcc(current, x, y, len, state)
    state.copy(accs = state.accs.updated(p, updated))

  private def finalize(state: State): Map[ProvinceId, ProvinceLocation] =
    state.accs.map { case (p, acc) =>
      val centroidX =
        if state.wrapH then
          val angle = math.atan2(acc.sumSinX, acc.sumCosX)
          val norm  = if angle < 0 then angle + 2 * math.Pi else angle
          state.width.toDouble * norm / (2 * math.Pi)
        else acc.sumX.toDouble / acc.count.toDouble
      val centroidY =
        if state.wrapV then
          val angle = math.atan2(acc.sumSinY, acc.sumCosY)
          val norm  = if angle < 0 then angle + 2 * math.Pi else angle
          state.height.toDouble * norm / (2 * math.Pi)
        else acc.sumY.toDouble / acc.count.toDouble
      val xCell = math.floor(centroidX / 256.0).toInt
      val yCell = math.floor(centroidY / 160.0).toInt
      p -> ProvinceLocation(XCell(xCell), YCell(yCell))
    }
