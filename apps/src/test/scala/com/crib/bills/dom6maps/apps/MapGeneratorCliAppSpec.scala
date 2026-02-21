package com.crib.bills.dom6maps
package apps

import cats.effect.IO
import model.map.WrapState
import model.map.generation.TerrainImageVariantPolicy
import weaver.SimpleIOSuite

object MapGeneratorCliAppSpec extends SimpleIOSuite:
  test("parses wrap state aliases") {
    val parsedHorizontal = MapGeneratorCliApp.parseWrapStateForTest("horizontal")
    val parsedNoWrap = MapGeneratorCliApp.parseWrapStateForTest("no-wrap")

    IO(expect.all(
      parsedHorizontal == Right(WrapState.HorizontalWrap),
      parsedNoWrap == Right(WrapState.NoWrap)
    ))
  }

  test("parses terrain image policy aliases") {
    val parsedBase = MapGeneratorCliApp.parseTerrainImagePolicyForTest("base")
    val parsedFull = MapGeneratorCliApp.parseTerrainImagePolicyForTest("full-terrain-set")

    IO(expect.all(
      parsedBase == Right(TerrainImageVariantPolicy.BaseOnly),
      parsedFull == Right(TerrainImageVariantPolicy.FullTerrainSet)
    ))
  }
