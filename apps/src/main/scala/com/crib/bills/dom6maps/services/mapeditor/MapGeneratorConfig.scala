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
    terrainImages: MapGeneratorTerrainImagesConfig,
    connectionBorders: Option[MapGeneratorConnectionBordersConfig]
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
