package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import fs2.io.file.Path
import model.ProvinceId
import model.map.{
  MapFileParser,
  MapWidth,
  MapHeight,
  MapState,
  WrapState
}
import weaver.SimpleIOSuite

object WrapSeverServiceSpec extends SimpleIOSuite:
  test("adds hwrap and removes top-bottom neighbours") {
    for
      state <- MapState.fromDirectives(MapFileParser.parseFile[IO](Path("data/five-by-twelve.map")))
      result = WrapSeverService.severVertically(state)
      size <- IO.fromOption(result.size)(new NoSuchElementException("#mapsize not found"))
      w = MapWidth(size.value)
      h = MapHeight(size.value)
      hasTopBottom = result.adjacency.exists((a, b) => WrapSeverService.isTopBottom(a, b, w, h))
      hasMiddle = result.adjacency.exists { case (a, b) => a.value == 6 && b.value == 7 }
    yield expect(result.wrap == WrapState.HorizontalWrap && !hasTopBottom && hasMiddle)
  }

  test("adds vwrap and removes left-right neighbours") {
    for
      state <- MapState.fromDirectives(MapFileParser.parseFile[IO](Path("data/five-by-twelve-horizontal.map")))
      result = WrapSeverService.severHorizontally(state)
      size <- IO.fromOption(result.size)(new NoSuchElementException("#mapsize not found"))
      w = MapWidth(size.value)
      hasLeftRight = result.adjacency.exists((a, b) => WrapSeverService.isLeftRight(a, b, w))
      hasMiddle = result.adjacency.exists { case (a, b) => a.value == 6 && b.value == 7 }
    yield expect(result.wrap == WrapState.VerticalWrap && !hasLeftRight && hasMiddle)
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
