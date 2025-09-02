package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{Applicative, MonadError, Traverse}
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import cats.effect.Sync
import model.ProvinceId
import model.map.{MapDirective, SetLand, Feature, FeatureId, ThroneFeatureConfig}
import scala.annotation.tailrec
import model.dominions.{Feature as DomFeature}

trait ThroneFeatureService[Sequencer[_]]:
  def apply[ErrorChannel[_]](
      map: Path,
      config: ThroneFeatureConfig,
      output: Path
    )(using Files[Sequencer],
          MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[Unit]]

class ThroneFeatureServiceImpl[Sequencer[_]: Sync](
    loader: MapLayerLoader[Sequencer],
    throneService: ThronePlacementService[Sequencer],
    writer: MapWriter[Sequencer]
) extends ThroneFeatureService[Sequencer]:
  protected val sequencer = summon[Sync[Sequencer]]

  private val throneIds: Set[Int] = DomFeature.thrones.map(_.id.value).toSet

  private def stripThroneFeatures(passThrough: Vector[MapDirective]): Vector[MapDirective] =
    @tailrec
    def loop(
        remaining: List[MapDirective],
        currentProvince: Option[ProvinceId],
        acc: Vector[MapDirective]
    ): Vector[MapDirective] =
      remaining match
        case SetLand(province) :: tail =>
          loop(tail, Some(province), acc :+ SetLand(province))
        case Feature(id) :: tail if currentProvince.nonEmpty && throneIds.contains(id.value) =>
          loop(tail, currentProvince, acc)
        case head :: tail =>
          loop(tail, currentProvince, acc :+ head)
        case Nil => acc
    loop(passThrough.toList, None, Vector.empty)

  override def apply[ErrorChannel[_]](
      map: Path,
      config: ThroneFeatureConfig,
      output: Path
    )(using Files[Sequencer],
          MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[Unit]] =
    val placements = config.placements
    for
      layerEC <- loader.load[ErrorChannel](map)
      result <- layerEC.flatTraverse { layer =>
        for
          updatedEC <- throneService.update[ErrorChannel](layer.state, placements)
          directives <- layer.passThrough.compile.toVector
          filtered = stripThroneFeatures(directives)
          writtenEC <- updatedEC.traverse(updated => writer.write[ErrorChannel](updated, filtered, output))
        yield writtenEC
      }
    yield result.flatMap(identity)

class ThroneFeatureServiceStub[Sequencer[_]: Applicative] extends ThroneFeatureService[Sequencer]:
  override def apply[ErrorChannel[_]](
      map: Path,
      config: ThroneFeatureConfig,
      output: Path
    )(using Files[Sequencer],
          MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[Unit]] =
    ().pure[ErrorChannel].pure[Sequencer]
