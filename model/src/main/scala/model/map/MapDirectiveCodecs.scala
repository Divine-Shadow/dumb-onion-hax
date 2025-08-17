package com.crib.bills.dom6maps
package model.map

import model.{ProvinceId, BorderFlag}

object MapDirectiveCodecs:
  trait Encoder[A]:
    def encode(value: A): Vector[MapDirective]

  object Encoder:
    def apply[A](using enc: Encoder[A]) = enc

    extension [A](value: A)(using enc: Encoder[A])
      def toDirectives: Vector[MapDirective] = enc.encode(value)

  import Encoder.*

  given Encoder[MapSize] with
    def encode(value: MapSize): Vector[MapDirective] =
      Vector(
        MapSizePixels(
          MapWidthPixels(value.value * 256),
          MapHeightPixels(value.value * 160)
        )
      )

  given Encoder[WrapState] with
    def encode(value: WrapState): Vector[MapDirective] =
      Vector(
        value match
          case WrapState.FullWrap       => WrapAround
          case WrapState.HorizontalWrap => HWrapAround
          case WrapState.VerticalWrap   => VWrapAround
          case WrapState.NoWrap         => NoWrapAround
      )

  given Encoder[MapTitle] with
    def encode(value: MapTitle): Vector[MapDirective] =
      Vector(Dom2Title(value.value))

  given Encoder[MapDescription] with
    def encode(value: MapDescription): Vector[MapDirective] =
      Vector(Description(value.value))

  given Encoder[AllowedPlayer] with
    def encode(value: AllowedPlayer): Vector[MapDirective] =
      Vector(value)

  given Encoder[SpecStart] with
    def encode(value: SpecStart): Vector[MapDirective] =
      Vector(value)

  given Encoder[Terrain] with
    def encode(value: Terrain): Vector[MapDirective] =
      Vector(value)

  given Encoder[Gate] with
    def encode(value: Gate): Vector[MapDirective] =
      Vector(value)

  given Encoder[(ProvinceId, ProvinceId)] with
    def encode(value: (ProvinceId, ProvinceId)): Vector[MapDirective] =
      Vector(Neighbour(value._1, value._2))

  given Encoder[Border] with
    def encode(value: Border): Vector[MapDirective] =
      Vector(NeighbourSpec(value.a, value.b, value.flag))

  given Encoder[MapState] with
    def encode(value: MapState): Vector[MapDirective] =
      val size = value.size.toVector.flatMap(Encoder[MapSize].encode)
      val wrap = Encoder[WrapState].encode(value.wrap)
      val title = value.title.toVector.flatMap(Encoder[MapTitle].encode)
      val description = value.description.toVector.flatMap(Encoder[MapDescription].encode)
      val players = value.allowedPlayers.flatMap(Encoder[AllowedPlayer].encode)
      val starts = value.startingPositions.flatMap(Encoder[SpecStart].encode)
      val terrains = value.terrains.flatMap(Encoder[Terrain].encode)
      val gates = value.gates.flatMap(Encoder[Gate].encode)
      val borderPairs = value.borders.map(b => (b.a, b.b)).toSet
      val adjacency = value.adjacency
        .filterNot(borderPairs.contains)
        .flatMap(Encoder[(ProvinceId, ProvinceId)].encode)
      val borders = value.borders.flatMap(Encoder[Border].encode)
      size ++ wrap ++ title ++ description ++ players ++ starts ++ terrains ++ gates ++ adjacency ++ borders

  private def isPassThrough(directive: MapDirective): Boolean =
    directive match
      case MapSizePixels(_, _)                                         => false
      case Dom2Title(_)                                                => false
      case Description(_)                                              => false
      case WrapAround | HWrapAround | VWrapAround | NoWrapAround       => false
      case _: AllowedPlayer | _: SpecStart | _: Terrain | _: Gate      => false
      case Neighbour(_, _) | NeighbourSpec(_, _, _)                    => false
      case _                                                           => true

  def merge(state: MapState, passThrough: Vector[MapDirective]): Vector[MapDirective] =
    val (pt, nonPt) = passThrough.partition(isPassThrough)
    require(nonPt.isEmpty, "passThrough contains state-owned directives")
    Encoder[MapState].encode(state) ++ pt
