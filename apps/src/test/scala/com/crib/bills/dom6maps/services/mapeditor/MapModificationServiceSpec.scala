package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.instances.either.*
import fs2.io.file.Path
import weaver.SimpleIOSuite
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import model.ProvinceId
import model.{TerrainFlag, TerrainMask}
import model.map.{Gate, Terrain, MapFileParser}
import model.map.{GateSpec, ThronePlacement, ThroneLevel, ProvinceLocation, XCell, YCell}

object MapModificationServiceSpec extends SimpleIOSuite:
  type EC[A] = Either[Throwable, A]

  test("modify applies gate and throne transformations") {
    val loader = new MapLayerLoaderImpl[IO]
    val gateService = new GateDirectiveServiceImpl[IO]
    val throneService = new ThronePlacementServiceImpl[IO]
    val writer = new MapWriterImpl[IO]
    val service = new MapModificationServiceImpl[IO](loader, gateService, throneService, writer)
    val gates = Vector(GateSpec(ProvinceId(3), ProvinceId(4)))
    val thrones = Vector(ThronePlacement(ProvinceLocation(XCell(0), YCell(0)), ThroneLevel(1)))
    for
      dir <- IO(Files.createTempDirectory("maps"))
      surfaceIn = dir.resolve("surface.map")
      caveIn = dir.resolve("cave.map")
      surfaceOut = dir.resolve("surface-out.map")
      caveOut = dir.resolve("cave-out.map")
      _ <- IO(Files.write(surfaceIn, """#dom2title surface
#mapsize 1280 800
#pb 0 0 1 1
#pb 256 0 1 2
#terrain 1 0
#terrain 2 67108864
#gate 1 2
""".getBytes(StandardCharsets.UTF_8)))
      _ <- IO(Files.write(caveIn, """#dom2title cave
#gate 2 3
""".getBytes(StandardCharsets.UTF_8)))
      resultEC <- service.modify[EC](Path.fromNioPath(surfaceIn), Path.fromNioPath(caveIn), gates, thrones, Path.fromNioPath(surfaceOut), Path.fromNioPath(caveOut))
      _ <- IO.fromEither(resultEC)
      surfaceDirectives <- MapFileParser.parseFile[IO](Path.fromNioPath(surfaceOut)).compile.toVector
      caveDirectives    <- MapFileParser.parseFile[IO](Path.fromNioPath(caveOut)).compile.toVector
      mask1 = surfaceDirectives.collectFirst { case Terrain(ProvinceId(1), m) => TerrainMask(m) }.get
      mask2 = surfaceDirectives.collectFirst { case Terrain(ProvinceId(2), m) => TerrainMask(m) }.get
    yield expect.all(
      surfaceDirectives.contains(Gate(ProvinceId(3), ProvinceId(4))),
      !surfaceDirectives.exists { case Gate(ProvinceId(1), ProvinceId(2)) => true; case _ => false },
      caveDirectives.contains(Gate(ProvinceId(3), ProvinceId(4))),
      !caveDirectives.exists { case Gate(ProvinceId(2), ProvinceId(3)) => true; case _ => false },
      mask1.hasFlag(TerrainFlag.Throne),
      !mask2.hasFlag(TerrainFlag.Throne)
    )
  }
