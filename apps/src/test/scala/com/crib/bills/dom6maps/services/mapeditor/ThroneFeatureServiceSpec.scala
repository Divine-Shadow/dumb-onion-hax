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
import model.map.{Terrain, ThroneFeatureConfig, ThronePlacement, ThroneLevel, MapFileParser}

object ThroneFeatureServiceSpec extends SimpleIOSuite:
  type EC[A] = Either[Throwable, A]

  test("apply places thrones and clears others") {
    val loader = new MapLayerLoaderImpl[IO]
    val throneService = new ThronePlacementServiceImpl[IO]
    val writer = new MapWriterImpl[IO]
    val service = new ThroneFeatureServiceImpl[IO](loader, throneService, writer)
    val config = ThroneFeatureConfig(
      randomLevelOne = Vector(ProvinceId(2)),
      randomLevelTwo = Vector(ProvinceId(3)),
      fixed = Vector(ThronePlacement(ProvinceId(4), ThroneLevel(2)))
    )
    for
      dir <- IO(Files.createTempDirectory("thrones"))
      in = dir.resolve("in.map")
      out = dir.resolve("out.map")
      _ <- IO(Files.write(in,
        """#dom2title test
#terrain 1 67108864
#terrain 2 0
#terrain 3 0
#terrain 4 0
""".getBytes(StandardCharsets.UTF_8)))
      resultEC <- service.apply[EC](Path.fromNioPath(in), config, Path.fromNioPath(out))
      _ <- IO.fromEither(resultEC)
      directives <- MapFileParser.parseFile[IO](Path.fromNioPath(out)).compile.toVector
      mask1 = directives.collectFirst { case Terrain(ProvinceId(1), m) => TerrainMask(m) }.get
      mask2 = directives.collectFirst { case Terrain(ProvinceId(2), m) => TerrainMask(m) }.get
      mask3 = directives.collectFirst { case Terrain(ProvinceId(3), m) => TerrainMask(m) }.get
      mask4 = directives.collectFirst { case Terrain(ProvinceId(4), m) => TerrainMask(m) }.get
    yield expect.all(
      !mask1.hasFlag(TerrainFlag.Throne),
      mask2.hasFlag(TerrainFlag.Throne),
      mask3.hasFlag(TerrainFlag.Throne),
      mask4.hasFlag(TerrainFlag.Throne)
    )
  }
