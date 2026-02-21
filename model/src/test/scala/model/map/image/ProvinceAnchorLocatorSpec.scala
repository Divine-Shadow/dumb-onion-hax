package com.crib.bills.dom6maps
package model.map.image

import cats.effect.IO
import model.ProvinceId
import model.map.Pb
import weaver.SimpleIOSuite

object ProvinceAnchorLocatorSpec extends SimpleIOSuite:
  test("locates one anchor pixel per province") {
    val ownership = ProvincePixelRasterizer
      .rasterize(
        4,
        2,
        Vector(
          Pb(0, 0, 2, ProvinceId(1)),
          Pb(2, 0, 2, ProvinceId(2)),
          Pb(0, 1, 4, ProvinceId(3))
        )
      )
      .toOption
      .get

    val anchors = ProvinceAnchorLocator.locateAnchorPixelByProvince(ownership)

    IO(expect.all(
      anchors.keySet == Set(1, 2, 3),
      anchors.values.toSet.size == 3
    ))
  }
