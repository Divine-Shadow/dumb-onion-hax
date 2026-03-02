package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.syntax.all.*
import cats.effect.Async
import fs2.io.file.{Files, Path}
import fs2.Stream
import model.map.{ImageFile, MapDirective, MapLayer, MapSizePixels, MapWidthPixels, MapHeightPixels, Pb}
import model.map.image.{
  BorderFlagMapConnectionOverlayPainter,
  MapConnectionOverlayPainter,
  MapImagePainter,
  MapTerrainPainter,
  ProvinceAnchorLocator,
  PrimaryTerrainColorMapTerrainPainter,
  ProvincePixelRasterizer,
  TargaImageEncoder
}

trait MapImageWriter[Sequencer[_]]:
  def writeMainImage[ErrorChannel[_]](
      layer: MapLayer[Sequencer],
      outputImagePath: Path
  )(using
      files: Files[Sequencer],
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Unit]]

class MapImageWriterImpl[Sequencer[_]: Async: Files](
    mapTerrainPainter: MapTerrainPainter = new PrimaryTerrainColorMapTerrainPainter(),
    mapConnectionOverlayPainter: MapConnectionOverlayPainter = new BorderFlagMapConnectionOverlayPainter()
) extends MapImageWriter[Sequencer]:
  private val sequencer = summon[Async[Sequencer]]

  override def writeMainImage[ErrorChannel[_]](
      layer: MapLayer[Sequencer],
      outputImagePath: Path
  )(using
      files: Files[Sequencer],
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Unit]] =
    (for
      passThroughDirectives <- layer.passThrough.compile.toVector
      _ <- sequencer.delay(println(s"Writing map image to $outputImagePath"))
      provinceRuns <- sequencer.pure(passThroughDirectives.collect { case run: Pb => run })
      mapSizePixels <- sequencer.fromEither(resolveMapSizePixels(layer, passThroughDirectives, provinceRuns))
      ownership <- sequencer.fromEither(
        ProvincePixelRasterizer.rasterize(
          mapSizePixels.width.value,
          mapSizePixels.height.value,
          provinceRuns
        )
      )
      terrainMaskByProvince = layer.state.terrains.map(t => t.province -> t.mask).toMap
      baseImage = mapTerrainPainter.paint(ownership, terrainMaskByProvince)
      overlaidImage = mapConnectionOverlayPainter.paint(baseImage, ownership, layer.state.borders)
      paintedImage = repaintProvinceAnchors(overlaidImage, ownership)
      bytes <- sequencer.fromEither(
        TargaImageEncoder.encodeRawBottomLeft24Bit(
          paintedImage.widthPixels,
          paintedImage.heightPixels,
          paintedImage.redGreenBlueBytes
        )
      )
      _ <- Files[Sequencer].createDirectories(outputImagePath.parent.getOrElse(outputImagePath))
      _ <- Files[Sequencer]
        .writeAll(outputImagePath)
        .apply(Stream.emits(bytes).covary[Sequencer])
        .compile
        .drain
    yield ()).attempt.map {
      case Left(e)      => errorChannel.raiseError[Unit](e)
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
          MapWidthPixels(stateSize.value * 256),
          MapHeightPixels(stateSize.value * 160)
        )
      })
      .orElse(inferMapSizePixelsFromProvinceRuns(provinceRuns))
      .toRight(IllegalArgumentException("Cannot generate image without #mapsize directive or #pb runs"))

  private def inferMapSizePixelsFromProvinceRuns(
      provinceRuns: Vector[Pb]
  ): Option[MapSizePixels] =
    if provinceRuns.isEmpty then None
    else
      val maxXExclusive = provinceRuns.map(run => run.x + run.length).max
      val maxYExclusive = provinceRuns.map(run => run.y + 1).max
      if maxXExclusive <= 0 || maxYExclusive <= 0 then None
      else Some(MapSizePixels(MapWidthPixels(maxXExclusive), MapHeightPixels(maxYExclusive)))

  private def repaintProvinceAnchors(
      image: MapImagePainter.MapImage,
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership
  ): MapImagePainter.MapImage =
    val bytes = image.redGreenBlueBytes.clone()
    val anchorColor = MapImagePainter.defaultPalette.provinceAnchorColor
    val anchorByProvince = ProvinceAnchorLocator.locateAnchorPixelByProvince(ownership)
    anchorByProvince.values.foreach { pixelIndex =>
      val byteIndex = pixelIndex * 3
      bytes(byteIndex) = anchorColor.red.toByte
      bytes(byteIndex + 1) = anchorColor.green.toByte
      bytes(byteIndex + 2) = anchorColor.blue.toByte
    }
    MapImagePainter.MapImage(image.widthPixels, image.heightPixels, bytes)

object MapImageWriter:
  def resolveImagePath(
      mapOutputPath: Path,
      passThroughDirectives: Vector[MapDirective]
  ): Path =
    val fileName =
      passThroughDirectives.collectFirst { case ImageFile(value) => value }
        .filter(_.trim.nonEmpty)
        .getOrElse(defaultImageNameForMap(mapOutputPath))

    val candidate = Path(fileName)
    if candidate.isAbsolute then candidate
    else mapOutputPath.parent.getOrElse(mapOutputPath) / fileName

  private def defaultImageNameForMap(mapOutputPath: Path): String =
    val mapFileName = mapOutputPath.fileName.toString
    if mapFileName.toLowerCase.endsWith(".map") then mapFileName.dropRight(4) + ".tga"
    else mapFileName + ".tga"
