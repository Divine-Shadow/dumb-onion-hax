package com.crib.bills.dom6maps
package model.map

import model.ProvinceId

final case class CornerProvinces(topLeft: ProvinceId, topRight: ProvinceId, bottomLeft: ProvinceId, bottomRight: ProvinceId)

object CornerProvinces:
  def of(size: MapSize): CornerProvinces =
    val n = size.value
    val topLeft = ProvinceId(1)
    val topRight = ProvinceId(n)
    val bottomLeft = ProvinceId((n - 1) * n + 1)
    val bottomRight = ProvinceId(n * n)
    CornerProvinces(topLeft, topRight, bottomLeft, bottomRight)

  def all(size: MapSize): Vector[ProvinceId] =
    val c = of(size)
    Vector(c.topLeft, c.topRight, c.bottomLeft, c.bottomRight)
