package com.crib.bills.dom6maps
package model.map

import cats.effect.IO
import weaver.SimpleIOSuite
import model.{ProvinceId, BorderFlag, Nation}
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
      allowedPlayers = Vector(AllowedPlayer(Nation.Atlantis_Early)),
      startingPositions = Vector(SpecStart(Nation.Atlantis_Early, ProvinceId(42))),
      terrains = Vector(Terrain(ProvinceId(5), 7)),
      gates = Vector(Gate(ProvinceId(1), ProvinceId(2))),
      provinceLocations = Map.empty
    )

    val expected = Vector(
      MapSizePixels(MapWidthPixels(1280), MapHeightPixels(800)),
      HWrapAround,
      Dom2Title("T"),
      Description("D"),
      AllowedPlayer(Nation.Atlantis_Early),
      SpecStart(Nation.Atlantis_Early, ProvinceId(42)),
      Terrain(ProvinceId(5), 7),
      Gate(ProvinceId(1), ProvinceId(2)),
      Neighbour(ProvinceId(3), ProvinceId(4)),
      NeighbourSpec(ProvinceId(5), ProvinceId(6), BorderFlag.MountainPass)
    )

    IO.pure(expect(Encoder[MapState].encode(state) == expected))
  }
