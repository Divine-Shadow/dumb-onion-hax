package com.crib.bills.dom6maps
package model.map

import cats.effect.IO
import fs2.Stream
import fs2.io.file.Path
import weaver.SimpleIOSuite

object ProvinceLocationServiceSpec extends SimpleIOSuite:
  test("derived coordinates form a coherent map"):
    val path        = Path("data/duel-map-example.map")
    for
      directives <- MapFileParser.parseFile[IO](path).compile.toVector
      coords     <- ProvinceLocationService.derive(Stream.emits(directives).covary[IO])
    yield
      val sizeDirective = directives.collectFirst { case m: MapSizePixels => m }.get
      val widthCells    = sizeDirective.toProvinceSize.width.value
      val heightCells   = sizeDirective.toProvinceSize.height.value
      val uniqueCoords   = coords.values.toSet.size == coords.size
      val expectedCount  = widthCells * heightCells
      val withinBounds   = coords.values.forall { loc =>
        loc.x.value >= 0 && loc.x.value < widthCells &&
        loc.y.value >= 0 && loc.y.value < heightCells
      }
      expect(uniqueCoords && coords.size == expectedCount && withinBounds)
