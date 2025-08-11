package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.syntax.all.*
import weaver.SimpleIOSuite
import model.ProvinceId
import model.{TerrainFlag, TerrainMask}
import model.map.{MapState, Terrain, ThronePlacement, ThroneLevel}

object ThronePlacementServiceSpec extends SimpleIOSuite:
  test("update sets and clears throne flag") {
    val service = new ThronePlacementServiceImpl[IO]
    val state = MapState.empty.copy(terrains = Vector(
      Terrain(ProvinceId(1), 0),
      Terrain(ProvinceId(2), TerrainFlag.Throne.mask)
    ))
    val placements = Vector(ThronePlacement(ProvinceId(1), ThroneLevel(1)))
    for
      res <- service.update(state, placements)
      mask1 = res.terrains.collectFirst { case Terrain(ProvinceId(1), m) => TerrainMask(m) }.get
      mask2 = res.terrains.collectFirst { case Terrain(ProvinceId(2), m) => TerrainMask(m) }.get
    yield expect.all(
      mask1.hasFlag(TerrainFlag.Throne),
      !mask2.hasFlag(TerrainFlag.Throne)
    )
  }
