package com.crib.bills.dom6maps
package model.map.generation

import model.map.{MapDimensions, MapSize, WrapState}

final case class GeometryGenerationInput(
    mapDimensions: MapDimensions,
    provinceCount: Int,
    wrapState: WrapState,
    seed: Long,
    seaRatio: Double,
    noiseScale: Double,
    gridJitter: Double,
    terrainDistributionPolicy: TerrainDistributionPolicy = TerrainDistributionPolicy.default
)

object GeometryGenerationInput:
  def square(
      mapSize: MapSize,
      provinceCount: Int,
      wrapState: WrapState,
      seed: Long,
      seaRatio: Double,
      noiseScale: Double,
      gridJitter: Double,
      terrainDistributionPolicy: TerrainDistributionPolicy = TerrainDistributionPolicy.default
  ): GeometryGenerationInput =
    GeometryGenerationInput(
      mapDimensions = MapDimensions.square(mapSize),
      provinceCount = provinceCount,
      wrapState = wrapState,
      seed = seed,
      seaRatio = seaRatio,
      noiseScale = noiseScale,
      gridJitter = gridJitter,
      terrainDistributionPolicy = terrainDistributionPolicy
    )
