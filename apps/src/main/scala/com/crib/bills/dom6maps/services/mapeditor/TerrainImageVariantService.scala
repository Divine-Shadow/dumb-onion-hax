package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.effect.Async
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}
import model.map.{MapDirective, MapLayer, MapSizePixels, MapWidthPixels, MapHeightPixels, Pb}
import model.map.generation.TerrainImageVariantPolicy
import model.map.image.{ProvincePixelRasterizer, TargaImageEncoder, TerrainVariantPainter}

trait TerrainImageVariantService[Sequencer[_]]:
  def writeVariants[ErrorChannel[_]](
      layer: MapLayer[Sequencer],
      baseImagePath: Path,
      policy: TerrainImageVariantPolicy
  )(using
      files: Files[Sequencer],
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Unit]]

class TerrainImageVariantServiceImpl[Sequencer[_]: Async: Files] extends TerrainImageVariantService[Sequencer]:
  private val sequencer = summon[Async[Sequencer]]

  override def writeVariants[ErrorChannel[_]](
      layer: MapLayer[Sequencer],
      baseImagePath: Path,
      policy: TerrainImageVariantPolicy
  )(using
      files: Files[Sequencer],
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Unit]] =
    (for
      passThroughDirectives <- layer.passThrough.compile.toVector
      provinceRuns <- sequencer.pure(passThroughDirectives.collect { case provinceRun: Pb => provinceRun })
      mapSizePixels <- sequencer.fromEither(resolveMapSizePixels(layer, passThroughDirectives, provinceRuns))
      ownership <- sequencer.fromEither(
        ProvincePixelRasterizer.rasterize(
          mapSizePixels.width.value,
          mapSizePixels.height.value,
          provinceRuns
        )
      )
      terrainMaskByProvince = layer.state.terrains.map(terrain => terrain.province -> terrain.mask).toMap
      _ <- policy match
        case TerrainImageVariantPolicy.BaseOnly => sequencer.unit
        case TerrainImageVariantPolicy.BaseAndWinter =>
          val winterImage = TerrainVariantPainter.paintVariant(
            ownership,
            terrainMaskByProvince,
            TerrainVariantPainter.TerrainVariantKind.Plain,
            winterLook = true
          )
          writeImage(winterImagePath(baseImagePath), winterImage)
        case TerrainImageVariantPolicy.FullTerrainSet =>
          val winterImage = TerrainVariantPainter.paintVariant(
            ownership,
            terrainMaskByProvince,
            TerrainVariantPainter.TerrainVariantKind.Plain,
            winterLook = true
          )
          val terrainKinds = Vector(
            TerrainVariantPainter.TerrainVariantKind.Forest,
            TerrainVariantPainter.TerrainVariantKind.Waste,
            TerrainVariantPainter.TerrainVariantKind.Farm,
            TerrainVariantPainter.TerrainVariantKind.Swamp,
            TerrainVariantPainter.TerrainVariantKind.Highland,
            TerrainVariantPainter.TerrainVariantKind.Plain,
            TerrainVariantPainter.TerrainVariantKind.Kelp,
            TerrainVariantPainter.TerrainVariantKind.Water
          )
          for
            _ <- writeImage(winterImagePath(baseImagePath), winterImage)
            _ <- terrainKinds.traverse_ { terrainVariantKind =>
              val variantName = terrainVariantSuffix(terrainVariantKind)
              val normalImage = TerrainVariantPainter.paintVariant(
                ownership,
                terrainMaskByProvince,
                terrainVariantKind,
                winterLook = false
              )
              val winterVariantImage = TerrainVariantPainter.paintVariant(
                ownership,
                terrainMaskByProvince,
                terrainVariantKind,
                winterLook = true
              )
              for
                _ <- writeImage(variantImagePath(baseImagePath, variantName, winterLook = false), normalImage)
                _ <- writeImage(variantImagePath(baseImagePath, variantName, winterLook = true), winterVariantImage)
              yield ()
            }
          yield ()
    yield ()).attempt.map {
      case Left(error) => errorChannel.raiseError[Unit](error)
      case Right(value) => errorChannel.pure(value)
    }

  private def resolveMapSizePixels(
      layer: MapLayer[Sequencer],
      passThroughDirectives: Vector[MapDirective],
      provinceRuns: Vector[Pb]
  ): Either[Throwable, MapSizePixels] =
    inferMapSizePixelsFromProvinceRuns(provinceRuns)
      .orElse(passThroughDirectives.collectFirst { case mapSizePixels: MapSizePixels => mapSizePixels })
      .orElse(layer.state.size.map { stateSize =>
        MapSizePixels(
          MapWidthPixels(stateSize.width.value * 256),
          MapHeightPixels(stateSize.height.value * 160)
        )
      })
      .orElse(inferMapSizePixelsFromProvinceRuns(provinceRuns))
      .toRight(IllegalArgumentException("Cannot generate terrain image variants without map dimensions"))

  private def inferMapSizePixelsFromProvinceRuns(provinceRuns: Vector[Pb]): Option[MapSizePixels] =
    if provinceRuns.isEmpty then None
    else
      val maxXExclusive = provinceRuns.map(provinceRun => provinceRun.x + provinceRun.length).max
      val maxYExclusive = provinceRuns.map(provinceRun => provinceRun.y + 1).max
      if maxXExclusive <= 0 || maxYExclusive <= 0 then None
      else Some(MapSizePixels(MapWidthPixels(maxXExclusive), MapHeightPixels(maxYExclusive)))

  private def writeImage(path: Path, image: model.map.image.MapImagePainter.MapImage): Sequencer[Unit] =
    for
      bytes <- sequencer.fromEither(
        TargaImageEncoder.encodeRleBottomLeft24Bit(
          image.widthPixels,
          image.heightPixels,
          image.redGreenBlueBytes
        )
      )
      _ <- Files[Sequencer].createDirectories(path.parent.getOrElse(path))
      _ <- Files[Sequencer]
        .writeAll(path)
        .apply(Stream.emits(bytes).covary[Sequencer])
        .compile
        .drain
    yield ()

  private def winterImagePath(baseImagePath: Path): Path =
    val stem = baseNameWithoutExtension(baseImagePath.fileName.toString)
    val parent = baseImagePath.parent.getOrElse(baseImagePath)
    parent / s"${stem}_winter.tga"

  private def variantImagePath(baseImagePath: Path, terrainVariantName: String, winterLook: Boolean): Path =
    val stem = baseNameWithoutExtension(baseImagePath.fileName.toString)
    val winterSuffix = if winterLook then "w" else ""
    val parent = baseImagePath.parent.getOrElse(baseImagePath)
    parent / s"${stem}_${terrainVariantName}${winterSuffix}.tga"

  private def baseNameWithoutExtension(fileName: String): String =
    val extensionIndex = fileName.lastIndexOf('.')
    if extensionIndex <= 0 then fileName else fileName.substring(0, extensionIndex)

  private def terrainVariantSuffix(terrainVariantKind: TerrainVariantPainter.TerrainVariantKind): String =
    terrainVariantKind match
      case TerrainVariantPainter.TerrainVariantKind.Forest => "forest"
      case TerrainVariantPainter.TerrainVariantKind.Waste => "waste"
      case TerrainVariantPainter.TerrainVariantKind.Farm => "farm"
      case TerrainVariantPainter.TerrainVariantKind.Swamp => "swamp"
      case TerrainVariantPainter.TerrainVariantKind.Highland => "highland"
      case TerrainVariantPainter.TerrainVariantKind.Plain => "plain"
      case TerrainVariantPainter.TerrainVariantKind.Kelp => "kelp"
      case TerrainVariantPainter.TerrainVariantKind.Water => "water"
