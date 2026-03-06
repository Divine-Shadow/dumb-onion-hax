package com.crib.bills.dom6maps
package model.map

final case class MapWidthInProvinces(value: Int) extends AnyVal
final case class MapHeightInProvinces(value: Int) extends AnyVal

final case class MapDimensions private (
    width: MapWidthInProvinces,
    height: MapHeightInProvinces
)

object MapDimensions:
  def from(
      width: Int,
      height: Int
  ): Either[Throwable, MapDimensions] =
    if width <= 0 then Left(IllegalArgumentException(s"Map width must be positive, received: $width"))
    else if height <= 0 then Left(IllegalArgumentException(s"Map height must be positive, received: $height"))
    else Right(MapDimensions(MapWidthInProvinces(width), MapHeightInProvinces(height)))

  def square(size: MapSize): MapDimensions =
    MapDimensions(MapWidthInProvinces(size.value), MapHeightInProvinces(size.value))
