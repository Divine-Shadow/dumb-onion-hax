package com.crib.bills.dom6maps
package services.mapeditor

import cats.effect.IO
import cats.syntax.all.*
import weaver.SimpleIOSuite
import model.ProvinceId
import model.map.*

object PlacementPlannerSpec extends SimpleIOSuite:
  test("planner computes gates and thrones") {
    val planner = new apps.services.mapeditor.PlacementPlannerImpl[IO]
    val size = MapSize.from(5).toOption.get
    val config = GroundSurfaceDuelConfig.default
    val (gates, thrones) = planner.plan(size, config)
    val expectedGates = Vector(
      GateSpec(ProvinceId(3), ProvinceId(28)),
      GateSpec(ProvinceId(23), ProvinceId(48)),
      GateSpec(ProvinceId(11), ProvinceId(36)),
      GateSpec(ProvinceId(15), ProvinceId(40))
    )
    val expectedThrones = Vector(
      ProvinceLocation(XCell(0), YCell(0)),
      ProvinceLocation(XCell(4), YCell(0)),
      ProvinceLocation(XCell(0), YCell(4)),
      ProvinceLocation(XCell(4), YCell(4))
    ).map(loc => ThronePlacement(loc, ThroneLevel(1)))
    expect(gates == expectedGates && thrones == expectedThrones).pure[IO]
  }
