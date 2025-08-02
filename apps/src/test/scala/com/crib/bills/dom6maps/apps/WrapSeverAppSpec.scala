package com.crib.bills.dom6maps
package apps

import cats.effect.IO
import fs2.io.file.{Files, Path}
import com.crib.bills.dom6maps.model.ProvinceId
import com.crib.bills.dom6maps.model.map.{HWrapAround, MapFileParser, Neighbour}
import weaver.SimpleIOSuite

object WrapSeverAppSpec extends SimpleIOSuite:
  private val mapWidth = 5
  private val mapHeight = 12

  private def isTopBottom(a: ProvinceId, b: ProvinceId): Boolean =
    val rowA = (a.value - 1) / mapWidth
    val rowB = (b.value - 1) / mapWidth
    val top = 0
    val bottom = mapHeight - 1
    (rowA == top && rowB == bottom) || (rowA == bottom && rowB == top)

  test("adds hwrap and removes top-bottom neighbours") {
    MapFileParser
      .parseFile[IO](Path("data/five-by-twelve.map"))
      .compile
      .toVector
      .map(WrapSeverApp.process)
      .map { directives =>
        val hasHWrap = directives.contains(HWrapAround)
        val hasTopBottom = directives.exists {
          case Neighbour(a, b) => isTopBottom(a, b)
          case _               => false
        }
        val hasMiddle = directives.exists {
          case Neighbour(a, b) => a.value == 6 && b.value == 7
          case _               => false
        }
        expect(hasHWrap && !hasTopBottom && hasMiddle)
      }
  }
