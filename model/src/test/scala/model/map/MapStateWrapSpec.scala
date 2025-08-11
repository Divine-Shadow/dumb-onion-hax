package com.crib.bills.dom6maps
package model.map

import cats.effect.IO
import fs2.Stream
import weaver.SimpleIOSuite

object MapStateWrapSpec extends SimpleIOSuite:
  test("wrap around sets full wrap"):
    MapState.fromDirectives(Stream.emits(Vector(WrapAround)).covary[IO]).map { state =>
      expect.same(WrapState.FullWrap, state.wrap)
    }

  test("vertical wrap around sets vertical wrap"):
    MapState.fromDirectives(Stream.emits(Vector(VWrapAround)).covary[IO]).map { state =>
      expect.same(WrapState.VerticalWrap, state.wrap)
    }

  test("horizontal wrap around sets horizontal wrap"):
    MapState.fromDirectives(Stream.emits(Vector(HWrapAround)).covary[IO]).map { state =>
      expect.same(WrapState.HorizontalWrap, state.wrap)
    }

  test("no wrap around resets wrap state"):
    val directives = Vector(WrapAround, NoWrapAround)
    MapState.fromDirectives(Stream.emits(directives).covary[IO]).map { state =>
      expect.same(WrapState.NoWrap, state.wrap)
    }
