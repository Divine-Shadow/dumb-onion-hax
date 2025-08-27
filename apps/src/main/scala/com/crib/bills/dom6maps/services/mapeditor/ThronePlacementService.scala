package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.Applicative
import cats.syntax.all.*
import cats.effect.Sync
import model.{ProvinceId, TerrainFlag, TerrainMask}
import model.map.{MapState, Terrain, ThronePlacement}

trait ThronePlacementService[Sequencer[_]]:
  def update(state: MapState, thrones: Vector[ThronePlacement]): Sequencer[MapState]

class ThronePlacementServiceImpl[Sequencer[_]: Sync] extends ThronePlacementService[Sequencer]:
  protected val sequencer = summon[Sync[Sequencer]]

  override def update(state: MapState, thrones: Vector[ThronePlacement]): Sequencer[MapState] =
    val resolved: Vector[ProvinceId] = thrones.flatMap { tp =>
      state.provinceLocations.provinceIdAt(tp.location) match
        case Some(id) => id :: Nil
        case None =>
          println(s"Unresolved throne location: ${tp.location}")
          Nil
    }
    val throneSet = resolved.toSet
    val updatedTerrains = state.terrains.map {
      case t @ Terrain(province, mask) =>
        val updated =
          if throneSet.contains(province) then
            TerrainMask(mask).withFlag(TerrainFlag.Throne)
          else TerrainMask(mask).withoutFlag(TerrainFlag.Throne)
        t.copy(mask = updated.value)
    }
    val updatedState = state.copy(terrains = updatedTerrains)
    sequencer.delay(println(s"Placing ${resolved.size} thrones")).as(updatedState)

class ThronePlacementServiceStub[Sequencer[_]: Applicative] extends ThronePlacementService[Sequencer]:
  override def update(state: MapState, thrones: Vector[ThronePlacement]): Sequencer[MapState] =
    state.pure[Sequencer]
