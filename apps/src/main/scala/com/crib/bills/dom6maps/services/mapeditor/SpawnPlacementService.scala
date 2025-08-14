package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.Applicative
import cats.syntax.all.*
import cats.effect.Sync
import model.map.{AllowedPlayer, MapState, PlayerSpawn, SpecStart}

trait SpawnPlacementService[Sequencer[_]]:
  def update(state: MapState, spawns: Vector[PlayerSpawn]): Sequencer[MapState]

class SpawnPlacementServiceImpl[Sequencer[_]: Sync] extends SpawnPlacementService[Sequencer]:
  protected val sequencer = summon[Sync[Sequencer]]

  override def update(state: MapState, spawns: Vector[PlayerSpawn]): Sequencer[MapState] =
    val updated = state
      .copy(
        allowedPlayers = spawns.map(s => AllowedPlayer(s.nation)),
        startingPositions = spawns.map(s => SpecStart(s.nation, s.province))
      )
    sequencer.delay(println(s"Placing ${spawns.size} spawns")).as(updated)

class SpawnPlacementServiceStub[Sequencer[_]: Applicative] extends SpawnPlacementService[Sequencer]:
  override def update(state: MapState, spawns: Vector[PlayerSpawn]): Sequencer[MapState] =
    state.pure[Sequencer]
