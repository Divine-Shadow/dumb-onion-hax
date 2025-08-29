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
import model.map.{
  Feature,
  FeatureId,
  MapFileParser,
  ProvinceLocation,
  SetLand,
  Terrain,
  ThroneFeatureConfig,
  ThronePlacement,
  ThroneLevel,
  XCell,
  YCell
}

object ThroneFeatureServiceSpec extends SimpleIOSuite:
  type EC[A] = Either[Throwable, A]

  test("apply places thrones and clears others") {
    val loader = new MapLayerLoaderImpl[IO]
    val throneService = new ThronePlacementServiceImpl[IO]
    val writer = new MapWriterImpl[IO]
    val service = new ThroneFeatureServiceImpl[IO](loader, throneService, writer)
    val config = ThroneFeatureConfig(
      randomLevelOne = Vector(ProvinceLocation(XCell(1), YCell(0))),
      randomLevelTwo = Vector(ProvinceLocation(XCell(2), YCell(0))),
      fixed = Vector(ThronePlacement(ProvinceLocation(XCell(3), YCell(0)), ThroneLevel(2)))
    )
    for
      dir <- IO(Files.createTempDirectory("thrones"))
      in = dir.resolve("in.map")
      out = dir.resolve("out.map")
      _ <- IO(Files.write(in,
        """#dom2title test
#mapsize 1280 800
#pb 0 0 1 1
#pb 256 0 1 2
#pb 512 0 1 3
#pb 768 0 1 4
#terrain 1 67108864
#terrain 2 0
#terrain 3 0
#terrain 4 0
""".getBytes(StandardCharsets.UTF_8)))
      resultEC <- service.apply[EC](Path.fromNioPath(in), config, Path.fromNioPath(out))
      _ <- IO.fromEither(resultEC)
      directives <- MapFileParser.parseFile[IO](Path.fromNioPath(out)).compile.toVector
      featureAt = (p: ProvinceId) =>
        directives.sliding(2).collectFirst {
          case Vector(SetLand(id), Feature(f)) if id == p => f
        }
      mask1 = directives.collectFirst { case Terrain(ProvinceId(1), m) => TerrainMask(m) }.get
      mask2 = directives.collectFirst { case Terrain(ProvinceId(2), m) => TerrainMask(m) }.get
      mask3 = directives.collectFirst { case Terrain(ProvinceId(3), m) => TerrainMask(m) }.get
      mask4 = directives.collectFirst { case Terrain(ProvinceId(4), m) => TerrainMask(m) }.get
      f2 = featureAt(ProvinceId(2))
      f3 = featureAt(ProvinceId(3))
      f4 = featureAt(ProvinceId(4))
    yield expect.all(
      !mask1.hasFlag(TerrainFlag.Throne),
      mask2.hasFlag(TerrainFlag.Throne),
      mask3.hasFlag(TerrainFlag.Throne),
      mask4.hasFlag(TerrainFlag.Throne),
      f2.contains(FeatureId(5001)),
      f3.contains(FeatureId(5002)),
      f4.contains(FeatureId(5002))
    )
  }
