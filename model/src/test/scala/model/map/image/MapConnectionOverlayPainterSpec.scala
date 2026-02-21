package com.crib.bills.dom6maps
package model.map.image

import cats.effect.IO
import model.{BorderFlag, ProvinceId, TerrainFlag}
import model.map.{Border, Pb}
import weaver.SimpleIOSuite

object MapConnectionOverlayPainterSpec extends SimpleIOSuite:
  private val overlayPainter = new BorderFlagMapConnectionOverlayPainter()
  private val terrainPainter = new ConstantColorMapTerrainPainter()

  test("paints river color along province border") {
    val ownership = rasterizeTwoProvinceColumns()
    val baseImage = terrainPainter.paint(
      ownership,
      Map(ProvinceId(1) -> TerrainFlag.Plains.mask, ProvinceId(2) -> TerrainFlag.Plains.mask)
    )
    val image = overlayPainter.paint(
      baseImage,
      ownership,
      Vector(Border(ProvinceId(1), ProvinceId(2), BorderFlag.River))
    )

    val riverColor = BorderFlagMapConnectionOverlayPainter.defaultStyle.riverColor
    val riverPixels = countPixels(image, riverColor)

    IO(expect(riverPixels > 0))
  }

  test("paints road color between province centers") {
    val ownership = rasterizeTwoProvinceColumns()
    val baseImage = terrainPainter.paint(
      ownership,
      Map(ProvinceId(1) -> TerrainFlag.Plains.mask, ProvinceId(2) -> TerrainFlag.Plains.mask)
    )
    val image = overlayPainter.paint(
      baseImage,
      ownership,
      Vector(Border(ProvinceId(1), ProvinceId(2), BorderFlag.Road))
    )

    val roadColor = BorderFlagMapConnectionOverlayPainter.defaultStyle.roadColor
    val roadPixels = countPixels(image, roadColor)

    IO(expect(roadPixels > 0))
  }

  test("bridged river paints both river and road colors") {
    val ownership = rasterizeTwoProvinceColumns()
    val baseImage = terrainPainter.paint(
      ownership,
      Map(ProvinceId(1) -> TerrainFlag.Plains.mask, ProvinceId(2) -> TerrainFlag.Plains.mask)
    )
    val image = overlayPainter.paint(
      baseImage,
      ownership,
      Vector(Border(ProvinceId(1), ProvinceId(2), BorderFlag.BridgedRiver))
    )

    val riverColor = BorderFlagMapConnectionOverlayPainter.defaultStyle.riverColor
    val roadColor = BorderFlagMapConnectionOverlayPainter.defaultStyle.roadColor

    IO(expect.all(
      countPixels(image, riverColor) > 0,
      countPixels(image, roadColor) > 0
    ))
  }

  test("mountain and mountain pass both produce visible overlays") {
    val ownership = rasterizeTwoProvinceColumns()
    val baseImage = terrainPainter.paint(
      ownership,
      Map(ProvinceId(1) -> TerrainFlag.Plains.mask, ProvinceId(2) -> TerrainFlag.Plains.mask)
    )
    val image = overlayPainter.paint(
      baseImage,
      ownership,
      Vector(
        Border(ProvinceId(1), ProvinceId(2), BorderFlag.Impassable),
        Border(ProvinceId(1), ProvinceId(2), BorderFlag.MountainPass)
      )
    )

    val mountainColor = BorderFlagMapConnectionOverlayPainter.defaultStyle.mountainColor
    val mountainPassColor = BorderFlagMapConnectionOverlayPainter.defaultStyle.mountainPassColor

    IO(expect.all(
      countPixels(image, mountainColor) > 0,
      countPixels(image, mountainPassColor) > 0
    ))
  }

  private def rasterizeTwoProvinceColumns(): ProvincePixelRasterizer.ProvincePixelOwnership =
    ProvincePixelRasterizer
      .rasterize(
        40,
        30,
        (0 until 30).flatMap { yPixel =>
          Vector(
            Pb(0, yPixel, 20, ProvinceId(1)),
            Pb(20, yPixel, 20, ProvinceId(2))
          )
        }.toVector
      )
      .toOption
      .get

  private def countPixels(
      image: MapImagePainter.MapImage,
      color: MapImagePainter.RedGreenBlueColor
  ): Int =
    image.redGreenBlueBytes
      .grouped(3)
      .count { bytes =>
        (bytes(0) & 0xff) == color.red &&
        (bytes(1) & 0xff) == color.green &&
        (bytes(2) & 0xff) == color.blue
      }
