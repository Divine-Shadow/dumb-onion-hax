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
import model.dominions.{Feature as DomFeature}

trait ThronePlacementService[Sequencer[_]]:
  def update(state: MapState, thrones: Vector[ThronePlacement]): Sequencer[MapState]

class ThronePlacementServiceImpl[Sequencer[_]: Sync] extends ThronePlacementService[Sequencer]:
  protected val sequencer = summon[Sync[Sequencer]]

  private def featureIdFor(
      level: ThroneLevel,
      used: Set[FeatureId]
  ): Sequencer[Option[FeatureId]] =
    val thrones = level.value match
      case 1 => DomFeature.levelOneThrones
      case 2 => DomFeature.levelTwoThrones
      case 3 => DomFeature.levelThreeThrones
      case _ => Nil
    val remaining = thrones.filterNot(t => used.contains(FeatureId(t.id.value)))
    if remaining.isEmpty then None.pure[Sequencer]
    else
      sequencer.delay {
        val idx = scala.util.Random.nextInt(remaining.size)
        Some(FeatureId(remaining(idx).id.value))
      }

  override def update(state: MapState, thrones: Vector[ThronePlacement]): Sequencer[MapState] =
    for
      resolved <- thrones
        .foldM((Vector.empty[(ProvinceId, FeatureId)], Set.empty[FeatureId])) {
          case ((acc, used), tp) =>
            state.provinceLocations.provinceIdAt(tp.location) match
              case Some(id) =>
                tp.id match
                  case Some(fid) =>
                    ((acc :+ (id, fid), used + fid)).pure[Sequencer]
                  case None =>
                    tp.level match
                      case Some(level) =>
                        featureIdFor(level, used).flatMap {
                          case Some(fid) =>
                            ((acc :+ (id, fid), used + fid)).pure[Sequencer]
                          case None =>
                            sequencer
                              .delay(
                                println(
                                  s"No remaining thrones for level ${level.value} at location: ${tp.location}"
                                )
                              )
                              .as((acc, used))
                        }
                      case None =>
                        sequencer
                          .delay(
                            println(
                              s"Missing throne level or id at location: ${tp.location}"
                            )
                          )
                          .as((acc, used))
              case None =>
                sequencer
                  .delay(println(s"Unresolved throne location: ${tp.location}"))
                  .as((acc, used))
        }
        .map(_._1)
      throneSet = resolved.map(_._1).toSet
      updatedTerrains = state.terrains.map {
        case t @ Terrain(province, mask) =>
          val updated =
            if throneSet.contains(province) then
              TerrainMask(mask).withFlag(TerrainFlag.Throne)
            else TerrainMask(mask).withoutFlag(TerrainFlag.Throne)
          t.copy(mask = updated.value)
      }
      existingFeatures = state.features.filterNot(pf => throneSet.contains(pf.province))
      newFeatures = resolved.map { case (province, fid) =>
        ProvinceFeature(province, fid)
      }
      updatedState = state.copy(
        terrains = updatedTerrains,
        features = existingFeatures ++ newFeatures
      )
      _ <- sequencer.delay(println(s"Placing ${resolved.size} thrones"))
    yield updatedState

class ThronePlacementServiceStub[Sequencer[_]: Applicative] extends ThronePlacementService[Sequencer]:
  override def update(state: MapState, thrones: Vector[ThronePlacement]): Sequencer[MapState] =
    state.pure[Sequencer]
