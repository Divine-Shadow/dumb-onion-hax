package com.crib.bills.dom6maps
package model.map

final case class ThroneFeatureConfig(
    randomLevelOne: Vector[ProvinceLocation],
    randomLevelTwo: Vector[ProvinceLocation],
    fixed: Vector[ThronePlacement]
):
  def placements: Vector[ThronePlacement] =
    randomLevelOne.map(loc => ThronePlacement(loc, ThroneLevel(1))) ++
      randomLevelTwo.map(loc => ThronePlacement(loc, ThroneLevel(2))) ++
      fixed
