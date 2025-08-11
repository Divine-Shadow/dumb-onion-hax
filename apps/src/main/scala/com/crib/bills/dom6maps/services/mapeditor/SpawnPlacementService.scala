package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.Applicative
import fs2.{Pipe, Stream}
import model.map.{MapDirective, PlayerSpawn, AllowedPlayer, SpecStart}

trait SpawnPlacementService[Sequencer[_]]:
  def pipe(spawns: Vector[PlayerSpawn]): Pipe[Sequencer, MapDirective, MapDirective]

class SpawnPlacementServiceImpl[Sequencer[_]] extends SpawnPlacementService[Sequencer]:
  override def pipe(spawns: Vector[PlayerSpawn]): Pipe[Sequencer, MapDirective, MapDirective] =
    in =>
      val cleaned = in.filter {
        case _: AllowedPlayer => false
        case _: SpecStart     => false
        case _                => true
      }
      val additions = Stream
        .emits(spawns.flatMap(s => Vector(AllowedPlayer(s.nation), SpecStart(s.nation, s.province))))
        .covary[Sequencer]
      cleaned ++ additions

class SpawnPlacementServiceStub[Sequencer[_]: Applicative] extends SpawnPlacementService[Sequencer]:
  override def pipe(spawns: Vector[PlayerSpawn]): Pipe[Sequencer, MapDirective, MapDirective] =
    stream => stream
