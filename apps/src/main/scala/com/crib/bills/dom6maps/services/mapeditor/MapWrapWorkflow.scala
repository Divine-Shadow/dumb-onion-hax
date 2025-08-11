package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.syntax.all.*
import cats.instances.either.*
import fs2.{Stream}
import fs2.io.file.Path
import model.map.{GroundSurfaceDuelConfig, MapFileParser, MapSizePixels}
import model.version.{UpdateStatus, Version}
import apps.services.update.Service as GithubReleaseChecker

trait MapWrapWorkflow:
  def run(cfg: PathsConfig): IO[Unit]

class MapWrapWorkflowImpl(
    finder: LatestEditorFinder[IO],
    copier: MapEditorCopier[IO],
    writer: MapWriter[IO],
    converter: WrapConversionService[IO],
    checker: GithubReleaseChecker[IO],
    chooser: WrapChoiceService[IO],
    nationChooser: GroundSurfaceNationService[IO],
    dueler: GroundSurfaceDuelPipe[IO],
    currentVersion: Version
  ) extends MapWrapWorkflow:
  private type ErrorOr[A] = Either[Throwable, A]

  override def run(cfg: PathsConfig): IO[Unit] =
    val srcRoot = Path.fromNioPath(cfg.source)
    val destRoot = Path.fromNioPath(cfg.dest)
    for
      updateCheck <- checker.checkForUpdate[ErrorOr](currentVersion)
      _ <- updateCheck match
        case Right(UpdateStatus.UpdateAvailable) =>
          IO.println("A new version of the app is available.")
        case Right(UpdateStatus.CurrentVersionIsLatest) => IO.unit
        case Left(_)                                   => IO.println("Checking for updates failed.")
      latestEC <- finder.mostRecentFolder[ErrorOr](srcRoot)
      latest <- IO.fromEither(latestEC)
      targetDir = destRoot / latest.fileName.toString
      res <- copier.copyWithoutMaps[ErrorOr](latest, targetDir)
      copied <- IO.fromEither(res)
      (bytes, outPath) = copied.main
      directives <- bytes.through(MapFileParser.parse[IO]).compile.toVector
      wrapEC <- chooser.chooseWraps[ErrorOr]()
      wrapChoices <- IO.fromEither(wrapEC)
      _ <- wrapChoices.main match
        case WrapChoice.GroundSurfaceDuel =>
          copied.cave match
            case Some((caveBytes, caveOutPath)) =>
              for
                caveDirectives <- caveBytes.through(MapFileParser.parse[IO]).compile.toVector
                nationsEC <- nationChooser.chooseNations[ErrorOr]()
                nations <- IO.fromEither(nationsEC)
                duelResEC <- dueler
                  .apply[ErrorOr](
                    Stream.emits(directives).covary[IO],
                    Stream.emits(caveDirectives).covary[IO],
                    GroundSurfaceDuelConfig.default,
                    nations.surface,
                    nations.underground
                  )
                duelRes <- IO.fromEither(duelResEC)
                (surfRes, caveRes) = duelRes
                surfWritten <- writer.write[ErrorOr](surfRes, outPath)
                _ <- IO.fromEither(surfWritten)
                caveWritten <- writer.write[ErrorOr](caveRes, caveOutPath)
                _ <- IO.fromEither(caveWritten)
              yield ()
            case None => IO.raiseError(new NoSuchElementException("cave map not found"))
        case wrapChoice =>
          for
            sizePixels <- IO.fromOption(directives.collectFirst { case m: MapSizePixels => m })(
              new NoSuchElementException("#mapsize not found")
            )
            provinceSize = sizePixels.toProvinceSize
            convertedEC <-
              converter.convert[ErrorOr](directives, provinceSize.width, provinceSize.height, wrapChoice)
            converted <- IO.fromEither(convertedEC)
            written <- writer.write[ErrorOr](converted, outPath)
            _ <- IO.fromEither(written)
            _ <- wrapChoices.cave match
              case Some(caveChoice) =>
                copied.cave match
                  case Some((caveBytes, caveOutPath)) =>
                    for
                      caveDirectives <- caveBytes.through(MapFileParser.parse[IO]).compile.toVector
                      caveSizePixels <- IO.fromOption(caveDirectives.collectFirst { case m: MapSizePixels => m })(
                        new NoSuchElementException("#mapsize not found")
                      )
                      caveProvince = caveSizePixels.toProvinceSize
                      caveConvertedEC <-
                        converter.convert[ErrorOr](caveDirectives, caveProvince.width, caveProvince.height, caveChoice)
                      caveConverted <- IO.fromEither(caveConvertedEC)
                      caveWritten <- writer.write[ErrorOr](caveConverted, caveOutPath)
                      _ <- IO.fromEither(caveWritten)
                    yield ()
                  case None => IO.raiseError(new NoSuchElementException("cave map not found"))
              case None => IO.unit
          yield ()
    yield ()
