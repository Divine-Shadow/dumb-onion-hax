package com.crib.bills.dom6maps
package model.map

import model.ProvinceId

final case class ThroneFeatureConfig(
    randomLevelOne: Vector[ProvinceId],
    randomLevelTwo: Vector[ProvinceId],
    fixed: Vector[ThronePlacement]
):
  def placements: Vector[ThronePlacement] =
    randomLevelOne.map(p => ThronePlacement(p, ThroneLevel(1))) ++
      randomLevelTwo.map(p => ThronePlacement(p, ThroneLevel(2))) ++
      fixed
