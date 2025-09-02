package com.crib.bills.dom6maps
package apps

import cats.effect.IO
import cats.syntax.all.*
import cats.{MonadError, Traverse}
import fs2.io.file.Path
import model.map.{MapFileParser, MapState, FeatureId, SetLand, Feature}
import cats.instances.either.*
import services.mapeditor.{
  ThronePlacementServiceImpl,
  MapLayerLoaderImpl,
  MapWriterImpl,
  ThroneFeatureServiceImpl
}
import weaver.SimpleIOSuite
import java.nio.file.{Files => JFiles, Path => JPath}

object FullWrapThronePlacementSpec extends SimpleIOSuite:
  override def maxParallelism = 1
  private type ErrorOr[A] = Either[Throwable, A]

  test("places overridden thrones for full wrap") {
    val overrides =
      """overrides = [
        { x = 0, y = 0, level = 2 },
        { x = 6, y = 0, level = 2 },
        { x = 0, y = 6, level = 2 },
        { x = 6, y = 6, level = 2 },

        { x = 3, y = 0, level = 1 },
        { x = 7, y = 0, level = 1 },
        { x = 3, y = 6, level = 1 },
        { x = 7, y = 6, level = 1 },

        { x = 0, y = 3, level = 1 },
        { x = 6, y = 3, level = 1 },
        { x = 0, y = 7, level = 1 },
        { x = 6, y = 7, id = 1338 }
      ]
      """.stripMargin
    val placements = Vector(
      (0,0,Some(2),None),(6,0,Some(2),None),(3,0,Some(2),None),(7,0,Some(2),None),
      (1,0,Some(1),None),(5,0,Some(1),None),
      (0,7,Some(1),None),(6,7,Some(1),None),(3,7,Some(1),None),(7,7,Some(1),None),
      (1,7,Some(1),None),(5,7,None,Some(1338))
    ).map { case (x,y,levelOpt,idOpt) =>
      val loc = model.map.ProvinceLocation(model.map.XCell(x), model.map.YCell(y))
      levelOpt match
        case Some(lv) => model.map.ThronePlacement(loc, model.map.ThroneLevel(lv))
        case None     => model.map.ThronePlacement(loc, FeatureId(idOpt.get))
    }
    val cfg = model.map.ThroneFeatureConfig(Vector.empty, Vector.empty, placements)
    for
      srcDir <- IO(JFiles.createTempDirectory("wrap-src"))
      latest <- IO(JFiles.createDirectory(srcDir.resolve("latest")))
      inPath = latest.resolve("map.map")
      _ <- IO(JFiles.copy(Path("data/eight-by-eight.map").toNioPath, inPath))
      outDir <- IO(JFiles.createTempDirectory("wrap-dest"))
      outPath = outDir.resolve("map.map")
      svc = new ThroneFeatureServiceImpl[IO](new MapLayerLoaderImpl[IO], new ThronePlacementServiceImpl[IO], new MapWriterImpl[IO])
      _ <- svc.apply[ErrorOr](Path.fromNioPath(inPath), cfg, Path.fromNioPath(outPath)).flatMap(IO.fromEither)
      directives <- MapFileParser.parseFile[IO](Path.fromNioPath(outPath)).compile.toVector
      reconstructed =
        directives.foldLeft((Option.empty[model.ProvinceId], Vector.empty[(model.ProvinceId, FeatureId)])) {
          case ((cur, acc), SetLand(p))   => (Some(p), acc)
          case ((Some(p), acc), Feature(fid)) => (Some(p), acc :+ (p -> fid))
          case ((c, acc), _) => (c, acc)
        }._2
    yield expect.all(reconstructed.length == 12, reconstructed.exists(_._2 == FeatureId(1338)))
  }
