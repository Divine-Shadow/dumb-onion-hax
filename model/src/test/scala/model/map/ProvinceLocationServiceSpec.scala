package com.crib.bills.dom6maps
package model.map

import cats.effect.IO
import fs2.{Stream, text}
import fs2.io.file.{Files, Path}
import model.ProvinceId
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

  test("location index covers every grid cell uniquely"):
    val path = Path("data/duel-map-example.map")
    for
      directives <- MapFileParser.parseFile[IO](path).compile.toVector
      index      <- ProvinceLocationService.deriveLocationIndex(Stream.emits(directives).covary[IO])
    yield
      val sizeDirective = directives.collectFirst { case m: MapSizePixels => m }.get
      val widthCells    = sizeDirective.toProvinceSize.width.value
      val heightCells   = sizeDirective.toProvinceSize.height.value
      val expectedCount = widthCells * heightCells
      val allCoordinatesCovered =
        (0 until widthCells).forall { x =>
          (0 until heightCells).forall { y =>
            index.contains(ProvinceLocation(XCell(x), YCell(y)))
          }
        }
      val uniqueProvinces = index.values.toSet.size == expectedCount
      expect(allCoordinatesCovered && index.size == expectedCount && uniqueProvinces)

  private def parseGrid(lines: Vector[String]): Map[ProvinceId, ProvinceLocation] =
    lines.collect {
      case line if line.startsWith("| y=") =>
        val cells = line.split("\\|").toVector.map(_.trim).filter(_.nonEmpty)
        val y     = cells.head.stripPrefix("y=").toInt
        cells.tail.zipWithIndex.map { case (id, x) =>
          ProvinceId(id.toInt) -> ProvinceLocation(XCell(x), YCell(y))
        }
    }.flatten.toMap

  test("Science6 map provinces align with documented grid"):
    val mapPath  = Path("data/Science6.map")
    val gridPath = Path("data/Science6_province_grid.md")
    for
      directives <- MapFileParser.parseFile[IO](mapPath).compile.toVector
      coords     <- ProvinceLocationService.derive(Stream.emits(directives).covary[IO])
      gridLines  <- Files[IO].readAll(gridPath).through(text.utf8.decode).through(text.lines).compile.toVector
    yield
      val expected      = parseGrid(gridLines)
      val mismatches    = expected.collect { case (id, loc) if coords.get(id) != Some(loc) => id -> coords(id) }
      val expectedMismatches = Map(
        ProvinceId(5)  -> ProvinceLocation(XCell(1), YCell(1)),
        ProvinceId(20) -> ProvinceLocation(XCell(3), YCell(3)),
        ProvinceId(25) -> ProvinceLocation(XCell(3), YCell(3))
      )
      expect.same(expectedMismatches, mismatches)
