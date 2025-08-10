package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.Applicative
import fs2.Pipe
import model.TerrainFlag
import model.TerrainMask
import model.map.{MapDirective, Terrain, ThronePlacement}

trait ThronePlacementService[Sequencer[_]]:
  def pipe(thrones: Vector[ThronePlacement]): Pipe[Sequencer, MapDirective, MapDirective]

class ThronePlacementServiceImpl[Sequencer[_]] extends ThronePlacementService[Sequencer]:
  override def pipe(thrones: Vector[ThronePlacement]): Pipe[Sequencer, MapDirective, MapDirective] =
    in =>
      val throneSet = thrones.map(_.province).toSet
      in.map {
        case t @ Terrain(province, mask) =>
          val updated =
            if throneSet.contains(province) then
              TerrainMask(mask).withFlag(TerrainFlag.Throne)
            else TerrainMask(mask).withoutFlag(TerrainFlag.Throne)
          t.copy(mask = updated.value)
        case d => d
      }

class ThronePlacementServiceStub[Sequencer[_]: Applicative] extends ThronePlacementService[Sequencer]:
  override def pipe(thrones: Vector[ThronePlacement]): Pipe[Sequencer, MapDirective, MapDirective] =
    stream => stream
