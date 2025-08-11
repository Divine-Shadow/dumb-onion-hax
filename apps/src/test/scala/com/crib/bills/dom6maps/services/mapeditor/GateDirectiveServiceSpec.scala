package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.syntax.all.*
import weaver.SimpleIOSuite
import model.ProvinceId
import model.map.{Gate, GateSpec, MapState}

object GateDirectiveServiceSpec extends SimpleIOSuite:
  test("update replaces gates and appends new ones") {
    val service = new GateDirectiveServiceImpl[IO]
    val state = MapState.empty.copy(gates = Vector(
      Gate(ProvinceId(1), ProvinceId(2)),
      Gate(ProvinceId(3), ProvinceId(4))
    ))
    val gates = Vector(GateSpec(ProvinceId(5), ProvinceId(6)))
    for
      res <- service.update(state, gates)
    yield expect.all(
      !res.gates.contains(Gate(ProvinceId(1), ProvinceId(2))),
      res.gates.contains(Gate(ProvinceId(5), ProvinceId(6)))
    )
  }
