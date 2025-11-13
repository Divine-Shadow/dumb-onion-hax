package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import weaver.SimpleIOSuite
import model.{ProvinceId, TerrainFlag, TerrainMask}
import model.map.{MapState, Terrain}

object MagicSiteFlagServiceSpec extends SimpleIOSuite:
  private val service = new MagicSiteFlagServiceImpl

  test("sets ManySites flag when enabled") {
    val state = MapState.empty.copy(terrains = Vector(Terrain(ProvinceId(1), TerrainFlag.Plains.mask)))
    val updated = service.apply(state, MagicSiteToggle.Enabled)
    IO(expect(TerrainMask(updated.terrains.head.mask).hasFlag(TerrainFlag.ManySites)))
  }

  test("leaves terrains untouched when disabled") {
    val mask = TerrainFlag.Plains.mask | TerrainFlag.ManySites.mask
    val state = MapState.empty.copy(terrains = Vector(Terrain(ProvinceId(1), mask)))
    val updated = service.apply(state, MagicSiteToggle.Disabled)
    val updatedMask = updated.terrains.head.mask
    IO(expect(updatedMask == mask))
  }
