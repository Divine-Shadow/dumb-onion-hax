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
      accs: Map[ProvinceId, Acc]
  )
  private val emptyState = State(0, 0, Map.empty)

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
      case Pb(x, y, len, p)    => accumulatePixels(state, x, y, len, p)
      case _                   => state

  private def updateAcc(acc: Acc, x: Int, y: Int, len: Int, state: State): Acc =
    val count   = acc.count + len
    val sumX    = acc.sumX + len.toLong * x + (len.toLong - 1L) * len.toLong / 2L
    val sumY    = acc.sumY + len.toLong * y
    val centerX = x + (len - 1) / 2.0
    val angleX  = 2.0 * math.Pi * centerX / state.width.toDouble
    val weight  = len.toDouble
    val cosX    = acc.sumCosX + weight * math.cos(angleX)
    val sinX    = acc.sumSinX + weight * math.sin(angleX)
    val angleY  = 2.0 * math.Pi * y.toDouble / state.height.toDouble
    val cosY    = acc.sumCosY + weight * math.cos(angleY)
    val sinY    = acc.sumSinY + weight * math.sin(angleY)
    Acc(count, sumX, sumY, cosX, sinX, cosY, sinY)

  private def accumulatePixels(state: State, x: Int, y: Int, len: Int, p: ProvinceId): State =
    val current = state.accs.getOrElse(p, emptyAcc)
    val updated = updateAcc(current, x, y, len, state)
    state.copy(accs = state.accs.updated(p, updated))

  private def finalize(state: State): Map[ProvinceId, ProvinceLocation] =
    state.accs.map { case (p, acc) =>
      val angleX   = math.atan2(acc.sumSinX, acc.sumCosX)
      val normX    = if angleX < 0 then angleX + 2 * math.Pi else angleX
      val centroidX = state.width.toDouble * normX / (2 * math.Pi)
      val angleY   = math.atan2(acc.sumSinY, acc.sumCosY)
      val normY    = if angleY < 0 then angleY + 2 * math.Pi else angleY
      val centroidY = state.height.toDouble * normY / (2 * math.Pi)
      val xCell     = math.floor(centroidX / 256.0).toInt
      val yCell     = math.floor(centroidY / 160.0).toInt
      p -> ProvinceLocation(XCell(xCell), YCell(yCell))
    }
