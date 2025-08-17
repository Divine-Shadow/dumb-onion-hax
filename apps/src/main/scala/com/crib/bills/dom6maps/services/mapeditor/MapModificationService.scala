package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{Applicative, MonadError, Traverse}
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import cats.effect.Async
import model.map.{GateSpec, MapDirective, MapState, ThronePlacement}

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
        transformedSurface <- surfaceEC.traverse { case (state, passThrough) =>
          for
            gated   <- gateService.update(state, gates)
            throned <- throneService.update(gated, thrones)
          yield (throned, passThrough)
        }
        transformedCave <- caveEC.traverse { case (state, passThrough) =>
          gateService.update(state, gates).map((_, passThrough))
        }
        _ <- transformedSurface.flatTraverse { case (st, residual) =>
          writer.write[ErrorChannel](st, residual, surfaceOut)(using files, errorChannel)
        }
        _ <- transformedCave.flatTraverse { case (st, residual) =>
          writer.write[ErrorChannel](st, residual, caveOut)(using files, errorChannel)
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
