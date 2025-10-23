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

  given Encoder[ProvinceFeature] with
    def encode(value: ProvinceFeature): Vector[MapDirective] =
      Vector(SetLand(value.province), Feature(value.id))

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
      val players =
        value.allowedPlayers
          .sortBy(_.nation.id)
          .flatMap(Encoder[AllowedPlayer].encode)
      val starts =
        value.startingPositions
          .sortBy(_.nation.id)
          .flatMap(Encoder[SpecStart].encode)
      val terrains = value.terrains.flatMap(Encoder[Terrain].encode)
      val features = value.features.flatMap(Encoder[ProvinceFeature].encode)
      val gates = value.gates.flatMap(Encoder[Gate].encode)
      val adjacencyPairs =
        (value.adjacency ++ value.borders.map(b => (b.a, b.b))).distinct
      val adjacency = adjacencyPairs.flatMap(Encoder[(ProvinceId, ProvinceId)].encode)
      val borders = value.borders.flatMap(Encoder[Border].encode)
      size ++ wrap ++ title ++ description ++ players ++ starts ++ terrains ++ features ++ gates ++ adjacency ++ borders

  private def isPassThrough(directive: MapDirective): Boolean =
    directive match
      case MapSizePixels(_, _)                                         => false
      case Dom2Title(_)                                                => false
      case Description(_)                                              => false
      case WrapAround | HWrapAround | VWrapAround | NoWrapAround       => false
      case _: AllowedPlayer | _: SpecStart | _: Terrain | _: ProvinceFeature | _: Gate      => false
      case Neighbour(_, _) | NeighbourSpec(_, _, _)                    => false
      case SetLand(_) | Feature(_)                                     => true
      case _                                                           => true

  // Header-like pass-through directives that should appear before state-owned output
  // to match game loader expectations and typical map file conventions.
  private def isHeader(directive: MapDirective): Boolean =
    directive match
      case Comment(_)        => true
      case ImageFile(_)       => true
      case WinterImageFile(_) => true
      case DomVersion(_)      => true
      case PlaneName(_)       => true
      case MapTextColor(_)    => true
      case MapDomColor(_,_,_,_) => true
      case SailDist(_)        => true
      case Features(_)        => true
      case MapNoHide          => true
      case NoDeepCaves        => true
      case NoDeepChoice       => true
      case _                  => false

  def merge(state: MapState, passThrough: Vector[MapDirective]): Vector[MapDirective] =
    val (pt, nonPt) = passThrough.partition(isPassThrough)
    require(nonPt.isEmpty, "passThrough contains state-owned directives")

    val stateAll = Encoder[MapState].encode(state)

    // Extract state-owned header directives so we can place them early
    def removeFirst(vec: Vector[MapDirective])(pred: MapDirective => Boolean): (Vector[MapDirective], Option[MapDirective]) =
      val idx = vec.indexWhere(pred)
      if idx >= 0 then (vec.patch(idx, Nil, 1), Some(vec(idx))) else (vec, None)

    val (s1, title) = removeFirst(stateAll) { case Dom2Title(_) => true; case _ => false }
    val (s2, mapSize) = removeFirst(s1) { case MapSizePixels(_, _) => true; case _ => false }
    val (s3, wrap) = removeFirst(s2) {
      case WrapAround | HWrapAround | VWrapAround | NoWrapAround => true
      case _                                                     => false
    }
    val (stateBody, description) = removeFirst(s3) { case Description(_) => true; case _ => false }

    // Partition pass-through headers by specific kinds so we can order them
    def takePT[A](pred: MapDirective => Boolean) = pt.filter(pred)
    def dropPT[A](pred: MapDirective => Boolean) = pt.filterNot(pred)

    val imageFiles       = takePT { case ImageFile(_) => true; case _ => false }
    val winterImageFiles = takePT { case WinterImageFile(_) => true; case _ => false }
    val textCols         = takePT { case MapTextColor(_) => true; case _ => false }
    val domCols          = takePT { case MapDomColor(_,_,_,_) => true; case _ => false }
    val domVersions      = takePT { case DomVersion(_) => true; case _ => false }
    val mapNoHide        = takePT { case MapNoHide => true; case _ => false }
    val noDeepCaves      = takePT { case NoDeepCaves => true; case _ => false }
    val noDeepChoice     = takePT { case NoDeepChoice => true; case _ => false }
    val planeNames       = takePT { case PlaneName(_) => true; case _ => false }
    val sailDist         = takePT { case SailDist(_) => true; case _ => false }
    val features         = takePT { case Features(_) => true; case _ => false }
    val comments         = takePT { case Comment(_) => true; case _ => false }

    val headerSelected =
      comments ++
      title.toVector ++
      imageFiles ++
      winterImageFiles ++
      mapSize.toVector ++
      wrap.toVector ++
      textCols ++
      domCols ++
      domVersions ++
      description.toVector ++
      mapNoHide ++
      noDeepCaves ++
      noDeepChoice ++
      planeNames ++
      sailDist ++
      features

    // Remove all header-selected pass-through from pt
    val headerSet = headerSelected.toSet
    val remainingPT = pt.filterNot(headerSet.contains)

    headerSelected ++ stateBody ++ remainingPT
