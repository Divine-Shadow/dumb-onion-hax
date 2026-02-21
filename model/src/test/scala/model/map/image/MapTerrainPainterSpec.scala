package com.crib.bills.dom6maps
package model.map.image

import cats.effect.IO
import model.{ProvinceId, TerrainFlag}
import model.map.Pb
import weaver.SimpleIOSuite

object MapTerrainPainterSpec extends SimpleIOSuite:
  test("constant color painter matches MapImagePainter default output") {
    val ownership = ProvincePixelRasterizer
      .rasterize(
        3,
        3,
        Vector(
          Pb(0, 0, 3, ProvinceId(1)),
          Pb(0, 1, 3, ProvinceId(1)),
          Pb(0, 2, 3, ProvinceId(1))
        )
      )
      .toOption
      .get

    val terrainMaskByProvince = Map(ProvinceId(1) -> TerrainFlag.Plains.mask)
    val baselineImage = MapImagePainter.paint(ownership, terrainMaskByProvince)
    val constantPainter = new ConstantColorMapTerrainPainter()
    val image = constantPainter.paint(ownership, terrainMaskByProvince)

    IO(expect.same(baselineImage.redGreenBlueBytes.toVector, image.redGreenBlueBytes.toVector))
  }

  test("primary terrain painter uses terrain-specific colors") {
    val ownership = ProvincePixelRasterizer
      .rasterize(
        15,
        5,
        (0 until 5).flatMap { yValue =>
          Vector(
            Pb(0, yValue, 5, ProvinceId(1)),
            Pb(5, yValue, 5, ProvinceId(2)),
            Pb(10, yValue, 5, ProvinceId(3))
          )
        }.toVector
      )
      .toOption
      .get

    val terrainMaskByProvince = Map(
      ProvinceId(1) -> TerrainFlag.Forest.mask,
      ProvinceId(2) -> TerrainFlag.Waste.mask,
      ProvinceId(3) -> TerrainFlag.Sea.mask
    )

    val painter = new PrimaryTerrainColorMapTerrainPainter()
    val image = painter.paint(ownership, terrainMaskByProvince)

    def pixelAt(xPixel: Int, yPixel: Int): (Int, Int, Int) =
      val pixelIndex = yPixel * image.widthPixels + xPixel
      val byteIndex = pixelIndex * 3
      (
        image.redGreenBlueBytes(byteIndex) & 0xff,
        image.redGreenBlueBytes(byteIndex + 1) & 0xff,
        image.redGreenBlueBytes(byteIndex + 2) & 0xff
      )

    val forestColor = PrimaryTerrainColorMapTerrainPainter.defaultPalette.colorByPrimaryTerrainType(
      PrimaryTerrainColorMapTerrainPainter.PrimaryTerrainType.Forest
    )
    val wasteColor = PrimaryTerrainColorMapTerrainPainter.defaultPalette.colorByPrimaryTerrainType(
      PrimaryTerrainColorMapTerrainPainter.PrimaryTerrainType.Waste
    )
    val seaColor = PrimaryTerrainColorMapTerrainPainter.defaultPalette.colorByPrimaryTerrainType(
      PrimaryTerrainColorMapTerrainPainter.PrimaryTerrainType.Sea
    )

    IO(expect.all(
      pixelAt(1, 1) == (forestColor.red, forestColor.green, forestColor.blue),
      pixelAt(6, 1) == (wasteColor.red, wasteColor.green, wasteColor.blue),
      pixelAt(11, 1) == (seaColor.red, seaColor.green, seaColor.blue)
    ))
  }
