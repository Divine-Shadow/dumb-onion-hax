package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.Applicative
import cats.syntax.all.*
import model.TerrainFlag
import model.TerrainMask
import model.map.{MapState, Terrain, ThronePlacement}

trait ThronePlacementService[Sequencer[_]]:
  def update(state: MapState, thrones: Vector[ThronePlacement]): Sequencer[MapState]

class ThronePlacementServiceImpl[Sequencer[_]: Applicative] extends ThronePlacementService[Sequencer]:
  override def update(state: MapState, thrones: Vector[ThronePlacement]): Sequencer[MapState] =
    val throneSet = thrones.map(_.province).toSet
    val updatedTerrains = state.terrains.map {
      case t @ Terrain(province, mask) =>
        val updated =
          if throneSet.contains(province) then
            TerrainMask(mask).withFlag(TerrainFlag.Throne)
          else TerrainMask(mask).withoutFlag(TerrainFlag.Throne)
        t.copy(mask = updated.value)
    }
    state.copy(terrains = updatedTerrains).pure[Sequencer]

class ThronePlacementServiceStub[Sequencer[_]: Applicative] extends ThronePlacementService[Sequencer]:
  override def update(state: MapState, thrones: Vector[ThronePlacement]): Sequencer[MapState] =
    state.pure[Sequencer]
