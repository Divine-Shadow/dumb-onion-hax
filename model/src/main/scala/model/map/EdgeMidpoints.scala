package com.crib.bills.dom6maps
package model.map

import model.ProvinceId

final case class EdgeMidpoints(top: ProvinceId, bottom: ProvinceId, left: ProvinceId, right: ProvinceId)

object EdgeMidpoints:
  def of(size: MapSize): EdgeMidpoints =
    val n = size.value
    val mid = (n + 1) / 2
    val top = ProvinceId(mid)
    val bottom = ProvinceId((n - 1) * n + mid)
    val left = ProvinceId((mid - 1) * n + 1)
    val right = ProvinceId((mid - 1) * n + n)
    EdgeMidpoints(top, bottom, left, right)
