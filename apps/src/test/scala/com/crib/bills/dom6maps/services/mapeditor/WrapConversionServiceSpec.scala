package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.instances.either.*
import cats.syntax.all.*
import fs2.io.file.Path
import weaver.SimpleIOSuite
import model.map.{
  MapDirective,
  MapFileParser,
  MapSizePixels,
  MapWidth,
  MapHeight,
  Neighbour,
  NeighbourSpec,
  HWrapAround,
  VWrapAround,
  NoWrapAround,
  WrapAround
}
import WrapSeverService.{isTopBottom, isLeftRight}

object WrapConversionServiceSpec extends SimpleIOSuite:
  type EC[A] = Either[Throwable, A]

  private def load: IO[(Vector[MapDirective], MapWidth, MapHeight)] =
    for
      directives <- MapFileParser.parseFile[IO](Path("data/five-by-twelve.map")).compile.toVector
      sizePixels <- IO.fromOption(directives.collectFirst { case m: MapSizePixels => m })(
        new NoSuchElementException("#mapsize not found")
      )
      psize = sizePixels.toProvinceSize
    yield (directives, psize.width, psize.height)

  test("convert to hwrap") {
    val service = new WrapConversionServiceImpl[IO]
    for
      (directives, w, h) <- load
      resEC <- service.convert[EC](directives, w, h, WrapChoice.HWrap)
      res <- IO.fromEither(resEC)
      hasTopBottom = res.exists {
        case Neighbour(a, b)       => isTopBottom(a, b, w, h)
        case NeighbourSpec(a, b, _) => isTopBottom(a, b, w, h)
        case _                     => false
      }
    yield expect.all(
      res.contains(HWrapAround),
      !res.exists(d => d == WrapAround || d == VWrapAround || d == NoWrapAround),
      !hasTopBottom
    )
  }

  test("convert to vwrap") {
    val service = new WrapConversionServiceImpl[IO]
    for
      (directives, w, h) <- load
      resEC <- service.convert[EC](directives, w, h, WrapChoice.VWrap)
      res <- IO.fromEither(resEC)
      hasLeftRight = res.exists {
        case Neighbour(a, b)       => isLeftRight(a, b, w)
        case NeighbourSpec(a, b, _) => isLeftRight(a, b, w)
        case _                     => false
      }
    yield expect.all(
      res.contains(VWrapAround),
      !res.exists(d => d == WrapAround || d == HWrapAround || d == NoWrapAround),
      !hasLeftRight
    )
  }

  test("convert to no-wrap") {
    val service = new WrapConversionServiceImpl[IO]
    for
      (directives, w, h) <- load
      resEC <- service.convert[EC](directives, w, h, WrapChoice.NoWrap)
      res <- IO.fromEither(resEC)
      hasTopBottom = res.exists {
        case Neighbour(a, b)       => isTopBottom(a, b, w, h)
        case NeighbourSpec(a, b, _) => isTopBottom(a, b, w, h)
        case _                     => false
      }
      hasLeftRight = res.exists {
        case Neighbour(a, b)       => isLeftRight(a, b, w)
        case NeighbourSpec(a, b, _) => isLeftRight(a, b, w)
        case _                     => false
      }
    yield expect.all(
      res.contains(NoWrapAround),
      !res.exists(d => d == WrapAround || d == HWrapAround || d == VWrapAround),
      !hasTopBottom,
      !hasLeftRight
    )
  }
