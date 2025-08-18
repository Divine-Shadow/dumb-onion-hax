package com.crib.bills.dom6maps
package model.map

import cats.effect.IO
import fs2.Stream
import model.ProvinceId
import weaver.SimpleIOSuite

object ProvinceLocationWrapSpec extends SimpleIOSuite:
  private val size = MapSizePixels(MapWidthPixels(1024), MapHeightPixels(160))
  private val id   = ProvinceId(1)
  private val left  = Pb(0, 0, 256, id)
  private val right = Pb(768, 0, 256, id)
  private def derive(dirs: List[MapDirective]) =
    ProvinceLocationService.derive(Stream.emits(dirs).covary[IO])

  test("no-wrap directive still applies wrap-around protection"):
    val base = List(size, left, right)
    for
      wrap   <- derive(WrapAround :: base)
      nowrap <- derive(NoWrapAround :: base)
    yield
      val wrapLoc   = wrap(id)
      val noWrapLoc = nowrap(id)
      expect.same(wrapLoc, noWrapLoc) &&
      expect.same(ProvinceLocation(XCell(3), YCell(0)), wrapLoc)
