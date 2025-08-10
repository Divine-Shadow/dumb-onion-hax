package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.Applicative
import fs2.{Pipe, Stream}
import model.map.{Gate, MapDirective}
import model.map.GateSpec

trait GateDirectiveService[Sequencer[_]]:
  def pipe(gates: Vector[GateSpec]): Pipe[Sequencer, MapDirective, MapDirective]

class GateDirectiveServiceImpl[Sequencer[_]] extends GateDirectiveService[Sequencer]:
  override def pipe(gates: Vector[GateSpec]): Pipe[Sequencer, MapDirective, MapDirective] =
    in =>
      val cleaned = in.filter {
        case _: Gate => false
        case _       => true
      }
      val additions = Stream.emits(gates.map(gs => Gate(gs.a, gs.b))).covary[Sequencer]
      cleaned ++ additions

class GateDirectiveServiceStub[Sequencer[_]: Applicative] extends GateDirectiveService[Sequencer]:
  override def pipe(gates: Vector[GateSpec]): Pipe[Sequencer, MapDirective, MapDirective] =
    stream => stream
