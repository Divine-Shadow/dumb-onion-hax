package com.crib.bills.dom6maps
package apps

import cats.effect.{IO, IOApp}
import fs2.io.file.{Files, Path}
import com.crib.bills.dom6maps.model.ProvinceId
import com.crib.bills.dom6maps.model.map.{MapDirective, MapFileParser, Neighbour, NeighbourSpec, HWrapAround, MapWidth, MapHeight}
import com.crib.bills.dom6maps.model.map.Renderer.*

object WrapSeverApp extends IOApp.Simple:
  private val inputFile = Path("data") / "five-by-twelve.map"
  private val mapWidth = MapWidth(5)
  private val mapHeight = MapHeight(12)

  def process(directives: Vector[MapDirective]): Vector[MapDirective] =
    val withoutConnections = directives.filter {
      case Neighbour(a, b)      => !isTopBottom(a, b)
      case NeighbourSpec(a, b, _) => !isTopBottom(a, b)
      case _                    => true
    }
    val withoutWrapDirective = withoutConnections.filterNot(_ == HWrapAround)
    withoutWrapDirective :+ HWrapAround

  private def isTopBottom(a: ProvinceId, b: ProvinceId): Boolean =
    val rowA = (a.value - 1) / mapWidth.value
    val rowB = (b.value - 1) / mapWidth.value
    val top = 0
    val bottom = mapHeight.value - 1
    (rowA == top && rowB == bottom) || (rowA == bottom && rowB == top)

  def run: IO[Unit] =
    MapFileParser
      .parseFile[IO](inputFile)
      .compile
      .toVector
      .map(process)
      .flatMap(ds => IO.println(ds.map(_.render).mkString("\n")))
