package com.crib.bills.dom6maps
package apps

import cats.effect.IO
import apps.services.mapeditor.{MapGeneratorConnectionBordersConfig, MapGeneratorTerrainDistributionConfig, MapGeneratorThroneDefenderSetPieceConfig, MapGeneratorThroneDefenderUnitConfig, MapGeneratorThronesConfig, MapGeneratorUndergroundConfig, ThroneGenerationMode, UndergroundGenerationMode}
import model.map.{ThroneLevel, WrapState}
import model.map.generation.{BorderSpecGenerationPolicy, TerrainImageVariantPolicy}
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

  test("parses valid border generation policy config") {
    val parsed = MapGeneratorCliApp.parseBorderSpecGenerationPolicyForTest(
      MapGeneratorConnectionBordersConfig.default
    )

    IO(expect(parsed == Right(BorderSpecGenerationPolicy.default)))
  }

  test("rejects invalid border generation policy config") {
    val parsed = MapGeneratorCliApp.parseBorderSpecGenerationPolicyForTest(
      MapGeneratorConnectionBordersConfig.default.copy(
        nonHighlandRiverPercent = 0.8,
        highlandMountainPercent = 0.5
      )
    )

    IO(expect(parsed.isLeft))
  }

  test("parses valid terrain distribution policy config") {
    val parsed = MapGeneratorCliApp.parseTerrainDistributionPolicyForTest(
      MapGeneratorTerrainDistributionConfig(
        swampPercent = 0.07,
        wastePercent = 0.07,
        highlandPercent = 0.06,
        forestPercent = 0.17,
        farmPercent = 0.15,
        extraLakePercent = 0.07
      )
    )

    IO(expect(parsed.isRight))
  }

  test("rejects invalid terrain distribution policy config") {
    val parsed = MapGeneratorCliApp.parseTerrainDistributionPolicyForTest(
      MapGeneratorTerrainDistributionConfig(
        swampPercent = 0.25,
        wastePercent = 0.25,
        highlandPercent = 0.20,
        forestPercent = 0.20,
        farmPercent = 0.20,
        extraLakePercent = 0.20
      )
    )

    IO(expect(parsed.isLeft))
  }

  test("parses enabled underground generation mode") {
    val parsed = MapGeneratorCliApp.parseUndergroundGenerationModeForTest(
      MapGeneratorUndergroundConfig(
        enabled = true,
        planeName = "The Underworld",
        connectEveryProvinceWithTunnel = true
      )
    )

    IO(expect(parsed == Right(UndergroundGenerationMode.MirroredPlane("The Underworld", true))))
  }

  test("rejects enabled underground generation mode with blank plane name") {
    val parsed = MapGeneratorCliApp.parseUndergroundGenerationModeForTest(
      MapGeneratorUndergroundConfig(
        enabled = true,
        planeName = "   ",
        connectEveryProvinceWithTunnel = true
      )
    )

    IO(expect(parsed.isLeft))
  }

  test("parses random corner throne generation mode") {
    val parsed = MapGeneratorCliApp.parseThroneGenerationModeForTest(
      MapGeneratorThronesConfig.disabled.copy(
        mode = "random-corners",
        randomCornerLevel = 1,
        includeSurface = true,
        includeUnderground = true
      ),
      UndergroundGenerationMode.MirroredPlane("The Underworld", true)
    )

    IO(expect(parsed == Right(ThroneGenerationMode.RandomCorners(throneLevel = ThroneLevel(1), includeSurface = true, includeUnderground = true))))
  }

  test("rejects underground throne generation when underground map generation is disabled") {
    val parsed = MapGeneratorCliApp.parseThroneGenerationModeForTest(
      MapGeneratorThronesConfig.disabled.copy(
        mode = "random-corners",
        randomCornerLevel = 1,
        includeSurface = true,
        includeUnderground = true
      ),
      UndergroundGenerationMode.Disabled
    )

    IO(expect(parsed.isLeft))
  }

  test("parses throne defender set pieces") {
    val parsed = MapGeneratorCliApp.parseThroneDefenderSetPiecesForTest(
      Vector(
        MapGeneratorThroneDefenderSetPieceConfig(
          level = 1,
          commanderType = "Indie Commander",
          units = Vector(
            MapGeneratorThroneDefenderUnitConfig(20, "Militia"),
            MapGeneratorThroneDefenderUnitConfig(10, "Archer")
          )
        )
      )
    )

    IO(expect(parsed.isRight))
  }
