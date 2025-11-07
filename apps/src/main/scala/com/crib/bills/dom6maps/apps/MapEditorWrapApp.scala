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
  GroundSurfaceDuelPipe,
  MapLayerLoaderImpl,
  MapLayerLoader,
  ThronePlacementService
}
import services.update.GithubReleaseCheckerImpl
import pureconfig.*
import apps.util.PathUtils
import java.nio.file.{Files as JFiles, Path as NioPath}

object MapEditorWrapApp extends IOApp:
  private def configPath: NioPath =
    val fileName = "map-editor-wrap.conf"
    sys.props
      .get("dom6.configPath")
      .map(p => PathUtils.normalizeForWSL(NioPath.of(p)))
      .orElse {
        val cwd = NioPath.of(fileName)
        if (JFiles.exists(cwd)) Some(cwd) else None
      }
      .orElse {
        // When running via sbt subproject, working dir is often ./apps; check parent
        val parent = NioPath.of("..", fileName).normalize()
        if (JFiles.exists(parent)) Some(parent) else None
      }
      .getOrElse(NioPath.of(fileName))
  private val sampleConfig =
    """source="/path/to/mapnuke/output"
dest="/path/to/dominions/maps"
"""

  private val currentVersion = Version("1.3")

  def runWith(
      loader: MapLayerLoader[IO],
      chooser: WrapChoiceService[IO],
      nationChooser: GroundSurfaceNationService[IO],
      dueler: GroundSurfaceDuelPipe[IO],
      throneService: ThronePlacementService[IO]
  ): IO[ExitCode] =
    val finder = new LatestEditorFinderImpl[IO]
    val copier = new MapEditorCopierImpl[IO]
    val writer = new MapWriterImpl[IO]
    val converter = new WrapConversionServiceImpl[IO]
    val checker = new GithubReleaseCheckerImpl[IO]
    val workflow =
      new MapWrapWorkflowImpl(
        finder,
        copier,
        writer,
        loader,
        converter,
        checker,
        chooser,
        nationChooser,
        dueler,
        throneService,
        currentVersion
      )
    val action =
      for
        exists <- IO(JFiles.exists(configPath))
        _ <-
          if exists then IO.unit
          else
            IO(JFiles.writeString(configPath, sampleConfig)) *>
              IO.raiseError(new RuntimeException(s"${configPath.getFileName.toString} created; please edit and rerun"))
        rawCfg <- IO(ConfigSource.file(configPath).loadOrThrow[PathsConfig])
        cfg = PathsConfig(PathUtils.normalizeForWSL(rawCfg.source), PathUtils.normalizeForWSL(rawCfg.dest))
        res <- workflow.run(cfg)
        _ <- IO.fromEither(res)
      yield ExitCode.Success
    action

  override def run(args: List[String]): IO[ExitCode] =
    val loader = new MapLayerLoaderImpl[IO]
    val dueler = new GroundSurfaceDuelPipeImpl[IO](
      new MapSizeValidatorImpl[IO],
      new PlacementPlannerImpl[IO],
      new GateDirectiveServiceImpl[IO],
      new ThronePlacementServiceImpl[IO],
      new SpawnPlacementServiceImpl[IO]
    )
    runWith(
      loader,
      new WrapChoiceServiceImpl[IO],
      new GroundSurfaceNationServiceImpl[IO],
      dueler,
      new ThronePlacementServiceImpl[IO]
    )
