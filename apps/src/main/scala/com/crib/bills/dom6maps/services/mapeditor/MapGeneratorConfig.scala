package com.crib.bills.dom6maps
package apps.services.mapeditor

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import java.nio.file.Path as NioPath

final case class MapGeneratorConfig(
    output: NioPath,
    mapName: String,
    mapTitle: String,
    mapDescription: Option[String],
    geometry: MapGeneratorGeometryConfig,
    terrainDistribution: Option[MapGeneratorTerrainDistributionConfig],
    terrainImages: MapGeneratorTerrainImagesConfig,
    connectionBorders: Option[MapGeneratorConnectionBordersConfig],
    underground: Option[MapGeneratorUndergroundConfig],
    thrones: Option[MapGeneratorThronesConfig],
    nations: Option[MapGeneratorNationsConfig]
) derives ConfigReader

final case class MapGeneratorNationsConfig(
    starts: Vector[MapGeneratorNationStartConfig]
) derives ConfigReader

final case class MapGeneratorNationStartConfig(
    nationId: Int,
    surfaceStartProvinceId: Int
) derives ConfigReader

final case class MapGeneratorGeometryConfig(
    mapSize: Int,
    provinceCount: Int,
    wrapState: String,
    seed: Option[Long],
    seaRatio: Double,
    noiseScale: Double,
    gridJitter: Double
) derives ConfigReader

final case class MapGeneratorTerrainImagesConfig(
    policy: String
) derives ConfigReader

final case class MapGeneratorTerrainDistributionConfig(
    swampPercent: Double,
    wastePercent: Double,
    highlandPercent: Double,
    forestPercent: Double,
    farmPercent: Double,
    extraLakePercent: Double
) derives ConfigReader

object MapGeneratorTerrainDistributionConfig:
  val default: MapGeneratorTerrainDistributionConfig =
    MapGeneratorTerrainDistributionConfig(
      swampPercent = 0.16,
      wastePercent = 0.18,
      highlandPercent = 0.16,
      forestPercent = 0.18,
      farmPercent = 0.16,
      extraLakePercent = 0.0
    )

final case class MapGeneratorUndergroundConfig(
    enabled: Boolean,
    planeName: String,
    connectEveryProvinceWithTunnel: Boolean
) derives ConfigReader

object MapGeneratorUndergroundConfig:
  val disabled: MapGeneratorUndergroundConfig =
    MapGeneratorUndergroundConfig(
      enabled = false,
      planeName = "The Underworld",
      connectEveryProvinceWithTunnel = true
    )

final case class MapGeneratorThronesConfig(
    mode: String,
    randomCornerLevel: Int,
    includeSurface: Boolean,
    includeUnderground: Boolean,
    surfaceOverrides: Vector[MapGeneratorThronePlacementConfig],
    undergroundOverrides: Vector[MapGeneratorThronePlacementConfig],
    defenderSetPieces: Option[Vector[MapGeneratorThroneDefenderSetPieceConfig]]
) derives ConfigReader

object MapGeneratorThronesConfig:
  val disabled: MapGeneratorThronesConfig =
    MapGeneratorThronesConfig(
      mode = "disabled",
      randomCornerLevel = 1,
      includeSurface = true,
      includeUnderground = true,
      surfaceOverrides = Vector.empty,
      undergroundOverrides = Vector.empty,
      defenderSetPieces = Some(Vector.empty)
    )

final case class MapGeneratorThroneDefenderUnitConfig(
    count: Int,
    unitType: String
) derives ConfigReader

final case class MapGeneratorThroneDefenderSetPieceConfig(
    level: Int,
    commanderType: String,
    units: Vector[MapGeneratorThroneDefenderUnitConfig]
) derives ConfigReader

final case class MapGeneratorThronePlacementConfig(
    provinceId: Option[Int],
    x: Option[Int],
    y: Option[Int],
    level: Option[Int],
    id: Option[Int]
) derives ConfigReader

final case class MapGeneratorConnectionBordersConfig(
    nonHighlandRiverPercent: Double,
    nonHighlandRoadPercent: Double,
    nonHighlandBridgedRiverPercent: Double,
    highlandMountainPercent: Double,
    highlandMountainPassPercent: Double,
    highlandRoadPercent: Double
) derives ConfigReader

object MapGeneratorConnectionBordersConfig:
  val default: MapGeneratorConnectionBordersConfig =
    MapGeneratorConnectionBordersConfig(
      nonHighlandRiverPercent = 0.20,
      nonHighlandRoadPercent = 0.20,
      nonHighlandBridgedRiverPercent = 0.0,
      highlandMountainPercent = 0.30,
      highlandMountainPassPercent = 0.20,
      highlandRoadPercent = 0.15
    )
