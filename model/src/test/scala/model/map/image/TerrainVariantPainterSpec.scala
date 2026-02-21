package com.crib.bills.dom6maps
package model.map.image

import cats.effect.IO
import model.{ProvinceId, TerrainFlag}
import model.map.Pb
import weaver.SimpleIOSuite

object TerrainVariantPainterSpec extends SimpleIOSuite:
  test("highlights matching terrain variant pixels") {
    val ownership = ProvincePixelRasterizer
      .rasterize(
        2,
        1,
        Vector(
          Pb(0, 0, 1, ProvinceId(1)),
          Pb(1, 0, 1, ProvinceId(2))
        )
      )
      .toOption
      .get

    val terrainMaskByProvince = Map(
      ProvinceId(1) -> TerrainFlag.Forest.mask,
      ProvinceId(2) -> TerrainFlag.Waste.mask
    )

    val variantImage = TerrainVariantPainter.paintVariant(
      ownership,
      terrainMaskByProvince,
      TerrainVariantPainter.TerrainVariantKind.Forest,
      winterLook = false
    )

    val firstPixelRed = variantImage.redGreenBlueBytes(0) & 0xff
    val secondPixelRed = variantImage.redGreenBlueBytes(3) & 0xff

    IO(expect(firstPixelRed != secondPixelRed))
  }
