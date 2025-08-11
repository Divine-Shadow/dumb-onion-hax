package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.Applicative
import cats.syntax.all.*
import model.map.{AllowedPlayer, MapState, PlayerSpawn, SpecStart}

trait SpawnPlacementService[Sequencer[_]]:
  def update(state: MapState, spawns: Vector[PlayerSpawn]): Sequencer[MapState]

class SpawnPlacementServiceImpl[Sequencer[_]: Applicative] extends SpawnPlacementService[Sequencer]:
  override def update(state: MapState, spawns: Vector[PlayerSpawn]): Sequencer[MapState] =
    state
      .copy(
        allowedPlayers = spawns.map(s => AllowedPlayer(s.nation)),
        startingPositions = spawns.map(s => SpecStart(s.nation, s.province))
      )
      .pure[Sequencer]

class SpawnPlacementServiceStub[Sequencer[_]: Applicative] extends SpawnPlacementService[Sequencer]:
  override def update(state: MapState, spawns: Vector[PlayerSpawn]): Sequencer[MapState] =
    state.pure[Sequencer]
