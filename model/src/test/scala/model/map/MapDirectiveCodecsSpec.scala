package com.crib.bills.dom6maps
package model.map

import cats.effect.IO
import weaver.SimpleIOSuite
import model.{ProvinceId, BorderFlag, Nation, TerrainFlag, MagicType, FeatureId}
import MapDirectiveCodecs.given
import MapDirectiveCodecs.Encoder

object MapDirectiveCodecsSpec extends SimpleIOSuite:
  test("encodes map state to directives") {
    val state = MapState(
      size = MapSize.from(5).toOption,
      adjacency = Vector(
        (ProvinceId(3), ProvinceId(4)),
        (ProvinceId(5), ProvinceId(6)),
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
      SetLand(ProvinceId(5)),
      Feature(FeatureId(9)),
      Gate(ProvinceId(1), ProvinceId(2)),
      Neighbour(ProvinceId(3), ProvinceId(4)),
      Neighbour(ProvinceId(5), ProvinceId(6)),
      NeighbourSpec(ProvinceId(5), ProvinceId(6), BorderFlag.MountainPass)
    )

    IO.pure(expect(Encoder[MapState].encode(state) == expected))
  }

  test("merges pass-through directives") {
    val state = MapState.empty
    val pass = Vector(SailDist(2), Features(7))
    val expected = Vector(NoWrapAround, LineBreak, SailDist(2), Features(7))
    IO.pure(expect(MapDirectiveCodecs.merge(state, pass) == expected))
  }

  test("inserts section breaks between directive groups") {
    val state = MapState.empty.copy(
      title = Some(MapTitle("Example")),
      description = Some(MapDescription("desc")),
      allowedPlayers = Vector(AllowedPlayer(Nation.Agartha_Early)),
      startingPositions = Vector(SpecStart(Nation.Agartha_Early, ProvinceId(1))),
      terrains = Vector(Terrain(ProvinceId(1), 1L)),
      features = Vector(ProvinceFeature(ProvinceId(1), FeatureId(5))),
      gates = Vector(Gate(ProvinceId(1), ProvinceId(2))),
      adjacency = Vector((ProvinceId(1), ProvinceId(2))),
      borders = Vector(Border(ProvinceId(2), ProvinceId(3), BorderFlag.MountainPass))
    )

    val merged = MapDirectiveCodecs.merge(state, Vector.empty)
    val firstAllowed = merged.indexWhere { case _: AllowedPlayer => true; case _ => false }
    val firstSpecStart = merged.indexWhere { case _: SpecStart => true; case _ => false }
    val firstTerrain = merged.indexWhere { case _: Terrain => true; case _ => false }
    val firstGate = merged.indexWhere { case _: Gate => true; case _ => false }
    val firstNeighbour = merged.indexWhere { case _: Neighbour => true; case _ => false }
    val firstNeighbourSpec = merged.indexWhere { case _: NeighbourSpec => true; case _ => false }

    IO.pure(
      expect(firstAllowed > 0 && merged(firstAllowed - 1) == LineBreak) &&
        expect(firstSpecStart > 0 && merged(firstSpecStart - 1) == LineBreak) &&
        expect(firstTerrain > 0 && merged(firstTerrain - 1) == LineBreak) &&
        expect(firstGate > 0 && merged(firstGate - 1) == LineBreak) &&
        expect(firstNeighbour > 0 && merged(firstNeighbour - 1) == LineBreak) &&
        expect(firstNeighbourSpec > 0 && merged(firstNeighbourSpec - 1) == LineBreak)
    )
  }

  test("encodes allowed players and spec starts in ascending nation id order") {
    val state = MapState.empty.copy(
      allowedPlayers = Vector(
        AllowedPlayer(Nation.Helheim_Early),
        AllowedPlayer(Nation.Atlantis_Early),
        AllowedPlayer(Nation.Marverni_Early)
      ),
      startingPositions = Vector(
        SpecStart(Nation.Helheim_Early, ProvinceId(3)),
        SpecStart(Nation.Atlantis_Early, ProvinceId(1)),
        SpecStart(Nation.Marverni_Early, ProvinceId(2))
      )
    )

    val directives = Encoder[MapState].encode(state)

    val allowedIds = directives.collect { case AllowedPlayer(nation) => nation.id }
    val specStartIds = directives.collect { case SpecStart(nation, _) => nation.id }

    IO.pure(
      expect(allowedIds == allowedIds.sorted) &&
        expect(specStartIds == specStartIds.sorted)
    )
  }
