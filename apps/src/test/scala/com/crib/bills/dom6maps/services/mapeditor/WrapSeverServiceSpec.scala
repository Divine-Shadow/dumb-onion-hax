package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import fs2.io.file.Path
import model.ProvinceId
import model.map.{
  MapFileParser,
  MapSize,
  MapWidth,
  MapHeight,
  MapState,
  ProvinceLocations,
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
      index = result.provinceLocations
      hasTopBottom = result.adjacency.exists((a, b) => WrapSeverService.isTopBottom(a, b, index, h))
      hasMiddle = result.adjacency.exists { case (a, b) => a.value == 6 && b.value == 7 }
    yield expect(result.wrap == WrapState.HorizontalWrap && !hasTopBottom && hasMiddle)
  }

  test("adds vwrap and removes left-right neighbours") {
    for
      state <- MapState.fromDirectives(MapFileParser.parseFile[IO](Path("data/five-by-twelve-horizontal.map")))
      result = WrapSeverService.severHorizontally(state)
      size <- IO.fromOption(result.size)(new NoSuchElementException("#mapsize not found"))
      w = MapWidth(size.value)
      index = result.provinceLocations
      hasLeftRight = result.adjacency.exists((a, b) => WrapSeverService.isLeftRight(a, b, index, w))
      hasMiddle = result.adjacency.exists { case (a, b) => a.value == 6 && b.value == 7 }
    yield expect(result.wrap == WrapState.VerticalWrap && !hasLeftRight && hasMiddle)
  }

  test("isTopBottom treats province ids as 1-indexed") {
    IO.pure(
      expect(
        !WrapSeverService.isTopBottom(
          ProvinceId(6),
          ProvinceId(1),
          ProvinceLocations.fromProvinceIdMap(
            Map(
              ProvinceId(6) -> model.map.ProvinceLocation(model.map.XCell(0), model.map.YCell(1)),
              ProvinceId(1) -> model.map.ProvinceLocation(model.map.XCell(0), model.map.YCell(0))
            )
          ),
          MapHeight(12)
        )
      )
    )
  }

  test("isLeftRight matches provinces across different rows") {
    IO.pure(
      expect(
        WrapSeverService.isLeftRight(
          ProvinceId(5),
          ProvinceId(6),
          ProvinceLocations.fromProvinceIdMap(
            Map(
              ProvinceId(5) -> model.map.ProvinceLocation(model.map.XCell(4), model.map.YCell(0)),
              ProvinceId(6) -> model.map.ProvinceLocation(model.map.XCell(0), model.map.YCell(1))
            )
          ),
          MapWidth(5)
        )
      )
    )
  }

  test("severHorizontally removes diagonal left-right neighbours") {
    val size = MapSize.from(5).toOption
    val locations = ProvinceLocations.fromProvinceIdMap(
      Map(
        ProvinceId(5) -> model.map.ProvinceLocation(model.map.XCell(4), model.map.YCell(0)),
        ProvinceId(6) -> model.map.ProvinceLocation(model.map.XCell(0), model.map.YCell(1)),
        ProvinceId(7) -> model.map.ProvinceLocation(model.map.XCell(1), model.map.YCell(1))
      )
    )
    val state = MapState.empty.copy(
      size = size,
      adjacency = Vector((ProvinceId(5), ProvinceId(6)), (ProvinceId(6), ProvinceId(7))),
      wrap = WrapState.FullWrap,
      provinceLocations = locations
    )
    val result = WrapSeverService.severHorizontally(state)
    val hasDiagonal = result.adjacency.contains((ProvinceId(5), ProvinceId(6)))
    val hasMiddle = result.adjacency.contains((ProvinceId(6), ProvinceId(7)))
    IO.pure(expect(result.wrap == WrapState.VerticalWrap && !hasDiagonal && hasMiddle))
  }
