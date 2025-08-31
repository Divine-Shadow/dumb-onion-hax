package com.crib.bills.dom6maps
package model.map

import cats.effect.IO
import cats.syntax.all.*
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object ProvinceLocationGridPrinterSpec extends SimpleIOSuite with Checkers:
  import Arbitraries.given

  test("render places provinces at expected coordinates") {
    forall { (g: Geometry) =>
      val buffer = scala.collection.mutable.ArrayBuffer.empty[String]
      val printer = new model.Printer[IO]:
        def println(value: String): IO[Unit] = IO(buffer += value)

      val locations = ProvinceLocations.fromLocationMap(g.locations)
      ProvinceLocationGridPrinter.render(g.size, locations, printer).map { _ =>
        val expected = (0 until g.size.value).toVector.map { y =>
          (0 until g.size.value)
            .map { x =>
              val loc = ProvinceLocation(XCell(x), YCell(y))
              g.locations.get(loc).fold(".")(_.value.toString)
            }
            .mkString(" ")
        }
        expect.same(expected, buffer.toVector)
      }
    }
  }
