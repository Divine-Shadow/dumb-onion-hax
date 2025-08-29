package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{Applicative, MonadError, Traverse}
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import cats.effect.Sync
import model.map.{MapDirective, SetLand, Feature, FeatureId, ThroneFeatureConfig}
import scala.annotation.tailrec

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

  private def stripThroneFeatures(passThrough: Vector[MapDirective]): Vector[MapDirective] =
    @tailrec
    def loop(remaining: List[MapDirective], acc: Vector[MapDirective]): Vector[MapDirective] =
      remaining match
        case SetLand(_) :: Feature(id) :: tail if id.value >= 5000 =>
          loop(tail, acc)
        case head :: tail =>
          loop(tail, acc :+ head)
        case Nil => acc
    loop(passThrough.toList, Vector.empty)

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
      result <- layerEC.traverse { layer =>
        for
          updated <- throneService.update(layer.state, placements)
          directives <- layer.passThrough.compile.toVector
          filtered = stripThroneFeatures(directives)
          _ <- writer.write[ErrorChannel](updated, filtered, output)
        yield ()
      }
    yield result

class ThroneFeatureServiceStub[Sequencer[_]: Applicative] extends ThroneFeatureService[Sequencer]:
  override def apply[ErrorChannel[_]](
      map: Path,
      config: ThroneFeatureConfig,
      output: Path
    )(using Files[Sequencer],
          MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[Unit]] =
    ().pure[ErrorChannel].pure[Sequencer]
