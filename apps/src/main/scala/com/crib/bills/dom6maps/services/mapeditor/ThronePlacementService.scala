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

import cats.{MonadError, Traverse}

trait ThronePlacementService[Sequencer[_]]:
  def update[ErrorChannel[_]](
      state: MapState,
      thrones: Vector[ThronePlacement]
  )(using MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]): Sequencer[ErrorChannel[MapState]]

class ThronePlacementServiceImpl[Sequencer[_]: Sync] extends ThronePlacementService[Sequencer]:
  protected val sequencer = summon[Sync[Sequencer]]

<<<<<<< HEAD
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

=======
  private def validate(state: MapState, thrones: Vector[ThronePlacement]): Option[IllegalArgumentException] =
    val oob = thrones.filter(tp => state.provinceLocations.provinceIdAt(tp.location).isEmpty)
    val missingSpec = thrones.filter(tp => tp.level.isEmpty && tp.id.isEmpty)
    val bounds =
      val locs = state.provinceLocations.indexByLocation.keys
      if locs.nonEmpty then
        val maxX = locs.map(_.x.value).max
        val maxY = locs.map(_.y.value).max
        Some((maxX, maxY))
      else None
    val oobMsg =
      if oob.nonEmpty then
        val header = bounds match
          case Some((maxX, maxY)) => s"Out-of-bounds throne overrides (valid cells: x=0..$maxX, y=0..$maxY): ${oob.size}"
          case None               => s"Out-of-bounds throne overrides: ${oob.size}"
        val details =
          oob
            .map { tp =>
              val spec = tp.level.map(l => s"level=${l.value}").orElse(tp.id.map(fid => s"id=${fid.value}")).getOrElse("")
              s" - at (${tp.location.x.value},${tp.location.y.value}) $spec"
            }
            .mkString("\n")
        Some(s"""${header}
${details}""")
      else None
    val missingMsg =
      if missingSpec.nonEmpty then
        val header = s"Invalid throne overrides (missing level or id): ${missingSpec.size}"
        val details = missingSpec
          .map(tp => s" - at (${tp.location.x.value},${tp.location.y.value})")
          .mkString("\n")
        Some(s"""${header}
${details}""")
      else None
    (oobMsg, missingMsg) match
      case (Some(a), Some(b)) => Some(new IllegalArgumentException(a + "\n" + b))
      case (Some(a), None)    => Some(new IllegalArgumentException(a))
      case (None, Some(b))    => Some(new IllegalArgumentException(b))
      case _                  => None

  private def featureIdFor(level: ThroneLevel, used: Set[FeatureId]): Sequencer[Option[FeatureId]] =
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

  override def update[ErrorChannel[_]](
      state: MapState,
      thrones: Vector[ThronePlacement]
  )(using errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]): Sequencer[ErrorChannel[MapState]] =
    validate(state, thrones) match
      case Some(err) => errorChannel.raiseError[MapState](err).pure[Sequencer]
      case None =>
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
                              case Some(fid) => ((acc :+ (id, fid), used + fid)).pure[Sequencer]
                              case None      => (acc, used).pure[Sequencer]
                            }
                          case None => (acc, used).pure[Sequencer]
                  case None => (acc, used).pure[Sequencer]
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
        yield updatedState.pure[ErrorChannel]

>>>>>>> ff5e655 ([Fix] Fatal OOB validation for throne placement; overwrite copier; add headless and inspector CLIs; workflow returns Either)
class ThronePlacementServiceStub[Sequencer[_]: Applicative] extends ThronePlacementService[Sequencer]:
  override def update[ErrorChannel[_]](
      state: MapState,
      thrones: Vector[ThronePlacement]
  )(using MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]): Sequencer[ErrorChannel[MapState]] =
    state.pure[ErrorChannel].pure[Sequencer]
