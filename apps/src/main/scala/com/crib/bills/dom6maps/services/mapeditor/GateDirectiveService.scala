package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.Applicative
import cats.syntax.all.*
import model.map.{Gate, GateSpec, MapState}

trait GateDirectiveService[Sequencer[_]]:
  def update(state: MapState, gates: Vector[GateSpec]): Sequencer[MapState]

class GateDirectiveServiceImpl[Sequencer[_]: Applicative] extends GateDirectiveService[Sequencer]:
  override def update(state: MapState, gates: Vector[GateSpec]): Sequencer[MapState] =
    state.copy(gates = gates.map(gs => Gate(gs.a, gs.b))).pure[Sequencer]

class GateDirectiveServiceStub[Sequencer[_]: Applicative] extends GateDirectiveService[Sequencer]:
  override def update(state: MapState, gates: Vector[GateSpec]): Sequencer[MapState] =
    state.pure[Sequencer]
