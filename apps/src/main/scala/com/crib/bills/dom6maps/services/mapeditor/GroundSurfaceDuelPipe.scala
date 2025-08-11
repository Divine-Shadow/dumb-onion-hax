package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse, Applicative}
import cats.syntax.all.*
import fs2.Stream
import model.map.*

trait GroundSurfaceDuelPipe[Sequencer[_]]:
  def apply[ErrorChannel[_]](
      surface: Stream[Sequencer, MapDirective],
      cave: Stream[Sequencer, MapDirective],
      config: GroundSurfaceDuelConfig,
      surfaceNation: SurfaceNation,
      undergroundNation: UndergroundNation
    )(using MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[(Vector[MapDirective], Vector[MapDirective])]]

class GroundSurfaceDuelPipeImpl[Sequencer[_]: cats.effect.Sync](
    sizeValidator: MapSizeValidator[Sequencer],
    planner: PlacementPlanner[Sequencer],
    gateService: GateDirectiveService[Sequencer],
    throneService: ThronePlacementService[Sequencer],
    spawnService: SpawnPlacementService[Sequencer]
) extends GroundSurfaceDuelPipe[Sequencer]:
  override def apply[ErrorChannel[_]](
      surface: Stream[Sequencer, MapDirective],
      cave: Stream[Sequencer, MapDirective],
      config: GroundSurfaceDuelConfig,
      surfaceNation: SurfaceNation,
      undergroundNation: UndergroundNation
    )(using errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[(Vector[MapDirective], Vector[MapDirective])]] =
    for
      validated <- sizeValidator.validate[ErrorChannel](surface, cave)
      result <- validated.traverse { case (size, surfaceDs, caveDs) =>
        val (gates, thrones) = planner.plan(size, config)
        val center = CenterProvince.of(size)
        val width = MapWidth(size.value)
        val height = MapHeight(size.value)
        def transform(ds: Vector[MapDirective], nation: model.Nation) =
          val spawn = PlayerSpawn(nation, center)
          Stream
            .emits(ds)
            .covary[Sequencer]
            .through(gateService.pipe(gates))
            .through(throneService.pipe(thrones))
            .through(spawnService.pipe(Vector(spawn)))
            .through(WrapSeverService.verticalPipe(width, height))
            .through(WrapSeverService.horizontalPipe(width, height))
            .compile
            .toVector
        (
          transform(surfaceDs, surfaceNation.value),
          transform(caveDs, undergroundNation.value)
        ).tupled
      }
    yield result

class GroundSurfaceDuelPipeStub[Sequencer[_]: Applicative] extends GroundSurfaceDuelPipe[Sequencer]:
  override def apply[ErrorChannel[_]](
      surface: Stream[Sequencer, MapDirective],
      cave: Stream[Sequencer, MapDirective],
      config: GroundSurfaceDuelConfig,
      surfaceNation: SurfaceNation,
      undergroundNation: UndergroundNation
    )(using MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[(Vector[MapDirective], Vector[MapDirective])]] =
    (Vector.empty[MapDirective], Vector.empty[MapDirective]).pure[ErrorChannel].pure[Sequencer]
