package com.crib.bills.dom6maps
package model.map

final case class ThronePlacement(
    location: ProvinceLocation,
    level: Option[ThroneLevel],
    id: Option[FeatureId]
)

object ThronePlacement:
  import scala.annotation.targetName

  @targetName("fromLevel")
  def apply(location: ProvinceLocation, level: ThroneLevel): ThronePlacement =
    new ThronePlacement(location, Some(level), None)

  @targetName("fromId")
  def apply(location: ProvinceLocation, id: FeatureId): ThronePlacement =
    new ThronePlacement(location, None, Some(id))

