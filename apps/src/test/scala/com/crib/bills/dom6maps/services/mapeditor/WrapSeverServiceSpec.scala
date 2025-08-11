package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import fs2.io.file.Path
import model.{ProvinceId}
import model.map.{
  HWrapAround,
  MapFileParser,
  MapWidth,
  MapHeight,
  Neighbour,
  NoWrapAround,
  VWrapAround,
  NeighbourSpec
}
import weaver.SimpleIOSuite

object WrapSeverServiceSpec extends SimpleIOSuite:
  test("adds hwrap and removes top-bottom neighbours") {
    MapFileParser
      .parseFile[IO](Path("data/five-by-twelve.map"))
      .through(WrapSeverService.verticalPipe[IO](MapWidth(5), MapHeight(12)))
      .compile
      .toVector
      .map { directives =>
        val hasHWrap = directives.contains(HWrapAround)
        val hasNoWrap = directives.contains(NoWrapAround)
        val hasTopBottom = directives.exists {
          case Neighbour(a, b)       => WrapSeverService.isTopBottom(a, b, MapWidth(5), MapHeight(12))
          case NeighbourSpec(a, b, _) => WrapSeverService.isTopBottom(a, b, MapWidth(5), MapHeight(12))
          case _                     => false
        }
        val hasMiddle = directives.exists {
          case Neighbour(a, b) => a.value == 6 && b.value == 7
          case _               => false
        }
        expect(hasHWrap && !hasNoWrap && !hasTopBottom && hasMiddle)
      }
  }

  test("adds vwrap and removes left-right neighbours") {
    MapFileParser
      .parseFile[IO](Path("data/five-by-twelve-horizontal.map"))
      .through(WrapSeverService.horizontalPipe[IO](MapWidth(5), MapHeight(12)))
      .compile
      .toVector
      .map { directives =>
        val hasVWrap = directives.contains(VWrapAround)
        val hasLeftRight = directives.exists {
          case Neighbour(a, b)       => WrapSeverService.isLeftRight(a, b, MapWidth(5))
          case NeighbourSpec(a, b, _) => WrapSeverService.isLeftRight(a, b, MapWidth(5))
          case _                     => false
        }
        val hasMiddle = directives.exists {
          case Neighbour(a, b) => a.value == 6 && b.value == 7
          case _               => false
        }
        expect(hasVWrap && !hasLeftRight && hasMiddle)
      }
  }

  test("isTopBottom treats province ids as 1-indexed") {
    IO.pure(
      expect(
        !WrapSeverService.isTopBottom(
          ProvinceId(6),
          ProvinceId(1),
          MapWidth(5),
          MapHeight(12)
        )
      )
    )
  }

  test("isLeftRight only matches provinces on the same row") {
    IO.pure(expect(!WrapSeverService.isLeftRight(ProvinceId(5), ProvinceId(6), MapWidth(5))))
  }
