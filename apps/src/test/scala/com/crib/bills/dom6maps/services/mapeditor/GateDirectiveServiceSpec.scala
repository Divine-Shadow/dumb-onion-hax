package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import fs2.Stream
import weaver.SimpleIOSuite
import model.ProvinceId
import model.map.{Gate, MapDirective}
import model.map.GateSpec

object GateDirectiveServiceSpec extends SimpleIOSuite:
  test("pipe replaces gates and appends new ones") {
    val service = new GateDirectiveServiceImpl[IO]
    val input = Stream.emits[IO, MapDirective](Vector(
      Gate(ProvinceId(1), ProvinceId(2)),
      Gate(ProvinceId(3), ProvinceId(4))
    ))
    val gates = Vector(GateSpec(ProvinceId(5), ProvinceId(6)))
    for
      res <- input.through(service.pipe(gates)).compile.toVector
      hasOld = res.exists { case Gate(ProvinceId(1), ProvinceId(2)) => true; case _ => false }
      hasNew = res.contains(Gate(ProvinceId(5), ProvinceId(6)))
    yield expect.all(!hasOld, hasNew)
  }
