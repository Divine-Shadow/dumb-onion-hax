package com.crib.bills.dom6maps
package apps

import cats.effect.IO
import fs2.io.file.{Files, Path}
import com.crib.bills.dom6maps.model.map.{HWrapAround, MapFileParser, Neighbour, NoWrapAround}
import WrapSever.isTopBottom
import weaver.SimpleIOSuite

object WrapSeverAppSpec extends SimpleIOSuite:

  test("adds hwrap and removes top-bottom neighbours") {
    MapFileParser
      .parseFile[IO](Path("data/five-by-twelve.map"))
      .compile
      .toVector
      .map(WrapSever.process)
      .map { directives =>
        val hasHWrap = directives.contains(HWrapAround)
        val hasNoWrap = directives.contains(NoWrapAround)
        val hasTopBottom = directives.exists {
          case Neighbour(a, b) => isTopBottom(a, b)
          case _               => false
        }
        val hasMiddle = directives.exists {
          case Neighbour(a, b) => a.value == 6 && b.value == 7
          case _               => false
        }
        expect(hasHWrap && !hasNoWrap && !hasTopBottom && hasMiddle)
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
          case Neighbour(a, b) => isTopBottom(a, b)
          case _               => false
        }
        expect(hasHWrap && !hasNoWrap && !hasTopBottom)
      }
  }
