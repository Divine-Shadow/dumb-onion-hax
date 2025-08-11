package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{Applicative, MonadError, Traverse}
import cats.syntax.all.*
import model.map.*

trait GroundSurfaceDuelPipe[Sequencer[_]]:
  def apply[ErrorChannel[_]](
      surface: MapState,
      cave: MapState,
      config: GroundSurfaceDuelConfig,
      surfaceNation: SurfaceNation,
      undergroundNation: UndergroundNation
    )(using MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[(MapState, MapState)]]

class GroundSurfaceDuelPipeImpl[Sequencer[_]: cats.effect.Sync](
    sizeValidator: MapSizeValidator[Sequencer],
    planner: PlacementPlanner[Sequencer],
    gateService: GateDirectiveService[Sequencer],
    throneService: ThronePlacementService[Sequencer],
    spawnService: SpawnPlacementService[Sequencer]
) extends GroundSurfaceDuelPipe[Sequencer]:
  override def apply[ErrorChannel[_]](
      surface: MapState,
      cave: MapState,
      config: GroundSurfaceDuelConfig,
      surfaceNation: SurfaceNation,
      undergroundNation: UndergroundNation
    )(using errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[(MapState, MapState)]] =
    for
      validated <- sizeValidator.validate[ErrorChannel](surface, cave)
      result <- validated.traverse { case (size, surfaceState, caveState) =>
        val (gates, thrones) = planner.plan(size, config)
        val center = CenterProvince.of(size)
        val spawnSurface = PlayerSpawn(surfaceNation.value, center)
        val spawnCave = PlayerSpawn(undergroundNation.value, center)
        def transform(state: MapState, spawn: PlayerSpawn) =
          for
            gated   <- gateService.update(state, gates)
            throned <- throneService.update(gated, thrones)
            spawned <- spawnService.update(throned, Vector(spawn))
          yield WrapSeverService.severHorizontally(WrapSeverService.severVertically(spawned))
        (transform(surfaceState, spawnSurface), transform(caveState, spawnCave)).tupled
      }
    yield result

class GroundSurfaceDuelPipeStub[Sequencer[_]: Applicative] extends GroundSurfaceDuelPipe[Sequencer]:
  override def apply[ErrorChannel[_]](
      surface: MapState,
      cave: MapState,
      config: GroundSurfaceDuelConfig,
      surfaceNation: SurfaceNation,
      undergroundNation: UndergroundNation
    )(using MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[(MapState, MapState)]] =
    (MapState.empty, MapState.empty).pure[ErrorChannel].pure[Sequencer]
