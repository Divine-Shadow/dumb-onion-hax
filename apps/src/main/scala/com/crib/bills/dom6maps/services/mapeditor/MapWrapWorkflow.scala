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
import java.nio.file.{Files as JFiles, Path as NioPath}

trait MapWrapWorkflow:
  def run(cfg: PathsConfig): IO[Unit]

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
    throneView: ThroneFeatureView[IO],
    throneService: ThronePlacementService[IO],
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
      layerEC <- loader.load[ErrorOr](bytes)
      layer <- IO.fromEither(layerEC)
      overridesEC <- IO {
        val path = NioPath.of("throne-override.conf")
        if JFiles.exists(path) then
          ConfigSource
            .file(path)
            .load[ThroneConfiguration]
            .leftMap(f => RuntimeException(f.toString))
        else Right(ThroneConfiguration(Vector.empty))
      }
      overrides <- IO.fromEither(overridesEC)
      throneCfgEC <- throneView.chooseConfig[ErrorOr](
        ThroneFeatureConfig(Vector.empty, Vector.empty, overrides.overrides)
      )
      throneCfg <- IO.fromEither(throneCfgEC)
      updatedState <- throneService.update(layer.state, throneCfg.placements)
      baseLayer = layer.copy(state = updatedState)
      wrapEC <- chooser.chooseWraps[ErrorOr]()
      wrapChoices <- IO.fromEither(wrapEC)
      _ <- wrapChoices.main match
        case WrapChoice.GroundSurfaceDuel =>
          copied.cave match
            case Some((caveBytes, caveOutPath)) =>
              for
                caveLayerEC <- loader.load[ErrorOr](caveBytes)
                caveLayer <- IO.fromEither(caveLayerEC)
                nationsEC <- nationChooser.chooseNations[ErrorOr]()
                nations <- IO.fromEither(nationsEC)
                duelResEC <- dueler
                  .apply[ErrorOr](
                    baseLayer.state,
                    caveLayer.state,
                    GroundSurfaceDuelConfig.default,
                    nations.surface,
                    nations.underground
                  )
                duelRes <- IO.fromEither(duelResEC)
                (surfRes, caveRes) = duelRes
                surfWritten <- writer.write[ErrorOr](baseLayer.copy(state = surfRes), outPath)
                _ <- IO.fromEither(surfWritten)
                caveWritten <- writer.write[ErrorOr](caveLayer.copy(state = caveRes), caveOutPath)
                _ <- IO.fromEither(caveWritten)
              yield ()
            case None => IO.raiseError(new NoSuchElementException("cave map not found"))
        case wrapChoice =>
          for
            convertedEC <- converter.convert[ErrorOr](baseLayer.state, wrapChoice)
            converted <- IO.fromEither(convertedEC)
            written <- writer.write[ErrorOr](baseLayer.copy(state = converted), outPath)
            _ <- IO.fromEither(written)
            _ <- wrapChoices.cave match
              case Some(caveChoice) =>
                copied.cave match
                  case Some((caveBytes, caveOutPath)) =>
                    for
                      caveLayerEC <- loader.load[ErrorOr](caveBytes)
                      caveLayer <- IO.fromEither(caveLayerEC)
                      caveConvertedEC <- converter.convert[ErrorOr](caveLayer.state, caveChoice)
                      caveConverted <- IO.fromEither(caveConvertedEC)
                      caveWritten <- writer.write[ErrorOr](caveLayer.copy(state = caveConverted), caveOutPath)
                      _ <- IO.fromEither(caveWritten)
                    yield ()
                  case None => IO.raiseError(new NoSuchElementException("cave map not found"))
              case None =>
                copied.cave match
                  case Some((caveBytes, caveOutPath)) =>
                    for
                      caveLayerEC <- loader.load[ErrorOr](caveBytes)
                      caveLayer <- IO.fromEither(caveLayerEC)
                      caveWritten <- writer.write[ErrorOr](caveLayer, caveOutPath)
                      _ <- IO.fromEither(caveWritten)
                    yield ()
                  case None => IO.unit
          yield ()
    yield ()
