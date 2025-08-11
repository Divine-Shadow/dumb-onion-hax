package com.crib.bills.dom6maps
package apps

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*
import model.version.Version
import services.mapeditor.{
  LatestEditorFinderImpl,
  MapEditorCopierImpl,
  MapWriterImpl,
  MapWrapWorkflowImpl,
  PathsConfig,
  WrapChoiceService,
  WrapChoiceServiceImpl,
  WrapConversionServiceImpl,
  GroundSurfaceDuelPipeImpl,
  MapSizeValidatorImpl,
  PlacementPlannerImpl,
  GateDirectiveServiceImpl,
  ThronePlacementServiceImpl,
  GroundSurfaceNationServiceImpl,
  GroundSurfaceNationService,
  SpawnPlacementServiceImpl,
  GroundSurfaceDuelPipe
}
import services.update.GithubReleaseCheckerImpl
import pureconfig.*
import java.nio.file.{Files as JFiles, Path as NioPath}

object MapEditorWrapApp extends IOApp:
  private val configFileName = "map-editor-wrap.conf"
  private val sampleConfig =
    """source="/path/to/mapnuke/output"
dest="/path/to/dominions/maps"
"""

  private val currentVersion = Version("1.1")

  def runWith(
      chooser: WrapChoiceService[IO],
      nationChooser: GroundSurfaceNationService[IO],
      dueler: GroundSurfaceDuelPipe[IO]
  ): IO[ExitCode] =
    val finder = new LatestEditorFinderImpl[IO]
    val copier = new MapEditorCopierImpl[IO]
    val writer = new MapWriterImpl[IO]
    val converter = new WrapConversionServiceImpl[IO]
    val checker = new GithubReleaseCheckerImpl[IO]
    val workflow =
      new MapWrapWorkflowImpl(finder, copier, writer, converter, checker, chooser, nationChooser, dueler, currentVersion)
    val action =
      for
        exists <- IO(JFiles.exists(NioPath.of(configFileName)))
        _ <-
          if exists then IO.unit
          else
            IO(JFiles.writeString(NioPath.of(configFileName), sampleConfig)) *>
              IO.raiseError(new RuntimeException(s"$configFileName created; please edit and rerun"))
        cfg <- IO(ConfigSource.file(configFileName).loadOrThrow[PathsConfig])
        _ <- workflow.run(cfg)
      yield ExitCode.Success
    action

  override def run(args: List[String]): IO[ExitCode] =
    val dueler = new GroundSurfaceDuelPipeImpl[IO](
      new MapSizeValidatorImpl[IO],
      new PlacementPlannerImpl[IO],
      new GateDirectiveServiceImpl[IO],
      new ThronePlacementServiceImpl[IO],
      new SpawnPlacementServiceImpl[IO]
    )
    runWith(new WrapChoiceServiceImpl[IO], new GroundSurfaceNationServiceImpl[IO], dueler)
