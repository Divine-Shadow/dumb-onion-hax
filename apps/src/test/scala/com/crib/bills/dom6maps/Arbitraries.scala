package com.crib.bills.dom6maps

import org.scalacheck.{Arbitrary, Gen}
import model.*
import model.map.*

object Arbitraries:
  given Arbitrary[Nation] =
    Arbitrary(Gen.oneOf(Nation.values.toSeq))

  given Arbitrary[ProvinceId] =
    Arbitrary(Gen.choose(1, 5000).map(ProvinceId.apply))

  given Arbitrary[BorderFlag] =
    Arbitrary(Gen.oneOf(BorderFlag.values.toSeq))

  given Arbitrary[MapWidth] =
    Arbitrary(Gen.choose(10, 5000).map(MapWidth.apply))

  given Arbitrary[MapHeight] =
    Arbitrary(Gen.choose(10, 5000).map(MapHeight.apply))

  given Arbitrary[Dom2Title] =
    Arbitrary(Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString).map(Dom2Title.apply))

  given Arbitrary[ImageFile] =
    Arbitrary(Gen.nonEmptyListOf(Gen.alphaChar).map(n => ImageFile(s"$n.tga")))

  given Arbitrary[MapSize] =
    Arbitrary(for
      w <- summon[Arbitrary[MapWidth]].arbitrary
      h <- summon[Arbitrary[MapHeight]].arbitrary
    yield MapSize(w, h))

  given Arbitrary[DomVersion] =
    Arbitrary(Gen.choose(500, 600).map(DomVersion.apply))

  given Arbitrary[ColorComponent] =
    Arbitrary(Gen.choose(0.0, 1.0).map(ColorComponent.apply))

  given Arbitrary[FloatColor] =
    Arbitrary(for
      r <- summon[Arbitrary[ColorComponent]].arbitrary
      g <- summon[Arbitrary[ColorComponent]].arbitrary
      b <- summon[Arbitrary[ColorComponent]].arbitrary
      a <- summon[Arbitrary[ColorComponent]].arbitrary
    yield FloatColor(r, g, b, a))

  given Arbitrary[MapTextColor] =
    Arbitrary(summon[Arbitrary[FloatColor]].arbitrary.map(MapTextColor.apply))

  given Arbitrary[MapDomColor] =
    Arbitrary(for
      r <- Gen.choose(0, 255)
      g <- Gen.choose(0, 255)
      b <- Gen.choose(0, 255)
      a <- Gen.choose(0, 255)
    yield MapDomColor(r, g, b, a))

  given Arbitrary[AllowedPlayer] =
    Arbitrary(summon[Arbitrary[Nation]].arbitrary.map(AllowedPlayer.apply))

  given Arbitrary[SpecStart] =
    Arbitrary(for
      n <- summon[Arbitrary[Nation]].arbitrary
      p <- summon[Arbitrary[ProvinceId]].arbitrary
    yield SpecStart(n, p))

  given Arbitrary[Terrain] =
    Arbitrary(for
      p <- summon[Arbitrary[ProvinceId]].arbitrary
      m <- Gen.choose(0, Int.MaxValue)
    yield Terrain(p, m))

  given Arbitrary[LandName] =
    Arbitrary(for
      p <- summon[Arbitrary[ProvinceId]].arbitrary
      n <- Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString)
    yield LandName(p, n))

  given Arbitrary[Neighbour] =
    Arbitrary(for
      a <- summon[Arbitrary[ProvinceId]].arbitrary
      b <- summon[Arbitrary[ProvinceId]].arbitrary
    yield Neighbour(a, b))

  given Arbitrary[NeighbourSpec] =
    Arbitrary(for
      a <- summon[Arbitrary[ProvinceId]].arbitrary
      b <- summon[Arbitrary[ProvinceId]].arbitrary
      f <- summon[Arbitrary[BorderFlag]].arbitrary
    yield NeighbourSpec(a, b, f))

  given Arbitrary[MapDirective] =
    val gen = Gen.oneOf[
      MapDirective](
      summon[Arbitrary[Dom2Title]].arbitrary,
      summon[Arbitrary[ImageFile]].arbitrary,
      summon[Arbitrary[MapSize]].arbitrary,
      summon[Arbitrary[DomVersion]].arbitrary,
      Gen.const(WrapAround),
      Gen.const(HWrapAround),
      Gen.const(VWrapAround),
      Gen.const(NoWrapAround),
      Gen.const(NoDeepCaves),
      Gen.const(NoDeepChoice),
      Gen.const(MapNoHide),
      summon[Arbitrary[MapTextColor]].arbitrary,
      summon[Arbitrary[MapDomColor]].arbitrary,
      summon[Arbitrary[AllowedPlayer]].arbitrary,
      summon[Arbitrary[SpecStart]].arbitrary,
      summon[Arbitrary[Terrain]].arbitrary,
      summon[Arbitrary[LandName]].arbitrary,
      summon[Arbitrary[Neighbour]].arbitrary,
      summon[Arbitrary[NeighbourSpec]].arbitrary
    )
    Arbitrary(gen)
