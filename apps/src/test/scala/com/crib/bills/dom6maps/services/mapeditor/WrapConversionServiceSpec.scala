package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.instances.either.*
import cats.syntax.all.*
import fs2.io.file.Path
import weaver.SimpleIOSuite
import model.map.{
  MapFileParser,
  MapState,
  MapWidth,
  MapHeight,
  WrapState
}
import WrapSeverService.{isTopBottom, isLeftRight}

object WrapConversionServiceSpec extends SimpleIOSuite:
  type EC[A] = Either[Throwable, A]

  private def load: IO[(MapState, MapWidth, MapHeight)] =
    for
      state <- MapState.fromDirectives(MapFileParser.parseFile[IO](Path("data/five-by-twelve.map")))
      size <- IO.fromOption(state.size)(new NoSuchElementException("#mapsize not found"))
      w = MapWidth(size.value)
      h = MapHeight(size.value)
    yield (state, w, h)

  test("convert to hwrap") {
    val service = new WrapConversionServiceImpl[IO]
    for
      (state, w, h) <- load
      resEC <- service.convert[EC](state, WrapChoice.HWrap)
      res <- IO.fromEither(resEC)
      hasTopBottom = res.adjacency.exists((a, b) => isTopBottom(a, b, w, h))
    yield expect.all(
      res.wrap == WrapState.HorizontalWrap,
      !hasTopBottom
    )
  }

  test("convert to vwrap") {
    val service = new WrapConversionServiceImpl[IO]
    for
      (state, w, h) <- load
      resEC <- service.convert[EC](state, WrapChoice.VWrap)
      res <- IO.fromEither(resEC)
      hasLeftRight = res.adjacency.exists((a, b) => isLeftRight(a, b, w))
    yield expect.all(
      res.wrap == WrapState.VerticalWrap,
      !hasLeftRight
    )
  }

  test("convert to no-wrap") {
    val service = new WrapConversionServiceImpl[IO]
    for
      (state, w, h) <- load
      resEC <- service.convert[EC](state, WrapChoice.NoWrap)
      res <- IO.fromEither(resEC)
      hasTopBottom = res.adjacency.exists((a, b) => isTopBottom(a, b, w, h))
      hasLeftRight = res.adjacency.exists((a, b) => isLeftRight(a, b, w))
    yield expect.all(
      res.wrap == WrapState.NoWrap,
      !hasTopBottom,
      !hasLeftRight
    )
  }
