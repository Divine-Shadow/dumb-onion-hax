package com.crib.bills.dom6maps
package model.map.image

import model.{ProvinceId, TerrainFlag, TerrainMask}

trait MapTerrainPainter:
  def paint(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      terrainMaskByProvince: Map[ProvinceId, Long]
  ): MapImagePainter.MapImage

final class ConstantColorMapTerrainPainter(
    palette: MapImagePainter.PainterPalette = MapImagePainter.defaultPalette
) extends MapTerrainPainter:
  override def paint(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      terrainMaskByProvince: Map[ProvinceId, Long]
  ): MapImagePainter.MapImage =
    MapImagePainter.paint(ownership, terrainMaskByProvince, palette)

object PrimaryTerrainColorMapTerrainPainter:
  enum PrimaryTerrainType:
    case Sea
    case Forest
    case Farm
    case Swamp
    case Waste
    case Highland
    case Mountain
    case Cave
    case Plain

  final case class PrimaryTerrainPalette(
      colorByPrimaryTerrainType: Map[PrimaryTerrainType, MapImagePainter.RedGreenBlueColor],
      backgroundColor: MapImagePainter.RedGreenBlueColor,
      borderColor: MapImagePainter.RedGreenBlueColor,
      provinceAnchorColor: MapImagePainter.RedGreenBlueColor
  )

  val defaultPalette: PrimaryTerrainPalette =
    PrimaryTerrainPalette(
      colorByPrimaryTerrainType = Map(
        PrimaryTerrainType.Sea -> MapImagePainter.RedGreenBlueColor(56, 126, 202),
        PrimaryTerrainType.Forest -> MapImagePainter.RedGreenBlueColor(36, 122, 56),
        PrimaryTerrainType.Farm -> MapImagePainter.RedGreenBlueColor(228, 206, 92),
        PrimaryTerrainType.Swamp -> MapImagePainter.RedGreenBlueColor(82, 102, 52),
        PrimaryTerrainType.Waste -> MapImagePainter.RedGreenBlueColor(191, 122, 64),
        PrimaryTerrainType.Highland -> MapImagePainter.RedGreenBlueColor(142, 170, 112),
        PrimaryTerrainType.Mountain -> MapImagePainter.RedGreenBlueColor(126, 126, 126),
        PrimaryTerrainType.Cave -> MapImagePainter.RedGreenBlueColor(96, 86, 130),
        PrimaryTerrainType.Plain -> MapImagePainter.RedGreenBlueColor(196, 174, 122)
      ),
      backgroundColor = MapImagePainter.RedGreenBlueColor(56, 126, 202),
      borderColor = MapImagePainter.defaultPalette.borderColor,
      provinceAnchorColor = MapImagePainter.defaultPalette.provinceAnchorColor
    )

  def resolvePrimaryTerrainType(terrainMask: TerrainMask): PrimaryTerrainType =
    if terrainMask.hasFlag(TerrainFlag.Sea) || terrainMask.hasFlag(TerrainFlag.DeepSea) then PrimaryTerrainType.Sea
    else if terrainMask.hasFlag(TerrainFlag.Cave) then PrimaryTerrainType.Cave
    else if terrainMask.hasFlag(TerrainFlag.Mountains) then PrimaryTerrainType.Mountain
    else if terrainMask.hasFlag(TerrainFlag.Forest) then PrimaryTerrainType.Forest
    else if terrainMask.hasFlag(TerrainFlag.Farm) then PrimaryTerrainType.Farm
    else if terrainMask.hasFlag(TerrainFlag.Swamp) then PrimaryTerrainType.Swamp
    else if terrainMask.hasFlag(TerrainFlag.Waste) then PrimaryTerrainType.Waste
    else if terrainMask.hasFlag(TerrainFlag.Highlands) then
      PrimaryTerrainType.Highland
    else PrimaryTerrainType.Plain

final class PrimaryTerrainColorMapTerrainPainter(
    palette: PrimaryTerrainColorMapTerrainPainter.PrimaryTerrainPalette =
      PrimaryTerrainColorMapTerrainPainter.defaultPalette
) extends MapTerrainPainter:
  import PrimaryTerrainColorMapTerrainPainter.*

  override def paint(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      terrainMaskByProvince: Map[ProvinceId, Long]
  ): MapImagePainter.MapImage =
    MapImagePainter.paintWithProvinceColor(
      ownership = ownership,
      provinceColor = provinceId => resolveProvinceColor(provinceId, terrainMaskByProvince),
      backgroundColor = palette.backgroundColor,
      borderColor = palette.borderColor,
      provinceAnchorColor = palette.provinceAnchorColor
    )

  private def resolveProvinceColor(
      provinceId: ProvinceId,
      terrainMaskByProvince: Map[ProvinceId, Long]
  ): MapImagePainter.RedGreenBlueColor =
    val primaryTerrainType = terrainMaskByProvince
      .get(provinceId)
      .map(TerrainMask.apply)
      .map(resolvePrimaryTerrainType)
      .getOrElse(PrimaryTerrainType.Plain)

    palette.colorByPrimaryTerrainType
      .getOrElse(primaryTerrainType, palette.colorByPrimaryTerrainType(PrimaryTerrainType.Plain))
