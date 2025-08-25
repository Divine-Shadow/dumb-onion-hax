package com.crib.bills.dom6maps
package model.map

import cats.effect.IO
import weaver.SimpleIOSuite
import model.{ProvinceId, BorderFlag, Nation, TerrainFlag, MagicType}
import MapDirectiveCodecs.given
import MapDirectiveCodecs.Encoder

object MapDirectiveCodecsSpec extends SimpleIOSuite:
  test("encodes map state to directives") {
    val state = MapState(
      size = MapSize.from(5).toOption,
      adjacency = Vector(
        (ProvinceId(3), ProvinceId(4)),
        (ProvinceId(5), ProvinceId(6))
      ),
      borders = Vector(Border(ProvinceId(5), ProvinceId(6), BorderFlag.MountainPass)),
      wrap = WrapState.HorizontalWrap,
      title = Some(MapTitle("T")),
      description = Some(MapDescription("D")),
      allowedPlayers = Vector(AllowedPlayer(Nation.Feminie_Late)),
      startingPositions = Vector(SpecStart(Nation.Feminie_Late, ProvinceId(42))),
      terrains = Vector(Terrain(ProvinceId(5), TerrainFlag.GoodThrone.mask | MagicType.Holy.mask.toLong)),
      features = Vector(ProvinceFeature(ProvinceId(5), FeatureId(9))),
      gates = Vector(Gate(ProvinceId(1), ProvinceId(2))),
      provinceLocations = ProvinceLocations.empty
    )

    val expected = Vector(
      MapSizePixels(MapWidthPixels(1280), MapHeightPixels(800)),
      HWrapAround,
      Dom2Title("T"),
      Description("D"),
      AllowedPlayer(Nation.Feminie_Late),
      SpecStart(Nation.Feminie_Late, ProvinceId(42)),
      Terrain(ProvinceId(5), TerrainFlag.GoodThrone.mask | MagicType.Holy.mask.toLong),
      ProvinceFeature(ProvinceId(5), FeatureId(9)),
      Gate(ProvinceId(1), ProvinceId(2)),
      Neighbour(ProvinceId(3), ProvinceId(4)),
      NeighbourSpec(ProvinceId(5), ProvinceId(6), BorderFlag.MountainPass)
    )

    IO.pure(expect(Encoder[MapState].encode(state) == expected))
  }

  test("merges pass-through directives") {
    val state = MapState.empty
    val pass = Vector(SailDist(2), Features(7))
    val expected = Vector(NoWrapAround) ++ pass
    IO.pure(expect(MapDirectiveCodecs.merge(state, pass) == expected))
  }
