package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{Applicative, MonadError, Traverse}
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import cats.effect.Async
import model.map.{GateSpec, MapState, ThronePlacement, MapLayer}

trait MapModificationService[Sequencer[_]]:
  def modify[ErrorChannel[_]](
      surface: Path,
      cave: Path,
      gates: Vector[GateSpec],
      thrones: Vector[ThronePlacement],
      surfaceOut: Path,
      caveOut: Path
    )(using files: Files[Sequencer],
          errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[Unit]]

class MapModificationServiceImpl[Sequencer[_]: Async](
    loader: MapLayerLoader[Sequencer],
    gateService: GateDirectiveService[Sequencer],
    throneService: ThronePlacementService[Sequencer],
    writer: MapWriter[Sequencer]
) extends MapModificationService[Sequencer]:
  override def modify[ErrorChannel[_]](
      surface: Path,
      cave: Path,
      gates: Vector[GateSpec],
      thrones: Vector[ThronePlacement],
      surfaceOut: Path,
      caveOut: Path
    )(using files: Files[Sequencer],
          errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[Unit]] =
        for
          surfaceEC <- loader.load[ErrorChannel](surface)(using files, errorChannel)
          caveEC    <- loader.load[ErrorChannel](cave)(using files, errorChannel)
          transformedSurface <- surfaceEC.flatTraverse { layer =>
            for
              gated     <- gateService.update(layer.state, gates)
              thronedEC <- throneService.update[ErrorChannel](gated, thrones)
            yield thronedEC.map(st => layer.copy(state = st))
          }
          transformedCave <- caveEC.flatTraverse { layer =>
            gateService.update(layer.state, gates).map(st => layer.copy(state = st)).map(_.pure[ErrorChannel])
          }
          _ <- transformedSurface.flatTraverse { layer =>
            writer.write[ErrorChannel](layer, surfaceOut)(using files, errorChannel)
          }
          _ <- transformedCave.flatTraverse { layer =>
            writer.write[ErrorChannel](layer, caveOut)(using files, errorChannel)
          }
        yield ().pure[ErrorChannel]

class MapModificationServiceStub[Sequencer[_]: Applicative] extends MapModificationService[Sequencer]:
  override def modify[ErrorChannel[_]](
      surface: Path,
      cave: Path,
      gates: Vector[GateSpec],
      thrones: Vector[ThronePlacement],
      surfaceOut: Path,
      caveOut: Path
    )(using files: Files[Sequencer],
          errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[Unit]] =
      ().pure[Sequencer].map(_.pure[ErrorChannel])
