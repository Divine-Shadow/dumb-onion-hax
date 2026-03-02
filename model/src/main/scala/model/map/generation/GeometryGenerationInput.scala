package com.crib.bills.dom6maps
package model.map.generation

import model.map.{MapSize, WrapState}

final case class GeometryGenerationInput(
    mapSize: MapSize,
    provinceCount: Int,
    wrapState: WrapState,
    seed: Long,
    seaRatio: Double,
    noiseScale: Double,
    gridJitter: Double,
    terrainDistributionPolicy: TerrainDistributionPolicy = TerrainDistributionPolicy.default
)
