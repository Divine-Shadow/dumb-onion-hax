package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.Applicative
import cats.syntax.all.*
import cats.effect.Sync
import model.{ProvinceId, TerrainFlag, TerrainMask}
import model.map.{
  FeatureId,
  MapState,
  ProvinceFeature,
  Terrain,
  ThronePlacement,
  ThroneLevel
}

trait ThronePlacementService[Sequencer[_]]:
  def update(state: MapState, thrones: Vector[ThronePlacement]): Sequencer[MapState]

class ThronePlacementServiceImpl[Sequencer[_]: Sync] extends ThronePlacementService[Sequencer]:
  protected val sequencer = summon[Sync[Sequencer]]

  private def featureIdFor(level: ThroneLevel): FeatureId =
    level.value match
      case 1 => FeatureId(5001)
      case 2 => FeatureId(5002)
      case 3 => FeatureId(5003)
      case other => FeatureId(5000 + other)

  override def update(state: MapState, thrones: Vector[ThronePlacement]): Sequencer[MapState] =
    val resolved: Vector[(ProvinceId, ThroneLevel)] = thrones.flatMap { tp =>
      state.provinceLocations.provinceIdAt(tp.location) match
        case Some(id) => (id, tp.level) :: Nil
        case None =>
          println(s"Unresolved throne location: ${tp.location}")
          Nil
    }
    val throneSet = resolved.map(_._1).toSet
    val updatedTerrains = state.terrains.map {
      case t @ Terrain(province, mask) =>
        val updated =
          if throneSet.contains(province) then
            TerrainMask(mask).withFlag(TerrainFlag.Throne)
          else TerrainMask(mask).withoutFlag(TerrainFlag.Throne)
        t.copy(mask = updated.value)
    }
    val features = resolved.map { case (province, level) =>
      ProvinceFeature(province, featureIdFor(level))
    }
    val updatedState = state.copy(terrains = updatedTerrains, features = features)
    sequencer.delay(println(s"Placing ${resolved.size} thrones")).as(updatedState)

class ThronePlacementServiceStub[Sequencer[_]: Applicative] extends ThronePlacementService[Sequencer]:
  override def update(state: MapState, thrones: Vector[ThronePlacement]): Sequencer[MapState] =
    state.pure[Sequencer]
