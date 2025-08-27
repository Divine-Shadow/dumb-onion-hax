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
      Pb(0, 0, 1, ProvinceId(1)),
      Pb(4 * 256, 0, 1, ProvinceId(5)),
      Pb(0, 4 * 160, 1, ProvinceId(21)),
      Pb(4 * 256, 4 * 160, 1, ProvinceId(25)),
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
      Pb(0, 0, 1, ProvinceId(1)),
      Pb(4 * 256, 0, 1, ProvinceId(5)),
      Pb(0, 4 * 160, 1, ProvinceId(21)),
      Pb(4 * 256, 4 * 160, 1, ProvinceId(25)),
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
        val thrones = Vector(
          ProvinceLocation(XCell(0), YCell(0)),
          ProvinceLocation(XCell(4), YCell(0)),
          ProvinceLocation(XCell(0), YCell(4)),
          ProvinceLocation(XCell(4), YCell(4))
        )
        val center = ProvinceId(13)
        def hasThrones(state: MapState) =
          thrones.forall { loc =>
            state.provinceLocations.provinceIdAt(loc).exists { id =>
              state.terrains.exists { case Terrain(p, m) if p == id => TerrainMask(m).hasFlag(TerrainFlag.Throne); case _ => false }
            }
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

  test("pipe removes diagonal adjacencies across wraps"):
    val size = MapSize.from(5).toOption.get
    val locations = ProvinceLocations.fromProvinceIdMap(
      Map(
        ProvinceId(5) -> ProvinceLocation(XCell(4), YCell(0)),
        ProvinceId(6) -> ProvinceLocation(XCell(0), YCell(1)),
        ProvinceId(7) -> ProvinceLocation(XCell(1), YCell(1))
      )
    )
    val base = MapState.empty.copy(
      size = Some(size),
      adjacency = Vector((ProvinceId(5), ProvinceId(6)), (ProvinceId(6), ProvinceId(7))),
      wrap = WrapState.FullWrap,
      provinceLocations = locations
    )
    import cats.instances.either.*
    type EC[A] = Either[Throwable, A]
    val pipe = new apps.services.mapeditor.GroundSurfaceDuelPipeImpl[IO](
      new apps.services.mapeditor.MapSizeValidatorStub[IO](size, base, base),
      new apps.services.mapeditor.PlacementPlannerStub[IO](Vector.empty, Vector.empty),
      new apps.services.mapeditor.GateDirectiveServiceStub[IO],
      new apps.services.mapeditor.ThronePlacementServiceStub[IO],
      new apps.services.mapeditor.SpawnPlacementServiceStub[IO]
    )
    for
      res <- pipe.apply[EC](
        base,
        base,
        GroundSurfaceDuelConfig.default,
        SurfaceNation(Nation.Atlantis_Early),
        UndergroundNation(Nation.Mictlan_Early)
      )
      result <- IO.fromEither(res.map { case (surfRes, caveRes) =>
        val pair      = (ProvinceId(5), ProvinceId(6))
        val interior  = (ProvinceId(6), ProvinceId(7))
        val removed   = !surfRes.adjacency.contains(pair) && !caveRes.adjacency.contains(pair)
        val kept      = surfRes.adjacency.contains(interior) && caveRes.adjacency.contains(interior)
        val wrapsGone = surfRes.wrap == WrapState.NoWrap && caveRes.wrap == WrapState.NoWrap
        expect(removed && kept && wrapsGone)
      })
    yield result
