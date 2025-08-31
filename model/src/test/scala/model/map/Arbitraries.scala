package com.crib.bills.dom6maps
package model.map

import cats.Show
import org.scalacheck.{Arbitrary, Gen}
import model.ProvinceId

final case class Geometry(size: MapSize, locations: Map[ProvinceLocation, ProvinceId])

object Arbitraries:
  given Arbitrary[Geometry] =
    val gen = for
      n <- Gen.choose(0, 3).map(i => 2 * i + 1)
      start <- Gen.choose(1, 10000)
    yield
      val size = MapSize.from(n).toOption.get
      val locs = (0 until n * n).map { idx =>
        val x = idx % n
        val y = idx / n
        ProvinceLocation(XCell(x), YCell(y)) -> ProvinceId(start + idx)
      }.toMap
      Geometry(size, locs)
    Arbitrary(gen)

  given Show[Geometry] = Show.show(g => s"Geometry(size=${g.size.value})")
