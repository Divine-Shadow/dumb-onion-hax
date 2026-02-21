package com.crib.bills.dom6maps
package model.map.image

import model.{ProvinceId, TerrainFlag, TerrainMask}

object TerrainVariantPainter:
  enum TerrainVariantKind:
    case Forest, Waste, Farm, Swamp, Highland, Plain, Kelp, Water

  def paintVariant(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      terrainMaskByProvince: Map[ProvinceId, Long],
      terrainVariantKind: TerrainVariantKind,
      winterLook: Boolean
  ): MapImagePainter.MapImage =
    val defaultPalette =
      if winterLook then
        MapImagePainter.PainterPalette(
          landColor = MapImagePainter.RedGreenBlueColor(182, 188, 192),
          seaColor = MapImagePainter.RedGreenBlueColor(102, 128, 151),
          borderColor = MapImagePainter.RedGreenBlueColor(36, 36, 42),
          provinceAnchorColor = MapImagePainter.RedGreenBlueColor(255, 255, 255)
        )
      else MapImagePainter.defaultPalette

    val variantHighlightColor =
      terrainVariantKind match
        case TerrainVariantKind.Forest => MapImagePainter.RedGreenBlueColor(64, 120, 70)
        case TerrainVariantKind.Waste => MapImagePainter.RedGreenBlueColor(170, 126, 74)
        case TerrainVariantKind.Farm => MapImagePainter.RedGreenBlueColor(188, 174, 92)
        case TerrainVariantKind.Swamp => MapImagePainter.RedGreenBlueColor(98, 124, 66)
        case TerrainVariantKind.Highland => MapImagePainter.RedGreenBlueColor(128, 128, 116)
        case TerrainVariantKind.Plain => MapImagePainter.RedGreenBlueColor(148, 168, 102)
        case TerrainVariantKind.Kelp => MapImagePainter.RedGreenBlueColor(58, 114, 90)
        case TerrainVariantKind.Water => MapImagePainter.RedGreenBlueColor(56, 106, 148)

    val baseImage = MapImagePainter.paint(ownership, terrainMaskByProvince, defaultPalette)
    val bytes = baseImage.redGreenBlueBytes.clone()

    var pixelIndex = 0
    while pixelIndex < ownership.provinceIdentifierByPixel.length do
      val provinceIdentifier = ownership.provinceIdentifierByPixel(pixelIndex)
      if provinceIdentifier > 0 then
        val provinceId = ProvinceId(provinceIdentifier)
        if terrainMaskByProvince.get(provinceId).exists(mask => matchesVariant(mask, terrainVariantKind)) then
          val byteIndex = pixelIndex * 3
          bytes(byteIndex) = variantHighlightColor.red.toByte
          bytes(byteIndex + 1) = variantHighlightColor.green.toByte
          bytes(byteIndex + 2) = variantHighlightColor.blue.toByte
      pixelIndex += 1

    MapImagePainter.MapImage(baseImage.widthPixels, baseImage.heightPixels, bytes)

  private def matchesVariant(maskValue: Long, terrainVariantKind: TerrainVariantKind): Boolean =
    val mask = TerrainMask(maskValue)

    def isSea: Boolean = mask.hasFlag(TerrainFlag.Sea) || mask.hasFlag(TerrainFlag.DeepSea)
    def isForest: Boolean = mask.hasFlag(TerrainFlag.Forest)
    def isWaste: Boolean = mask.hasFlag(TerrainFlag.Waste)
    def isFarm: Boolean = mask.hasFlag(TerrainFlag.Farm)
    def isSwamp: Boolean = mask.hasFlag(TerrainFlag.Swamp)
    def isHighland: Boolean = mask.hasFlag(TerrainFlag.Highlands)

    terrainVariantKind match
      case TerrainVariantKind.Forest => isForest && !isSea
      case TerrainVariantKind.Waste => isWaste && !isSea
      case TerrainVariantKind.Farm => isFarm && !isSea
      case TerrainVariantKind.Swamp => isSwamp && !isSea
      case TerrainVariantKind.Highland => isHighland && !isSea
      case TerrainVariantKind.Plain => !isSea && !isForest && !isWaste && !isFarm && !isSwamp && !isHighland
      case TerrainVariantKind.Kelp => isSea && isForest
      case TerrainVariantKind.Water => isSea
