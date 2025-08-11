package com.crib.bills.dom6maps
package model.map

import model.ProvinceId

object CenterProvince:
  def of(size: MapSize): ProvinceId =
    val n = size.value
    val mid = (n + 1) / 2
    ProvinceId((mid - 1) * n + mid)
