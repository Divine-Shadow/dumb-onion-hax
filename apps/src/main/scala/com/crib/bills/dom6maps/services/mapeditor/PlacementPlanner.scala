package com.crib.bills.dom6maps
package apps.services.mapeditor

import model.ProvinceId
import model.map.*

trait PlacementPlanner[Sequencer[_]]:
  def plan(size: MapSize, config: GroundSurfaceDuelConfig): (Vector[GateSpec], Vector[ThronePlacement])

class PlacementPlannerImpl[Sequencer[_]] extends PlacementPlanner[Sequencer]:
  override def plan(size: MapSize, config: GroundSurfaceDuelConfig): (Vector[GateSpec], Vector[ThronePlacement]) =
    val mids = EdgeMidpoints.of(size)
    val corners = CornerProvinces.all(size)
    val offset = size.value * size.value
    val gates = Vector(
      GateSpec(mids.top, ProvinceId(mids.top.value + offset)),
      GateSpec(mids.bottom, ProvinceId(mids.bottom.value + offset)),
      GateSpec(mids.left, ProvinceId(mids.left.value + offset)),
      GateSpec(mids.right, ProvinceId(mids.right.value + offset))
    )
    val level = config.throneLevel
    val thrones = corners.map(p => ThronePlacement(p, level))
    (gates, thrones)

class PlacementPlannerStub[Sequencer[_]](
    gates: Vector[GateSpec],
    thrones: Vector[ThronePlacement]
) extends PlacementPlanner[Sequencer]:
  override def plan(size: MapSize, config: GroundSurfaceDuelConfig): (Vector[GateSpec], Vector[ThronePlacement]) =
    (gates, thrones)
