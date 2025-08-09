package com.crib.bills.dom6maps
package apps

import cats.effect.IO
import fs2.io.file.{Files, Path}
import com.crib.bills.dom6maps.model.map.{
  HWrapAround,
  MapFileParser,
  MapWidth,
  MapHeight,
  Neighbour,
  NoWrapAround,
  VWrapAround
}
import WrapSever.{isTopBottom, isLeftRight}
import weaver.SimpleIOSuite

object WrapSeverAppSpec extends SimpleIOSuite:

  test("adds hwrap and removes top-bottom neighbours") {
    MapFileParser
      .parseFile[IO](Path("data/five-by-twelve.map"))
      .through(WrapSever.verticalPipe[IO](MapWidth(5), MapHeight(12)))
      .compile
      .toVector
      .map { directives =>
        val hasHWrap = directives.contains(HWrapAround)
        val hasNoWrap = directives.contains(NoWrapAround)
        val hasTopBottom = directives.exists {
          case Neighbour(a, b) => isTopBottom(a, b, MapWidth(5), MapHeight(12))
          case _               => false
        }
        val hasMiddle = directives.exists {
          case Neighbour(a, b) => a.value == 6 && b.value == 7
          case _               => false
        }
        expect(hasHWrap && !hasNoWrap && !hasTopBottom && hasMiddle)
      }
  }

  test("vertical sever removes 1-57 border in duel map") {
    MapFileParser
      .parseFile[IO](Path("data/duel-map-example.map"))
      .through(WrapSever.verticalPipe[IO](MapWidth(5), MapHeight(12)))
      .compile
      .toVector
      .map { directives =>
        val stillConnected = directives.exists {
          case Neighbour(a, b) =>
            (a.value == 1 && b.value == 57) || (a.value == 57 && b.value == 1)
          case _ => false
        }
        expect(!stillConnected)
      }
  }

  test("run writes processed directives to file") {
    val output = Path("data/five-by-twelve.hwrap.map")
    Files[IO]
      .deleteIfExists(output)
      .flatMap(_ => WrapSeverApp.run)
      .flatMap(_ => MapFileParser.parseFile[IO](output).compile.toVector)
      .map { directives =>
        val hasHWrap = directives.contains(HWrapAround)
        val hasNoWrap = directives.contains(NoWrapAround)
        val hasTopBottom = directives.exists {
          case Neighbour(a, b) => isTopBottom(a, b, MapWidth(5), MapHeight(12))
          case _               => false
        }
        expect(hasHWrap && !hasNoWrap && !hasTopBottom)
      }
  }

  test("adds vwrap and removes left-right neighbours") {
    MapFileParser
      .parseFile[IO](Path("data/five-by-twelve-horizontal.map"))
      .through(WrapSever.horizontalPipe[IO](MapWidth(5), MapHeight(12)))
      .compile
      .toVector
      .map { directives =>
        val hasVWrap = directives.contains(VWrapAround)
        val hasLeftRight = directives.exists {
          case Neighbour(a, b) => isLeftRight(a, b, MapWidth(5))
          case _               => false
        }
        val hasMiddle = directives.exists {
          case Neighbour(a, b) => a.value == 6 && b.value == 7
          case _               => false
        }
        expect(hasVWrap && !hasLeftRight && hasMiddle)
      }
  }
