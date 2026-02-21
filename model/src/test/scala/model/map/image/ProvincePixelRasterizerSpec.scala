package com.crib.bills.dom6maps
package model.map.image

import model.ProvinceId
import model.map.Pb
import weaver.SimpleIOSuite
import cats.effect.IO

object ProvincePixelRasterizerSpec extends SimpleIOSuite:
  test("rasterizes #pb runs into province ownership") {
    val runs = Vector(
      Pb(1, 0, 2, ProvinceId(3)),
      Pb(0, 1, 3, ProvinceId(4))
    )

    val ownership = ProvincePixelRasterizer.rasterize(4, 2, runs).toOption.get

    IO(expect.all(
      ownership.provinceIdentifierAt(0, 0) == 4,
      ownership.provinceIdentifierAt(1, 0) == 4,
      ownership.provinceIdentifierAt(2, 0) == 4,
      ownership.provinceIdentifierAt(0, 1) == 0,
      ownership.provinceIdentifierAt(1, 1) == 3,
      ownership.provinceIdentifierAt(2, 1) == 3,
      ownership.provinceIdentifierAt(3, 1) == 0
    ))
  }

  test("clips runs that extend outside map bounds") {
    val runs = Vector(Pb(-2, 0, 5, ProvinceId(9)))
    val ownership = ProvincePixelRasterizer.rasterize(3, 1, runs).toOption.get

    IO(expect.all(
      ownership.provinceIdentifierAt(0, 0) == 9,
      ownership.provinceIdentifierAt(1, 0) == 9,
      ownership.provinceIdentifierAt(2, 0) == 9
    ))
  }
