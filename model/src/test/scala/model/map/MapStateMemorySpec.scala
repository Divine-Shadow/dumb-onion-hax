package com.crib.bills.dom6maps
package model.map

import cats.effect.IO
import fs2.Stream
import weaver.SimpleIOSuite
import model.ProvinceId

object MapStateMemorySpec extends SimpleIOSuite:
  test("MapState.fromDirectivesWithPassThrough streams without buffering"):
    val stream = Stream.constant(WrapAround).covary[IO].take(10000000L)
    MapState.fromDirectivesWithPassThrough[IO](stream).map { _ =>
      expect(true)
    }

  test("ProvinceLocationService streams #pb directives without buffering"):
    val stream = Stream.constant(Pb(0, 0, 1, ProvinceId(1))).covary[IO].take(1000000L)
    ProvinceLocationService.derive[IO](stream).map { _ =>
      expect(true)
    }
