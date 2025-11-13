package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.syntax.all.*
import cats.instances.either.*
import fs2.io.file.Path
import model.map.{GroundSurfaceDuelConfig, ThroneFeatureConfig}
import model.version.{UpdateStatus, Version}
import apps.services.update.Service as GithubReleaseChecker
import pureconfig.*
import apps.util.PathUtils
import java.nio.file.{Files as JFiles, Path as NioPath}

trait MapWrapWorkflow:
  def run(cfg: PathsConfig): IO[Either[Throwable, Unit]]

class MapWrapWorkflowImpl(
    finder: LatestEditorFinder[IO],
    copier: MapEditorCopier[IO],
    writer: MapWriter[IO],
    loader: MapLayerLoader[IO],
    converter: WrapConversionService[IO],
    checker: GithubReleaseChecker[IO],
    chooser: WrapChoiceService[IO],
    nationChooser: GroundSurfaceNationService[IO],
    dueler: GroundSurfaceDuelPipe[IO],
    throneService: ThronePlacementService[IO],
    magicSiteFlags: MagicSiteFlagService,
    currentVersion: Version
  ) extends MapWrapWorkflow:
  private type ErrorOr[A] = Either[Throwable, A]

  override def run(cfg: PathsConfig): IO[Either[Throwable, Unit]] =
    val srcRoot = Path.fromNioPath(cfg.source)
    val destRoot = Path.fromNioPath(cfg.dest)
    def loadOverrides: IO[Either[Throwable, ThroneConfiguration]] = IO {
      val ignore = sys.props.get("dom6.ignoreOverrides").contains("true")
      if ignore then Right(ThroneConfiguration(Vector.empty))
      else
        sys.props.get("dom6.overridesPath") match
          case Some(p) =>
            val np = PathUtils.normalizeForWSL(NioPath.of(p))
            if JFiles.exists(np) then
              ConfigSource.file(np).load[ThroneConfiguration].leftMap(f => RuntimeException(f.toString))
            else Right(ThroneConfiguration(Vector.empty))
          case _ =>
            val path = NioPath.of("throne-override.conf")
            if JFiles.exists(path) then ConfigSource.file(path).load[ThroneConfiguration].leftMap(f => RuntimeException(f.toString))
            else Right(ThroneConfiguration(Vector.empty))
    }

    val et: cats.data.EitherT[IO, Throwable, Unit] = {
      import cats.data.EitherT
      for {
        _ <- EitherT.right[Throwable](checker.checkForUpdate[ErrorOr](currentVersion).void)
        latest <- EitherT(finder.mostRecentFolder[ErrorOr](srcRoot))
        targetDir = destRoot / latest.fileName.toString
        copied <- EitherT(copier.copyWithoutMaps[ErrorOr](latest, targetDir))
        caveAvailability = CaveLayerAvailability.fromOption(copied.cave)
        (bytes, outPath) = copied.main
        layer <- EitherT(loader.load[ErrorOr](bytes))
        overrides <- EitherT(loadOverrides)
        settings <- EitherT(
          chooser.chooseSettings[ErrorOr](ThroneFeatureConfig(Vector.empty, Vector.empty, overrides.overrides), caveAvailability)
        )
        updatedState <- EitherT(throneService.update[ErrorOr](layer.state, settings.thrones.placements))
        throneLayer = layer.copy(state = updatedState)
        baseLayer = throneLayer.copy(state = magicSiteFlags.apply(throneLayer.state, settings.magicSites.surface))
        _ <- settings.wraps.main match {
          case WrapChoice.GroundSurfaceDuel =>
            copied.cave match
              case Some((caveBytes, caveOutPath)) =>
                for {
                  caveLayer <- EitherT(loader.load[ErrorOr](caveBytes))
                  caveLayerWithSites = caveLayer.copy(state = magicSiteFlags.apply(caveLayer.state, settings.magicSites.cave))
                  nations <- EitherT(nationChooser.chooseNations[ErrorOr]())
                  duelRes <- EitherT(
                    dueler.apply[ErrorOr](
                      baseLayer.state,
                      caveLayerWithSites.state,
                      GroundSurfaceDuelConfig.default,
                      nations.surface,
                      nations.underground
                    )
                  )
                  (surfRes, caveRes) = duelRes
                  _ <- EitherT(writer.write[ErrorOr](baseLayer.copy(state = surfRes), outPath))
                  _ <- EitherT(writer.write[ErrorOr](caveLayerWithSites.copy(state = caveRes), caveOutPath))
                } yield ()
              case None => EitherT.leftT[IO, Unit](new NoSuchElementException("cave map not found"))
          case wrapChoice =>
            for {
              converted <- EitherT(converter.convert[ErrorOr](baseLayer.state, wrapChoice))
              _ <- EitherT(writer.write[ErrorOr](baseLayer.copy(state = converted), outPath))
              _ <- settings.wraps.cave match {
                case Some(caveChoice) =>
                  copied.cave match
                    case Some((caveBytes, caveOutPath)) =>
                      for {
                        caveLayer <- EitherT(loader.load[ErrorOr](caveBytes))
                        caveLayerWithSites = caveLayer.copy(state = magicSiteFlags.apply(caveLayer.state, settings.magicSites.cave))
                        caveConverted <- EitherT(converter.convert[ErrorOr](caveLayerWithSites.state, caveChoice))
                        _ <- EitherT(writer.write[ErrorOr](caveLayerWithSites.copy(state = caveConverted), caveOutPath))
                      } yield ()
                    case None => EitherT.leftT[IO, Unit](new NoSuchElementException("cave map not found"))
                case None =>
                  copied.cave match
                    case Some((caveBytes, caveOutPath)) =>
                      for {
                        caveLayer <- EitherT(loader.load[ErrorOr](caveBytes))
                        updated = magicSiteFlags.apply(caveLayer.state, settings.magicSites.cave)
                        _ <- EitherT(writer.write[ErrorOr](caveLayer.copy(state = updated), caveOutPath))
                      } yield ()
                    case None => EitherT.rightT[IO, Throwable](())
              }
            } yield ()
        }
      } yield ()
    }

    et.value
