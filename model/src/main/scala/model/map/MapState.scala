package com.crib.bills.dom6maps
package model.map

import cats.effect.kernel.Concurrent
import fs2.Stream
import model.{BorderFlag, ProvinceId}

enum WrapState:
  case NoWrap, HorizontalWrap, VerticalWrap, FullWrap

final case class Border(a: ProvinceId, b: ProvinceId, flag: BorderFlag)
final case class MapTitle(value: String) extends AnyVal
final case class MapDescription(value: String) extends AnyVal

final case class MapState(
    size: Option[MapSize],
    adjacency: Vector[(ProvinceId, ProvinceId)],
    borders: Vector[Border],
    wrap: WrapState,
    title: Option[MapTitle],
    description: Option[MapDescription],
    allowedPlayers: Vector[AllowedPlayer],
    startingPositions: Vector[SpecStart],
    terrains: Vector[Terrain],
    gates: Vector[Gate]
)

object MapState:
  val empty: MapState = MapState(
    None,
    Vector.empty,
    Vector.empty,
    WrapState.NoWrap,
    None,
    None,
    Vector.empty,
    Vector.empty,
    Vector.empty,
    Vector.empty
  )

  def fromDirectives[F[_]: Concurrent](directives: Stream[F, MapDirective]): F[MapState] =
    directives.compile.fold(empty)(accumulate)

  private def accumulate(state: MapState, directive: MapDirective): MapState =
    directive match
      case m: MapSizePixels =>
        val width = m.toProvinceSize.width.value
        MapSize.from(width).toOption match
          case Some(sz) => state.copy(size = Some(sz))
          case None     => state
      case Dom2Title(t) =>
        state.copy(title = Some(MapTitle(t)))
      case Description(d) =>
        state.copy(description = Some(MapDescription(d)))
      case WrapAround =>
        state.copy(wrap = WrapState.FullWrap)
      case HWrapAround =>
        state.copy(wrap = WrapState.HorizontalWrap)
      case VWrapAround =>
        state.copy(wrap = WrapState.VerticalWrap)
      case NoWrapAround =>
        state.copy(wrap = WrapState.NoWrap)
      case ap: AllowedPlayer =>
        state.copy(allowedPlayers = state.allowedPlayers :+ ap)
      case ss: SpecStart =>
        state.copy(startingPositions = state.startingPositions :+ ss)
      case t: Terrain =>
        state.copy(terrains = state.terrains :+ t)
      case g: Gate =>
        state.copy(gates = state.gates :+ g)
      case Neighbour(a, b) =>
        state.copy(adjacency = state.adjacency :+ ((a, b)))
      case NeighbourSpec(a, b, f) =>
        state.copy(
          adjacency = state.adjacency :+ ((a, b)),
          borders = state.borders :+ Border(a, b, f)
        )
      case _ =>
        state
