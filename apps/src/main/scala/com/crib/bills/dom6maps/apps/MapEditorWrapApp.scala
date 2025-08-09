package com.crib.bills.dom6maps
package apps

import cats.effect.{ExitCode, IO, IOApp}
import cats.instances.either.*
import cats.syntax.all.*
import fs2.io.file.Path
import model.map.{MapFileParser, MapSizePixels}
import model.version.{UpdateStatus, Version}
import services.mapeditor.{LatestEditorFinderImpl, MapEditorCopierImpl, MapWriterImpl}
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

  override def run(args: List[String]): IO[ExitCode] =
    val finder = new LatestEditorFinderImpl[IO]
    val copier = new MapEditorCopierImpl[IO]
    val writer = new MapWriterImpl[IO]
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
        res <- copier.copyWithoutMap[ErrorOr](latest, targetDir)
        (bytes, outPath) <- IO.fromEither(res)
        directives <- bytes.through(MapFileParser.parse[IO]).compile.toVector
        sizePixels <- IO.fromOption(directives.collectFirst { case m: MapSizePixels => m })(
          new NoSuchElementException("#mapsize not found")
        )
        provinceSize = sizePixels.toProvinceSize
        severed = WrapSever.severVertically(directives, provinceSize.width, provinceSize.height)
        written <- writer.write[ErrorOr](severed, outPath)
        _ <- IO.fromEither(written)
      yield ExitCode.Success
    action

