package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{Applicative, MonadError, Traverse}
import cats.syntax.all.*
import cats.effect.Sync
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

class GroundSurfaceDuelPipeImpl[Sequencer[_]: Sync](
    sizeValidator: MapSizeValidator[Sequencer],
    planner: PlacementPlanner[Sequencer],
    gateService: GateDirectiveService[Sequencer],
    throneService: ThronePlacementService[Sequencer],
    spawnService: SpawnPlacementService[Sequencer]
) extends GroundSurfaceDuelPipe[Sequencer]:
  protected val sequencer = summon[Sync[Sequencer]]

  override def apply[ErrorChannel[_]](
      surface: MapState,
      cave: MapState,
      config: GroundSurfaceDuelConfig,
      surfaceNation: SurfaceNation,
      undergroundNation: UndergroundNation
    )(using errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[(MapState, MapState)]] =
    for
      _ <- sequencer.delay(println("Running GroundSurfaceDuelPipe"))
      validated <- sizeValidator.validate[ErrorChannel](surface, cave)
      result <- validated.flatTraverse { case (size, surfaceState, caveState) =>
        val (gates, thrones) = planner.plan(size, config)
        val center = CenterProvince.of(size)
        val spawnSurface = PlayerSpawn(surfaceNation.value, center)
        val spawnCave = PlayerSpawn(undergroundNation.value, center)
        def transform(state: MapState, spawn: PlayerSpawn) =
          for
            gated   <- gateService.update(state, gates)
            thronedEC <- throneService.update[ErrorChannel](gated, thrones)
            spawnedEC <- thronedEC.traverse(throned => spawnService.update(throned, Vector(spawn)))
          yield spawnedEC.map(s => WrapSeverService.severHorizontally(WrapSeverService.severVertically(s)))
        for
          surfEC <- transform(surfaceState, spawnSurface)
          caveEC <- transform(caveState, spawnCave)
        yield (surfEC, caveEC).tupled
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
