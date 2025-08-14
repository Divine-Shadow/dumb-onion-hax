package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.Applicative
import cats.syntax.all.*
import cats.effect.Sync
import model.map.{Gate, GateSpec, MapState}

trait GateDirectiveService[Sequencer[_]]:
  def update(state: MapState, gates: Vector[GateSpec]): Sequencer[MapState]

class GateDirectiveServiceImpl[Sequencer[_]: Sync] extends GateDirectiveService[Sequencer]:
  protected val sequencer = summon[Sync[Sequencer]]

  override def update(state: MapState, gates: Vector[GateSpec]): Sequencer[MapState] =
    val updated = state.copy(gates = gates.map(gs => Gate(gs.a, gs.b)))
    sequencer.delay(println(s"Placing ${gates.size} gates")).as(updated)

class GateDirectiveServiceStub[Sequencer[_]: Applicative] extends GateDirectiveService[Sequencer]:
  override def update(state: MapState, gates: Vector[GateSpec]): Sequencer[MapState] =
    state.pure[Sequencer]
