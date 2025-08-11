package com.crib.bills.dom6maps
package model.map

import cats.effect.IO
import weaver.SimpleIOSuite
import model.ProvinceId

object GeometryHelpersSpec extends SimpleIOSuite:
  private val size = MapSize.from(5).toOption.get

  test("EdgeMidpoints.of returns ProvinceIds at expected midpoints") {
    val midpoints = EdgeMidpoints.of(size)
    val expected = EdgeMidpoints(
      top = ProvinceId(3),
      bottom = ProvinceId(23),
      left = ProvinceId(11),
      right = ProvinceId(15)
    )
    IO.pure(expect(midpoints == expected))
  }

  test("CornerProvinces.of and CornerProvinces.all return the four corners") {
    val corners = CornerProvinces.of(size)
    val expected = CornerProvinces(
      topLeft = ProvinceId(1),
      topRight = ProvinceId(5),
      bottomLeft = ProvinceId(21),
      bottomRight = ProvinceId(25)
    )
    val allExpected = Vector(
      ProvinceId(1),
      ProvinceId(5),
      ProvinceId(21),
      ProvinceId(25)
    )
    IO.pure(expect(corners == expected && CornerProvinces.all(size) == allExpected))
  }

  test("CenterProvince.of returns the central province") {
    val center = CenterProvince.of(size)
    IO.pure(expect(center == ProvinceId(13)))
  }
