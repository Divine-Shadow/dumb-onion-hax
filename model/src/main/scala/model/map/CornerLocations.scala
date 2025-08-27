package com.crib.bills.dom6maps
package model.map

final case class CornerLocations(
    topLeft: ProvinceLocation,
    topRight: ProvinceLocation,
    bottomLeft: ProvinceLocation,
    bottomRight: ProvinceLocation
)

object CornerLocations:
  def of(size: MapSize): CornerLocations =
    val n = size.value
    val topLeft = ProvinceLocation(XCell(0), YCell(0))
    val topRight = ProvinceLocation(XCell(n - 1), YCell(0))
    val bottomLeft = ProvinceLocation(XCell(0), YCell(n - 1))
    val bottomRight = ProvinceLocation(XCell(n - 1), YCell(n - 1))
    CornerLocations(topLeft, topRight, bottomLeft, bottomRight)

  def all(size: MapSize): Vector[ProvinceLocation] =
    val c = of(size)
    Vector(c.topLeft, c.topRight, c.bottomLeft, c.bottomRight)
