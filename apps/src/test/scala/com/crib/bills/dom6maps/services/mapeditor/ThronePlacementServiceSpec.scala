package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import fs2.Stream
import weaver.SimpleIOSuite
import model.ProvinceId
import model.{TerrainFlag, TerrainMask}
import model.map.{MapDirective, Terrain, ThronePlacement, ThroneLevel}

object ThronePlacementServiceSpec extends SimpleIOSuite:
  test("pipe sets and clears throne flag") {
    val service = new ThronePlacementServiceImpl[IO]
    val input = Stream.emits[IO, MapDirective](Vector(
      Terrain(ProvinceId(1), 0),
      Terrain(ProvinceId(2), TerrainFlag.Throne.mask)
    ))
    val placements = Vector(ThronePlacement(ProvinceId(1), ThroneLevel(1)))
    for
      res <- input.through(service.pipe(placements)).compile.toVector
      mask1 = res.collectFirst { case Terrain(ProvinceId(1), m) => TerrainMask(m) }.get
      mask2 = res.collectFirst { case Terrain(ProvinceId(2), m) => TerrainMask(m) }.get
    yield expect.all(
      mask1.hasFlag(TerrainFlag.Throne),
      !mask2.hasFlag(TerrainFlag.Throne)
    )
  }
