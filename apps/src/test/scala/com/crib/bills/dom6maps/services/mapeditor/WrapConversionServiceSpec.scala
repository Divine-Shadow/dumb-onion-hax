package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.instances.either.*
import cats.syntax.all.*
import fs2.io.file.Path
import weaver.SimpleIOSuite
import model.map.{
  HWrapAround,
  MapFileParser,
  MapState,
  MapWidth,
  MapHeight,
  NoWrapAround,
  VWrapAround,
  WrapAround,
  WrapState
}
import WrapSeverService.{isTopBottom, isLeftRight}
import model.map.MapDirectiveCodecs.given
import model.map.MapDirectiveCodecs.Encoder

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
      index = res.provinceLocations
      hasTopBottom = res.adjacency.exists((a, b) => isTopBottom(a, b, index, h))
      directives = Encoder[MapState].encode(res)
    yield expect.all(
      res.wrap == WrapState.HorizontalWrap,
      !hasTopBottom,
      directives.contains(HWrapAround)
    )
  }

  test("convert to vwrap") {
    val service = new WrapConversionServiceImpl[IO]
    for
      (state, w, h) <- load
      resEC <- service.convert[EC](state, WrapChoice.VWrap)
      res <- IO.fromEither(resEC)
      index = res.provinceLocations
      hasLeftRight = res.adjacency.exists((a, b) => isLeftRight(a, b, index, w))
      directives = Encoder[MapState].encode(res)
    yield expect.all(
      res.wrap == WrapState.VerticalWrap,
      !hasLeftRight,
      directives.contains(VWrapAround)
    )
  }

  test("convert to full-wrap") {
    val service = new WrapConversionServiceImpl[IO]
    for
      (state, _, _) <- load
      resEC <- service.convert[EC](state, WrapChoice.FullWrap)
      res <- IO.fromEither(resEC)
      directives = Encoder[MapState].encode(res)
    yield expect.all(
      res.wrap == WrapState.FullWrap,
      res.adjacency == state.adjacency,
      directives.contains(WrapAround)
    )
  }

  test("convert to no-wrap") {
    val service = new WrapConversionServiceImpl[IO]
    for
      (state, w, h) <- load
      resEC <- service.convert[EC](state, WrapChoice.NoWrap)
      res <- IO.fromEither(resEC)
      index = res.provinceLocations
      hasTopBottom = res.adjacency.exists((a, b) => isTopBottom(a, b, index, h))
      hasLeftRight = res.adjacency.exists((a, b) => isLeftRight(a, b, index, w))
      directives = Encoder[MapState].encode(res)
    yield expect.all(
      res.wrap == WrapState.NoWrap,
      !hasTopBottom,
      !hasLeftRight,
      directives.contains(NoWrapAround)
    )
  }

  test("convert no-wrap map to hwrap renders directive") {
    val service = new WrapConversionServiceImpl[IO]
    for
      resEC <- service.convert[EC](MapState.empty, WrapChoice.HWrap)
      res <- IO.fromEither(resEC)
      directives = Encoder[MapState].encode(res)
    yield expect.all(
      res.wrap == WrapState.HorizontalWrap,
      directives.contains(HWrapAround)
    )
  }

  test("convert no-wrap map to vwrap renders directive") {
    val service = new WrapConversionServiceImpl[IO]
    for
      resEC <- service.convert[EC](MapState.empty, WrapChoice.VWrap)
      res <- IO.fromEither(resEC)
      directives = Encoder[MapState].encode(res)
    yield expect.all(
      res.wrap == WrapState.VerticalWrap,
      directives.contains(VWrapAround)
    )
  }

  test("convert no-wrap map to full-wrap renders directive") {
    val service = new WrapConversionServiceImpl[IO]
    for
      resEC <- service.convert[EC](MapState.empty, WrapChoice.FullWrap)
      res <- IO.fromEither(resEC)
      directives = Encoder[MapState].encode(res)
    yield expect.all(
      res.wrap == WrapState.FullWrap,
      directives.contains(WrapAround)
    )
  }
