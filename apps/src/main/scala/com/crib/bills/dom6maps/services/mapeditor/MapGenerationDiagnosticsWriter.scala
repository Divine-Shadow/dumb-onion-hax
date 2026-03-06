package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.effect.Async
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}
import model.ProvinceId
import model.map.{MapDirective, MapLayer, MapSizePixels, MapWidthPixels, MapHeightPixels, Pb}
import model.map.image.{MapImagePainter, ProvinceAnchorLocator, ProvincePixelRasterizer, TargaImageEncoder}

import java.nio.charset.StandardCharsets
import scala.collection.mutable

trait MapGenerationDiagnosticsWriter[Sequencer[_]]:
  def write[ErrorChannel[_]](
      layer: MapLayer[Sequencer],
      mapName: String,
      outputBundleDirectory: Path
  )(using
      files: Files[Sequencer],
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Unit]]

class MapGenerationDiagnosticsWriterImpl[Sequencer[_]: Async: Files] extends MapGenerationDiagnosticsWriter[Sequencer]:
  private val sequencer = summon[Async[Sequencer]]

  override def write[ErrorChannel[_]](
      layer: MapLayer[Sequencer],
      mapName: String,
      outputBundleDirectory: Path
  )(using
      files: Files[Sequencer],
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Unit]] =
    (for
      passThroughDirectives <- layer.passThrough.compile.toVector
      provinceRuns <- sequencer.pure(passThroughDirectives.collect { case run: Pb => run })
      mapSizePixels <- sequencer.fromEither(resolveMapSizePixels(layer, passThroughDirectives, provinceRuns))
      ownership <- sequencer.fromEither(
        ProvincePixelRasterizer.rasterize(
          mapSizePixels.width.value,
          mapSizePixels.height.value,
          provinceRuns
        )
      )
      reportText <- sequencer.fromEither(buildReportText(ownership, provinceRuns))
      overlayBytes <- sequencer.fromEither(buildOverlayBytes(ownership, provinceRuns))
      reportPath = outputBundleDirectory / s"${mapName}_debug_anchors.txt"
      overlayPath = outputBundleDirectory / s"${mapName}_debug_anchors.tga"
      _ <- sequencer.delay(println(s"Writing map diagnostics report to $reportPath"))
      _ <- Files[Sequencer].createDirectories(outputBundleDirectory)
      _ <- Files[Sequencer]
        .writeAll(reportPath)
        .apply(Stream.emits(reportText.getBytes(StandardCharsets.UTF_8)).covary[Sequencer])
        .compile
        .drain
      _ <- sequencer.delay(println(s"Writing map diagnostics overlay to $overlayPath"))
      _ <- Files[Sequencer]
        .writeAll(overlayPath)
        .apply(Stream.emits(overlayBytes).covary[Sequencer])
        .compile
        .drain
    yield ()).attempt.map {
      case Left(error) => errorChannel.raiseError[Unit](error)
      case Right(value) => errorChannel.pure(value)
    }

  private final case class CandidatePoint(xPixel: Int, yPixelTopOrigin: Int)

  private final case class ProvinceDiagnostics(
      provinceId: ProvinceId,
      pixelCount: Long,
      firstRunPoint: Option[CandidatePoint],
      longestRunPoint: Option[CandidatePoint],
      centroidPoint: Option[CandidatePoint],
      anchorPoint: Option[CandidatePoint]
  )

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
      .toRight(IllegalArgumentException("Cannot write diagnostics without #mapsize directive or #pb runs"))

  private def inferMapSizePixelsFromProvinceRuns(
      provinceRuns: Vector[Pb]
  ): Option[MapSizePixels] =
    if provinceRuns.isEmpty then None
    else
      val maxXExclusive = provinceRuns.map(run => run.x + run.length).max
      val maxYExclusive = provinceRuns.map(run => run.y + 1).max
      if maxXExclusive <= 0 || maxYExclusive <= 0 then None
      else Some(MapSizePixels(MapWidthPixels(maxXExclusive), MapHeightPixels(maxYExclusive)))

  private def buildReportText(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      provinceRuns: Vector[Pb]
  ): Either[Throwable, String] =
    val provinceDiagnostics = computeProvinceDiagnostics(ownership, provinceRuns)
    val header =
      "province_id,pixel_count,first_run_x,first_run_y_top,longest_run_mid_x,longest_run_mid_y_top,centroid_x,centroid_y_top,anchor_x,anchor_y_top\n"
    val rows = provinceDiagnostics.map { diagnostics =>
      def pointOrEmpty(point: Option[CandidatePoint]): String =
        point match
          case Some(value) => s"${value.xPixel},${value.yPixelTopOrigin}"
          case None => ","

      val firstRun = pointOrEmpty(diagnostics.firstRunPoint)
      val longestRun = pointOrEmpty(diagnostics.longestRunPoint)
      val centroid = pointOrEmpty(diagnostics.centroidPoint)
      val anchor = pointOrEmpty(diagnostics.anchorPoint)
      s"${diagnostics.provinceId.value},${diagnostics.pixelCount},$firstRun,$longestRun,$centroid,$anchor"
    }
    Right(header + rows.mkString("\n") + "\n")

  private def computeProvinceDiagnostics(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      provinceRuns: Vector[Pb]
  ): Vector[ProvinceDiagnostics] =
    val maxProvinceIdentifier = ownership.provinceIdentifierByPixel.foldLeft(0)(math.max)

    val firstRunByProvince = mutable.HashMap.empty[Int, CandidatePoint]
    val longestRunByProvince = mutable.HashMap.empty[Int, Pb]
    provinceRuns.foreach { run =>
      firstRunByProvince.getOrElseUpdate(
        run.province.value,
        CandidatePoint(run.x, (ownership.heightPixels - 1) - run.y)
      )
      longestRunByProvince.get(run.province.value) match
        case Some(existing) if existing.length >= run.length => ()
        case _ => longestRunByProvince.update(run.province.value, run)
    }

    val pixelCountByProvince = Array.fill[Long](maxProvinceIdentifier + 1)(0L)
    val sumXByProvince = Array.fill[Long](maxProvinceIdentifier + 1)(0L)
    val sumYByProvince = Array.fill[Long](maxProvinceIdentifier + 1)(0L)

    var yPixel = 0
    while yPixel < ownership.heightPixels do
      var xPixel = 0
      while xPixel < ownership.widthPixels do
        val provinceIdentifier = ownership.provinceIdentifierAt(xPixel, yPixel)
        if provinceIdentifier > 0 then
          pixelCountByProvince(provinceIdentifier) += 1L
          sumXByProvince(provinceIdentifier) += xPixel.toLong
          sumYByProvince(provinceIdentifier) += yPixel.toLong
        xPixel += 1
      yPixel += 1

    val anchorPixelByProvince = ProvinceAnchorLocator.locateAnchorPixelByProvince(ownership)

    (1 to maxProvinceIdentifier).toVector.flatMap { provinceIdentifier =>
      val pixelCount = pixelCountByProvince(provinceIdentifier)
      if pixelCount <= 0 then None
      else
        val centroidX = math.round(sumXByProvince(provinceIdentifier).toDouble / pixelCount.toDouble).toInt
        val centroidY = math.round(sumYByProvince(provinceIdentifier).toDouble / pixelCount.toDouble).toInt
        val firstRunPoint = firstRunByProvince.get(provinceIdentifier)
        val longestRunPoint = longestRunByProvince.get(provinceIdentifier).map { run =>
          CandidatePoint(run.x + (run.length / 2), (ownership.heightPixels - 1) - run.y)
        }
        val anchorPoint = anchorPixelByProvince.get(provinceIdentifier).map { pixelIndex =>
          CandidatePoint(
            xPixel = pixelIndex % ownership.widthPixels,
            yPixelTopOrigin = pixelIndex / ownership.widthPixels
          )
        }
        Some(
          ProvinceDiagnostics(
            provinceId = ProvinceId(provinceIdentifier),
            pixelCount = pixelCount,
            firstRunPoint = firstRunPoint,
            longestRunPoint = longestRunPoint,
            centroidPoint = Some(CandidatePoint(centroidX, centroidY)),
            anchorPoint = anchorPoint
          )
        )
    }

  private def buildOverlayBytes(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      provinceRuns: Vector[Pb]
  ): Either[Throwable, Array[Byte]] =
    val diagnostics = computeProvinceDiagnostics(ownership, provinceRuns)
    val baseImage = MapImagePainter.paintWithProvinceColor(
      ownership = ownership,
      provinceColor = _ => MapImagePainter.RedGreenBlueColor(60, 60, 60),
      backgroundColor = MapImagePainter.RedGreenBlueColor(0, 0, 0),
      borderColor = MapImagePainter.RedGreenBlueColor(120, 120, 120),
      provinceAnchorColor = MapImagePainter.RedGreenBlueColor(255, 255, 255)
    )
    val bytes = baseImage.redGreenBlueBytes.clone()

    diagnostics.foreach { diagnosticsEntry =>
      paintCross(bytes, baseImage.widthPixels, baseImage.heightPixels, diagnosticsEntry.firstRunPoint, MapImagePainter.RedGreenBlueColor(255, 0, 0), 4)
      paintCross(bytes, baseImage.widthPixels, baseImage.heightPixels, diagnosticsEntry.longestRunPoint, MapImagePainter.RedGreenBlueColor(255, 165, 0), 4)
      paintCross(bytes, baseImage.widthPixels, baseImage.heightPixels, diagnosticsEntry.centroidPoint, MapImagePainter.RedGreenBlueColor(0, 255, 0), 4)
      paintCross(bytes, baseImage.widthPixels, baseImage.heightPixels, diagnosticsEntry.anchorPoint, MapImagePainter.RedGreenBlueColor(255, 0, 255), 4)
    }

    TargaImageEncoder.encodeRawBottomLeft24Bit(
      baseImage.widthPixels,
      baseImage.heightPixels,
      bytes
    )

  private def paintCross(
      bytes: Array[Byte],
      widthPixels: Int,
      heightPixels: Int,
      point: Option[CandidatePoint],
      color: MapImagePainter.RedGreenBlueColor,
      radiusPixels: Int
  ): Unit =
    point.foreach { value =>
      var offset = -radiusPixels
      while offset <= radiusPixels do
        paintPixel(bytes, widthPixels, heightPixels, value.xPixel + offset, value.yPixelTopOrigin, color)
        paintPixel(bytes, widthPixels, heightPixels, value.xPixel, value.yPixelTopOrigin + offset, color)
        offset += 1
    }

  private def paintPixel(
      bytes: Array[Byte],
      widthPixels: Int,
      heightPixels: Int,
      xPixel: Int,
      yPixelTopOrigin: Int,
      color: MapImagePainter.RedGreenBlueColor
  ): Unit =
    if xPixel >= 0 && xPixel < widthPixels && yPixelTopOrigin >= 0 && yPixelTopOrigin < heightPixels then
      val pixelIndex = yPixelTopOrigin * widthPixels + xPixel
      val byteIndex = pixelIndex * 3
      bytes(byteIndex) = color.red.toByte
      bytes(byteIndex + 1) = color.green.toByte
      bytes(byteIndex + 2) = color.blue.toByte
