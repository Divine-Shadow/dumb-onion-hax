package com.crib.bills.dom6maps
package model.map

final case class GroundSurfaceDuelConfig(throneLevel: ThroneLevel)

object GroundSurfaceDuelConfig:
  val default: GroundSurfaceDuelConfig = GroundSurfaceDuelConfig(ThroneLevel(1))
