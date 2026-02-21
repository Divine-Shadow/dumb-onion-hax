package com.crib.bills.dom6maps
package model.map.image

import model.{ProvinceId, TerrainFlag}
import model.map.Pb
import weaver.SimpleIOSuite
import cats.effect.IO

object MapImagePainterSpec extends SimpleIOSuite:
  test("paints province anchors white and sea provinces as sea color") {
    val ownership = ProvincePixelRasterizer
      .rasterize(
        3,
        1,
        Vector(
          Pb(0, 0, 2, ProvinceId(1)),
          Pb(2, 0, 1, ProvinceId(2))
        )
      )
      .toOption
      .get

    val terrainMaskByProvince = Map(
      ProvinceId(1) -> TerrainFlag.Plains.mask,
      ProvinceId(2) -> TerrainFlag.Sea.mask
    )

    val image = MapImagePainter.paint(ownership, terrainMaskByProvince)
    val bytes = image.redGreenBlueBytes

    def pixelAt(index: Int): (Int, Int, Int) =
      val offset = index * 3
      val red = bytes(offset) & 0xff
      val green = bytes(offset + 1) & 0xff
      val blue = bytes(offset + 2) & 0xff
      (red, green, blue)

    val anchor = MapImagePainter.defaultPalette.provinceAnchorColor
    val sea = MapImagePainter.defaultPalette.seaColor

    IO(expect.all(
      pixelAt(0) == (anchor.red, anchor.green, anchor.blue),
      pixelAt(2) == (anchor.red, anchor.green, anchor.blue),
      pixelAt(1) == (MapImagePainter.defaultPalette.borderColor.red, MapImagePainter.defaultPalette.borderColor.green, MapImagePainter.defaultPalette.borderColor.blue)
    ) and expect(pixelAt(2) != (sea.red, sea.green, sea.blue)))
  }
