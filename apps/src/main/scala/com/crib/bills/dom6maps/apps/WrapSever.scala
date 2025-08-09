package com.crib.bills.dom6maps
package apps

import cats.effect.Sync
import fs2.{Pipe, Stream}
import com.crib.bills.dom6maps.model.ProvinceId
import com.crib.bills.dom6maps.model.map.{
  MapDirective,
  MapHeight,
  MapWidth,
  Neighbour,
  NeighbourSpec,
  WrapAround,
  HWrapAround,
  VWrapAround,
  NoWrapAround
}

object WrapSever:
  def isTopBottom(
      a: ProvinceId,
      b: ProvinceId,
      width: MapWidth,
      height: MapHeight
  ): Boolean =
    val rowA = (a.value - 1) / width.value
    val rowB = (b.value - 1) / width.value
    val top = height.value - 1
    val bottom = 0
    (rowA == top && rowB == bottom) || (rowA == bottom && rowB == top)

  def isLeftRight(a: ProvinceId, b: ProvinceId, width: MapWidth): Boolean =
    val colA = (a.value - 1) % width.value
    val colB = (b.value - 1) % width.value
    val left = 0
    val right = width.value - 1
    (colA == left && colB == right) || (colA == right && colB == left)

  private val wrapDirectives =
    Set[MapDirective](WrapAround, HWrapAround, VWrapAround, NoWrapAround)

  def severVertically(
      directives: Vector[MapDirective],
      width: MapWidth,
      height: MapHeight
  ): Vector[MapDirective] =
    val wrap = directives.collectFirst { case d if wrapDirectives.contains(d) => d }
    val shouldSever = wrap.exists(d => d == WrapAround || d == VWrapAround)
    val withoutConnections = directives.filter {
      case Neighbour(a, b) if shouldSever        => !isTopBottom(a, b, width, height)
      case NeighbourSpec(a, b, _) if shouldSever => !isTopBottom(a, b, width, height)
      case _                                     => true
    }
    val withoutWrapDirective = withoutConnections.filterNot(wrapDirectives.contains)
    val newDirective = wrap match
      case Some(WrapAround)  => HWrapAround
      case Some(VWrapAround) => NoWrapAround
      case Some(HWrapAround) => HWrapAround
      case _                 => NoWrapAround
    withoutWrapDirective :+ newDirective

  def severHorizontally(
      directives: Vector[MapDirective],
      width: MapWidth,
      height: MapHeight
  ): Vector[MapDirective] =
    val wrap = directives.collectFirst { case d if wrapDirectives.contains(d) => d }
    val shouldSever = wrap.exists(d => d == WrapAround || d == HWrapAround)
    val withoutConnections = directives.filter {
      case Neighbour(a, b) if shouldSever        => !isLeftRight(a, b, width)
      case NeighbourSpec(a, b, _) if shouldSever => !isLeftRight(a, b, width)
      case _                                     => true
    }
    val withoutWrapDirective = withoutConnections.filterNot(wrapDirectives.contains)
    val newDirective = wrap match
      case Some(WrapAround)  => VWrapAround
      case Some(HWrapAround) => NoWrapAround
      case Some(VWrapAround) => VWrapAround
      case _                 => NoWrapAround
    withoutWrapDirective :+ newDirective

  private def pipe[F[_]: Sync](
      f: (Vector[MapDirective], MapWidth, MapHeight) => Vector[MapDirective],
      width: MapWidth,
      height: MapHeight
  ): Pipe[F, MapDirective, MapDirective] =
    in =>
      Stream
        .eval(in.compile.toVector)
        .flatMap(ds => Stream.emits(f(ds, width, height)))

  def verticalPipe[F[_]: Sync](
      width: MapWidth,
      height: MapHeight
  ): Pipe[F, MapDirective, MapDirective] =
    pipe(severVertically, width, height)

  def horizontalPipe[F[_]: Sync](
      width: MapWidth,
      height: MapHeight
  ): Pipe[F, MapDirective, MapDirective] =
    pipe(severHorizontally, width, height)
