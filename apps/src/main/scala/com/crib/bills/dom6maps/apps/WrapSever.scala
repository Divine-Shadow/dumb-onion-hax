package com.crib.bills.dom6maps
package apps

import com.crib.bills.dom6maps.model.ProvinceId
import com.crib.bills.dom6maps.model.map.{HWrapAround, MapDirective, MapHeight, MapWidth, Neighbour, NeighbourSpec, NoWrapAround}

object WrapSever:
  private val mapWidth = MapWidth(5)
  private val mapHeight = MapHeight(12)

  def isTopBottom(a: ProvinceId, b: ProvinceId): Boolean =
    val rowA = (a.value - 1) / mapWidth.value
    val rowB = (b.value - 1) / mapWidth.value
    val top = 0
    val bottom = mapHeight.value - 1
    (rowA == top && rowB == bottom) || (rowA == bottom && rowB == top)

  def process(directives: Vector[MapDirective]): Vector[MapDirective] =
    val withoutConnections = directives.filter {
      case Neighbour(a, b)        => !isTopBottom(a, b)
      case NeighbourSpec(a, b, _) => !isTopBottom(a, b)
      case _                      => true
    }
    val withoutWrapDirective = withoutConnections.filter {
      case HWrapAround | NoWrapAround => false
      case _                          => true
    }
    withoutWrapDirective :+ HWrapAround
