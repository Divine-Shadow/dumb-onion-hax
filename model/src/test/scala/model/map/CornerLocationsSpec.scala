package com.crib.bills.dom6maps
package model.map

import cats.effect.IO
import weaver.SimpleIOSuite

object CornerLocationsSpec extends SimpleIOSuite:
  test("CornerLocations.all returns corner coordinates") {
    val size = MapSize.from(5).toOption.get
    val corners = CornerLocations.all(size)
    val expected = Vector(
      ProvinceLocation(XCell(0), YCell(0)),
      ProvinceLocation(XCell(4), YCell(0)),
      ProvinceLocation(XCell(0), YCell(4)),
      ProvinceLocation(XCell(4), YCell(4))
    )
    IO.pure(expect(corners == expected))
  }
