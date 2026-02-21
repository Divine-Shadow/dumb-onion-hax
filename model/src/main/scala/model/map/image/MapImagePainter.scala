package com.crib.bills.dom6maps
package model.map.image

import model.{ProvinceId, TerrainFlag, TerrainMask}

object MapImagePainter:
  final case class RedGreenBlueColor(red: Int, green: Int, blue: Int)

  final case class MapImage(
      widthPixels: Int,
      heightPixels: Int,
      redGreenBlueBytes: Array[Byte]
  )

  final case class PainterPalette(
      landColor: RedGreenBlueColor,
      seaColor: RedGreenBlueColor,
      borderColor: RedGreenBlueColor,
      provinceAnchorColor: RedGreenBlueColor
  )

  val defaultPalette: PainterPalette =
    PainterPalette(
      landColor = RedGreenBlueColor(113, 142, 86),
      seaColor = RedGreenBlueColor(54, 90, 120),
      borderColor = RedGreenBlueColor(25, 22, 18),
      provinceAnchorColor = RedGreenBlueColor(255, 255, 255)
    )

  def paint(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      terrainMaskByProvince: Map[ProvinceId, Long],
      palette: PainterPalette = defaultPalette
  ): MapImage =
    val bytes = Array.ofDim[Byte](ownership.widthPixels * ownership.heightPixels * 3)

    fillBackground(bytes, palette.seaColor)
    paintProvinceAreas(ownership, bytes, terrainMaskByProvince, palette)
    paintProvinceBorders(ownership, bytes, palette.borderColor)
    paintProvinceAnchorPixels(ownership, bytes, palette.provinceAnchorColor)

    MapImage(ownership.widthPixels, ownership.heightPixels, bytes)

  private def fillBackground(bytes: Array[Byte], color: RedGreenBlueColor): Unit =
    var index = 0
    while index < bytes.length do
      bytes(index) = color.red.toByte
      bytes(index + 1) = color.green.toByte
      bytes(index + 2) = color.blue.toByte
      index += 3

  private def paintProvinceAreas(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      bytes: Array[Byte],
      terrainMaskByProvince: Map[ProvinceId, Long],
      palette: PainterPalette
  ): Unit =
    var pixelIndex = 0
    while pixelIndex < ownership.provinceIdentifierByPixel.length do
      val provinceIdentifier = ownership.provinceIdentifierByPixel(pixelIndex)
      if provinceIdentifier > 0 then
        val color =
          if isSeaProvince(ProvinceId(provinceIdentifier), terrainMaskByProvince) then palette.seaColor
          else palette.landColor
        paintPixel(bytes, pixelIndex, color)
      pixelIndex += 1

  private def isSeaProvince(
      provinceId: ProvinceId,
      terrainMaskByProvince: Map[ProvinceId, Long]
  ): Boolean =
    terrainMaskByProvince
      .get(provinceId)
      .exists { maskValue =>
        val mask = TerrainMask(maskValue)
        mask.hasFlag(TerrainFlag.Sea) || mask.hasFlag(TerrainFlag.DeepSea)
      }

  private def paintProvinceBorders(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      bytes: Array[Byte],
      borderColor: RedGreenBlueColor
  ): Unit =
    var y = 0
    while y < ownership.heightPixels do
      var x = 0
      while x < ownership.widthPixels do
        val provinceIdentifier = ownership.provinceIdentifierAt(x, y)
        if provinceIdentifier > 0 && touchesDifferentProvince(ownership, x, y, provinceIdentifier) then
          paintPixel(bytes, y * ownership.widthPixels + x, borderColor)
        x += 1
      y += 1

  private def touchesDifferentProvince(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      x: Int,
      y: Int,
      provinceIdentifier: Int
  ): Boolean =
    hasDifferentNeighbour(ownership, x - 1, y, provinceIdentifier) ||
    hasDifferentNeighbour(ownership, x + 1, y, provinceIdentifier) ||
    hasDifferentNeighbour(ownership, x, y - 1, provinceIdentifier) ||
    hasDifferentNeighbour(ownership, x, y + 1, provinceIdentifier)

  private def hasDifferentNeighbour(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      x: Int,
      y: Int,
      provinceIdentifier: Int
  ): Boolean =
    if x < 0 || x >= ownership.widthPixels || y < 0 || y >= ownership.heightPixels then false
    else
      val neighbour = ownership.provinceIdentifierAt(x, y)
      neighbour > 0 && neighbour != provinceIdentifier

  private def paintProvinceAnchorPixels(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      bytes: Array[Byte],
      anchorColor: RedGreenBlueColor
  ): Unit =
    val anchorByProvince = ProvinceAnchorLocator.locateAnchorPixelByProvince(ownership)
    anchorByProvince.values.foreach { pixelIndex =>
      paintPixel(bytes, pixelIndex, anchorColor)
    }

  private def paintPixel(
      bytes: Array[Byte],
      pixelIndex: Int,
      color: RedGreenBlueColor
  ): Unit =
    val byteIndex = pixelIndex * 3
    bytes(byteIndex) = color.red.toByte
    bytes(byteIndex + 1) = color.green.toByte
    bytes(byteIndex + 2) = color.blue.toByte
