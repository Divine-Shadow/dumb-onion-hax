package com.crib.bills.dom6maps
package apps

import cats.effect.{ExitCode, IO, IOApp}
import cats.instances.either.*
import cats.syntax.all.*
import fs2.io.file.Path
import model.map.{MapFileParser, MapSizePixels}
import model.version.{UpdateStatus, Version}
import services.mapeditor.{
  LatestEditorFinderImpl,
  MapEditorCopierImpl,
  MapWriterImpl,
  WrapChoiceService,
  WrapChoiceServiceImpl,
  WrapConversionServiceImpl,
  WrapChoices
}
import services.update.GithubReleaseCheckerImpl
import pureconfig.*
import pureconfig.generic.derivation.default.*
import java.nio.file.{Files as JFiles, Path as NioPath}

object MapEditorWrapApp extends IOApp:
  private type ErrorOr[A] = Either[Throwable, A]

  private final case class PathsConfig(source: NioPath, dest: NioPath) derives ConfigReader

  private val configFileName = "map-editor-wrap.conf"
  private val sampleConfig =
    """source="/path/to/mapnuke/output"
dest="/path/to/dominions/maps"
"""

  private val currentVersion = Version("1.1")

  def runWith(chooser: WrapChoiceService[IO]): IO[ExitCode] =
    val finder = new LatestEditorFinderImpl[IO]
    val copier = new MapEditorCopierImpl[IO]
    val writer = new MapWriterImpl[IO]
    val converter = new WrapConversionServiceImpl[IO]
    val checker = new GithubReleaseCheckerImpl[IO]
    val action =
      for
        updateCheck <- checker.checkForUpdate[ErrorOr](currentVersion)
        _ <- updateCheck match
          case Right(UpdateStatus.UpdateAvailable) =>
            IO.println("A new version of the app is available.")
          case Right(UpdateStatus.CurrentVersionIsLatest) =>
            IO.unit
          case Left(_) =>
            IO.println("Checking for updates failed.")
        exists <- IO(JFiles.exists(NioPath.of(configFileName)))
        _ <-
          if exists then IO.unit
          else
            IO(JFiles.writeString(NioPath.of(configFileName), sampleConfig)) *>
              IO.raiseError(new RuntimeException(s"$configFileName created; please edit and rerun"))
        cfg <- IO(ConfigSource.file(configFileName).loadOrThrow[PathsConfig])
        srcRoot = Path.fromNioPath(cfg.source)
        destRoot = Path.fromNioPath(cfg.dest)
        latestEC <- finder.mostRecentFolder[ErrorOr](srcRoot)
        latest <- IO.fromEither(latestEC)
        targetDir = destRoot / latest.fileName.toString
        res <- copier.copyWithoutMaps[ErrorOr](latest, targetDir)
        copied <- IO.fromEither(res)
        (bytes, outPath) = copied.main
        directives <- bytes.through(MapFileParser.parse[IO]).compile.toVector
        sizePixels <- IO.fromOption(directives.collectFirst { case m: MapSizePixels => m })(
          new NoSuchElementException("#mapsize not found")
        )
        provinceSize = sizePixels.toProvinceSize
        wrapEC <- chooser.chooseWraps[ErrorOr]()
        wrapChoices <- IO.fromEither(wrapEC)
        convertedEC <-
          converter.convert[ErrorOr](directives, provinceSize.width, provinceSize.height, wrapChoices.main)
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
                  caveConvertedEC <- converter.convert[ErrorOr](
                    caveDirectives,
                    caveProvince.width,
                    caveProvince.height,
                    caveChoice
                  )
                  caveConverted <- IO.fromEither(caveConvertedEC)
                  caveWritten <- writer.write[ErrorOr](caveConverted, caveOutPath)
                  _ <- IO.fromEither(caveWritten)
                yield ()
              case None => IO.raiseError(new NoSuchElementException("cave map not found"))
          case None => IO.unit
      yield ExitCode.Success
    action

  override def run(args: List[String]): IO[ExitCode] =
    runWith(new WrapChoiceServiceImpl[IO])
