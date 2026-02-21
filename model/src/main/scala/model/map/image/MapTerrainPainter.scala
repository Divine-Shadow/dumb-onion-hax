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
        PrimaryTerrainType.Sea -> MapImagePainter.RedGreenBlueColor(56, 106, 148),
        PrimaryTerrainType.Forest -> MapImagePainter.RedGreenBlueColor(64, 120, 70),
        PrimaryTerrainType.Farm -> MapImagePainter.RedGreenBlueColor(188, 174, 92),
        PrimaryTerrainType.Swamp -> MapImagePainter.RedGreenBlueColor(98, 124, 66),
        PrimaryTerrainType.Waste -> MapImagePainter.RedGreenBlueColor(170, 126, 74),
        PrimaryTerrainType.Highland -> MapImagePainter.RedGreenBlueColor(128, 128, 116),
        PrimaryTerrainType.Plain -> MapImagePainter.RedGreenBlueColor(148, 168, 102)
      ),
      backgroundColor = MapImagePainter.RedGreenBlueColor(56, 106, 148),
      borderColor = MapImagePainter.defaultPalette.borderColor,
      provinceAnchorColor = MapImagePainter.defaultPalette.provinceAnchorColor
    )

  def resolvePrimaryTerrainType(terrainMask: TerrainMask): PrimaryTerrainType =
    if terrainMask.hasFlag(TerrainFlag.Sea) || terrainMask.hasFlag(TerrainFlag.DeepSea) then PrimaryTerrainType.Sea
    else if terrainMask.hasFlag(TerrainFlag.Forest) then PrimaryTerrainType.Forest
    else if terrainMask.hasFlag(TerrainFlag.Farm) then PrimaryTerrainType.Farm
    else if terrainMask.hasFlag(TerrainFlag.Swamp) then PrimaryTerrainType.Swamp
    else if terrainMask.hasFlag(TerrainFlag.Waste) then PrimaryTerrainType.Waste
    else if terrainMask.hasFlag(TerrainFlag.Highlands) || terrainMask.hasFlag(TerrainFlag.Mountains) then
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
