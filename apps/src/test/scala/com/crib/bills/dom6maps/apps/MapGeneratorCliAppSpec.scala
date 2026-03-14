package com.crib.bills.dom6maps
package apps

import cats.effect.IO
import apps.services.mapeditor.{MapGeneratorAllocationConfig, MapGeneratorConnectionBordersConfig, MapGeneratorGeometryConfig, MapGeneratorPlayerStartConfig, MapGeneratorTerrainDistributionConfig, MapGeneratorThroneDefenderSetPieceConfig, MapGeneratorThroneDefenderUnitConfig, MapGeneratorThronePlacementConfig, MapGeneratorThronesConfig, MapGeneratorUndergroundConfig, MapScenarioLayersConfig, MapScenarioPlayerConfig, MapScenarioPointConfig, ThroneGenerationMode, UndergroundGenerationMode}
import model.{Nation, ProvinceId}
import model.map.{MapDimensions, ThroneLevel, WrapState}
import model.map.generation.{AllocationGenerationPolicy, BorderSpecGenerationPolicy, PlayerStartAssignment, TerrainImageVariantPolicy}
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

  test("parses configured throne overrides by province id") {
    val parsed = MapGeneratorCliApp.parseThroneGenerationModeForTest(
      MapGeneratorThronesConfig.disabled.copy(
        mode = "configured",
        surfaceOverrides = Vector(
          MapGeneratorThronePlacementConfig(
            provinceId = Some(41),
            x = None,
            y = None,
            level = Some(1),
            id = None
          )
        ),
        undergroundOverrides = Vector.empty
      ),
      UndergroundGenerationMode.MirroredPlane("The Underworld", true)
    )

    IO(expect(parsed.isRight))
  }

  test("parses player start assignments by nation id") {
    val parsed = MapGeneratorCliApp.parsePlayerStartAssignmentsForTest(
      Vector(
        MapGeneratorPlayerStartConfig(
          nationId = 56,
          surfaceStartProvinceId = Some(9),
          undergroundStartProvinceId = None
        )
      )
    )

    IO(expect(parsed == Right(Vector(PlayerStartAssignment(Nation.Pythium_Middle, Some(ProvinceId(9)), None)))))
  }

  test("rejects unknown nation id in player starts") {
    val parsed = MapGeneratorCliApp.parsePlayerStartAssignmentsForTest(
      Vector(
        MapGeneratorPlayerStartConfig(
          nationId = 9999,
          surfaceStartProvinceId = Some(9),
          undergroundStartProvinceId = None
        )
      )
    )

    IO(expect(parsed.isLeft))
  }

  test("parses disabled allocation policy") {
    val parsed = MapGeneratorCliApp.parseAllocationGenerationPolicyForTest(
      MapGeneratorAllocationConfig.disabled,
      parseDefaultTerrainDistribution()
    )

    IO(expect(parsed == Right(AllocationGenerationPolicy.Disabled)))
  }

  test("parses map dimensions from square map-size") {
    val parsed = MapGeneratorCliApp.parseMapDimensionsForTest(
      MapGeneratorGeometryConfig(
        mapSize = Some(8),
        xSize = None,
        ySize = None,
        provinceCount = 80,
        wrapState = "full",
        seed = Some(42L),
        seaRatio = 0.3,
        noiseScale = 1.0,
        gridJitter = 0.5
      )
    )

    IO(expect(parsed.exists(dimensions => dimensions.width.value == 8 && dimensions.height.value == 8)))
  }

  test("parses scenario player start locations") {
    val parsed = MapGeneratorCliApp.parseScenarioPlayerStartLocationsForTest(
      Vector(
        MapScenarioPlayerConfig(
          nationId = 56,
          profileEnvironment = Some("surface"),
          surfaceStart = Some(MapScenarioPointConfig(3, 2)),
          undergroundStart = None
        )
      )
    )

    IO(expect(parsed.isRight))
  }

  test("parses scenario underground mode with tunnels disabled") {
    val parsed = MapGeneratorCliApp.parseScenarioUndergroundModeForTest(
      MapScenarioLayersConfig(
        surfaceEnabled = true,
        undergroundEnabled = true,
        undergroundPlaneName = Some("The Underworld"),
        connectEveryProvinceWithTunnel = Some(false)
      )
    )

    IO(expect(parsed == Right(UndergroundGenerationMode.MirroredPlane("The Underworld", false))))
  }

  test("parses scenario underground mode with default tunnel policy enabled") {
    val parsed = MapGeneratorCliApp.parseScenarioUndergroundModeForTest(
      MapScenarioLayersConfig(
        surfaceEnabled = true,
        undergroundEnabled = true,
        undergroundPlaneName = Some("The Underworld"),
        connectEveryProvinceWithTunnel = None
      )
    )

    IO(expect(parsed == Right(UndergroundGenerationMode.MirroredPlane("The Underworld", true))))
  }

  test("derives scenario province count from dimensions") {
    val parsed = for
      mapDimensions <- MapDimensions.from(18, 5)
      provinceCount <- MapGeneratorCliApp.deriveScenarioProvinceCountForTest(mapDimensions)
    yield provinceCount

    IO(expect(parsed == Right(90)))
  }

  private def parseDefaultTerrainDistribution() =
    MapGeneratorCliApp.parseTerrainDistributionPolicyForTest(MapGeneratorTerrainDistributionConfig.default).toOption.get
