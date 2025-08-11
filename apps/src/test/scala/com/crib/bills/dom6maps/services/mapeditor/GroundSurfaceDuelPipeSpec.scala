package com.crib.bills.dom6maps
package services.mapeditor

import cats.effect.IO
import fs2.Stream
import weaver.SimpleIOSuite
import cats.syntax.all.*
import model.{ProvinceId, Nation, TerrainFlag, TerrainMask}
import model.map.*

object GroundSurfaceDuelPipeSpec extends SimpleIOSuite:
  private def mapSizeDirective = MapSizePixels(MapWidthPixels(5 * 256), MapHeightPixels(5 * 160))

  test("pipe adds gates, thrones, and severs wrap"):
    val surface = Vector(
      mapSizeDirective,
      WrapAround,
      Gate(ProvinceId(1), ProvinceId(2)),
      AllowedPlayer(Nation.Agartha_Early),
      SpecStart(Nation.Agartha_Early, ProvinceId(1)),
      Terrain(ProvinceId(1), 0),
      Terrain(ProvinceId(5), 0),
      Terrain(ProvinceId(21), 0),
      Terrain(ProvinceId(25), 0)
    )
    val cave = Vector(
      mapSizeDirective,
      WrapAround,
      Gate(ProvinceId(2), ProvinceId(3)),
      AllowedPlayer(Nation.Ulm_Early),
      SpecStart(Nation.Ulm_Early, ProvinceId(1)),
      Terrain(ProvinceId(1), 0),
      Terrain(ProvinceId(5), 0),
      Terrain(ProvinceId(21), 0),
      Terrain(ProvinceId(25), 0)
    )
    import cats.instances.either.*
    type EC[A] = Either[Throwable, A]
    val pipe = new apps.services.mapeditor.GroundSurfaceDuelPipeImpl[IO](
      new apps.services.mapeditor.MapSizeValidatorImpl[IO],
      new apps.services.mapeditor.PlacementPlannerImpl[IO],
      new apps.services.mapeditor.GateDirectiveServiceImpl[IO],
      new apps.services.mapeditor.ThronePlacementServiceImpl[IO],
      new apps.services.mapeditor.SpawnPlacementServiceImpl[IO]
    )
    for
      surfaceState <- MapState.fromDirectives(Stream.emits(surface).covary[IO])
      caveState <- MapState.fromDirectives(Stream.emits(cave).covary[IO])
      res <- pipe.apply[EC](
        surfaceState,
        caveState,
        GroundSurfaceDuelConfig.default,
        SurfaceNation(Nation.Atlantis_Early),
        UndergroundNation(Nation.Mictlan_Early)
      )
      result <- IO.fromEither(res.map { case (surfRes, caveRes) =>
        val gates = Vector(
          Gate(ProvinceId(3), ProvinceId(28)),
          Gate(ProvinceId(23), ProvinceId(48)),
          Gate(ProvinceId(11), ProvinceId(36)),
          Gate(ProvinceId(15), ProvinceId(40))
        )
        val thrones = Vector(1, 5, 21, 25).map(ProvinceId.apply)
        val center = ProvinceId(13)
        def hasThrones(state: MapState) =
          thrones.forall { id =>
            state.terrains.exists { case Terrain(p, m) if p == id => TerrainMask(m).hasFlag(TerrainFlag.Throne); case _ => false }
          }
        val surfChecks =
          gates.forall(surfRes.gates.contains) &&
            hasThrones(surfRes) &&
            !surfRes.gates.contains(Gate(ProvinceId(1), ProvinceId(2))) &&
            surfRes.wrap == WrapState.NoWrap &&
            surfRes.allowedPlayers.contains(AllowedPlayer(Nation.Atlantis_Early)) &&
            surfRes.startingPositions.contains(SpecStart(Nation.Atlantis_Early, center)) &&
            !surfRes.allowedPlayers.contains(AllowedPlayer(Nation.Agartha_Early))
        val caveChecks =
          gates.forall(caveRes.gates.contains) &&
            hasThrones(caveRes) &&
            !caveRes.gates.contains(Gate(ProvinceId(2), ProvinceId(3))) &&
            caveRes.wrap == WrapState.NoWrap &&
            caveRes.allowedPlayers.contains(AllowedPlayer(Nation.Mictlan_Early)) &&
            caveRes.startingPositions.contains(SpecStart(Nation.Mictlan_Early, center)) &&
            !caveRes.allowedPlayers.contains(AllowedPlayer(Nation.Ulm_Early))
        expect(surfChecks && caveChecks)
      })
    yield result
