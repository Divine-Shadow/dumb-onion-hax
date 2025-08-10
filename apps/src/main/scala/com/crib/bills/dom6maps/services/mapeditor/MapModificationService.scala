package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse, Applicative}
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}
import cats.effect.Async
import model.map.{GateSpec, ThronePlacement}

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
        transformedSurface <- surfaceEC.traverse { ds =>
          Stream
            .emits(ds)
            .covary[Sequencer]
            .through(gateService.pipe(gates))
            .through(throneService.pipe(thrones))
            .compile
            .toVector
        }
        transformedCave <- caveEC.traverse { ds =>
          Stream
            .emits(ds)
            .covary[Sequencer]
            .through(gateService.pipe(gates))
            .compile
            .toVector
        }
        _ <- transformedSurface.flatTraverse(ds => writer.write[ErrorChannel](ds, surfaceOut)(using files, errorChannel))
        _ <- transformedCave.flatTraverse(ds => writer.write[ErrorChannel](ds, caveOut)(using files, errorChannel))
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
