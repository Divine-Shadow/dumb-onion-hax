package com.crib.bills.dom6maps
package apps

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*
import model.version.Version
import pureconfig.*
import java.nio.file.{Files as JFiles, Path as NioPath}

import services.mapeditor.*
import apps.util.PathUtils
import services.update.GithubReleaseCheckerImpl

/**
 * CLI entry point that runs the map wrap workflow without any UI dialogs.
 * - Reads `map-editor-wrap.conf` for source/dest directories
 * - Optionally reads `throne-override.conf`
 * - Uses WrapChoiceServiceHeadlessImpl to avoid Swing
 *
 * You can influence wrap choices via system properties:
 *   -Ddom6.wrap.main=hwrap|vwrap|full|none|duel
 *   -Ddom6.wrap.cave=hwrap|vwrap|full|none
 */
object MapEditorWrapCliApp extends IOApp:
  private val configFileName = "map-editor-wrap.conf"
  private def resolveConfigPath(): NioPath =
    sys.props
      .get("dom6.configPath")
      .map(p => NioPath.of(p))
      .orElse {
        val cwd = NioPath.of(configFileName)
        if (JFiles.exists(cwd)) Some(cwd) else None
      }
      .orElse {
        val parent = NioPath.of("..", configFileName).normalize()
        if (JFiles.exists(parent)) Some(parent) else None
      }
      .getOrElse(NioPath.of(configFileName))
  private val sampleConfig =
    """source="./data/live-games"
dest="./data/generated-maps"
"""

  private val currentVersion = Version("1.3")

  def run(args: List[String]): IO[ExitCode] =
    val finder    = new LatestEditorFinderImpl[IO]
    val copier    = new MapEditorCopierImpl[IO]
    val writer    = new MapWriterImpl[IO]
    val loader    = new MapLayerLoaderImpl[IO]
    val converter = new WrapConversionServiceImpl[IO]
    val checker   = new GithubReleaseCheckerImpl[IO]
    val chooser   = new WrapChoiceServiceHeadlessImpl[IO]
    val nation    = new GroundSurfaceNationServiceImpl[IO]
    val dueler = new GroundSurfaceDuelPipeImpl[IO](
      new MapSizeValidatorImpl[IO],
      new PlacementPlannerImpl[IO],
      new GateDirectiveServiceImpl[IO],
      new ThronePlacementServiceImpl[IO],
      new SpawnPlacementServiceImpl[IO]
    )
    val throneSvc = new ThronePlacementServiceImpl[IO]

    val workflow = new MapWrapWorkflowImpl(
      finder,
      copier,
      writer,
      loader,
      converter,
      checker,
      chooser,
      nation,
      dueler,
      throneSvc,
      currentVersion
    )

    val path   = resolveConfigPath()
    val action =
      for
        exists <- IO(JFiles.exists(path))
        _ <-
          if exists then IO.unit
          else IO(JFiles.writeString(NioPath.of(configFileName), sampleConfig)) *>
            IO.raiseError(new RuntimeException(s"$configFileName created; please edit and rerun"))
        rawCfg <- IO(ConfigSource.file(path).loadOrThrow[PathsConfig])
        cfg = PathsConfig(PathUtils.normalizeForWSL(rawCfg.source), PathUtils.normalizeForWSL(rawCfg.dest))
        res <- workflow.run(cfg)
        _   <- IO.fromEither(res)
      yield ExitCode.Success

    action
