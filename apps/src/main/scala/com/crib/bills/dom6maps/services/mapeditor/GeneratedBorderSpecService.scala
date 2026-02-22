package com.crib.bills.dom6maps
package apps.services.mapeditor

import model.{BorderFlag, ProvinceId, TerrainFlag, TerrainMask}
import model.map.Border
import model.map.generation.{BorderSpecGenerationPolicy, GeneratedGeometry}

trait GeneratedBorderSpecService:
  def populateBorders(
      generatedGeometry: GeneratedGeometry,
      seed: Long,
      borderSpecGenerationPolicy: BorderSpecGenerationPolicy
  ): GeneratedGeometry

class GeneratedBorderSpecServiceImpl extends GeneratedBorderSpecService:
  override def populateBorders(
      generatedGeometry: GeneratedGeometry,
      seed: Long,
      borderSpecGenerationPolicy: BorderSpecGenerationPolicy
  ): GeneratedGeometry =
    val existingByPair = generatedGeometry.borders.map(border => normalizedPair(border.a, border.b) -> border).toMap
    val terrainMaskByProvince = generatedGeometry.terrainByProvince.map(terrain => terrain.province -> terrain.mask).toMap

    val inferredBorders = generatedGeometry.adjacency.flatMap { case (firstProvince, secondProvince) =>
      val provincePair = normalizedPair(firstProvince, secondProvince)
      if existingByPair.contains(provincePair) then None
      else inferBorderFlag(
        firstProvince,
        secondProvince,
        terrainMaskByProvince,
        seed,
        borderSpecGenerationPolicy
      ).map(flag =>
        Border(provincePair._1, provincePair._2, flag)
      )
    }

    val fallbackBorders =
      if inferredBorders.nonEmpty || generatedGeometry.borders.nonEmpty then Vector.empty
      else inferFallbackBorder(generatedGeometry.adjacency, terrainMaskByProvince)

    generatedGeometry.copy(
      borders = (generatedGeometry.borders ++ inferredBorders ++ fallbackBorders).distinct
    )

  private def normalizedPair(
      firstProvince: ProvinceId,
      secondProvince: ProvinceId
  ): (ProvinceId, ProvinceId) =
    if firstProvince.value <= secondProvince.value then (firstProvince, secondProvince)
    else (secondProvince, firstProvince)

  private def inferBorderFlag(
      firstProvince: ProvinceId,
      secondProvince: ProvinceId,
      terrainMaskByProvince: Map[ProvinceId, Long],
      seed: Long,
      borderSpecGenerationPolicy: BorderSpecGenerationPolicy
  ): Option[BorderFlag] =
    val firstTerrainMask = terrainMaskByProvince.get(firstProvince).map(TerrainMask.apply)
    val secondTerrainMask = terrainMaskByProvince.get(secondProvince).map(TerrainMask.apply)

    val hasSea =
      firstTerrainMask.exists(isSeaTerrain) || secondTerrainMask.exists(isSeaTerrain)
    if hasSea then None
    else
      val hasHighland =
        firstTerrainMask.exists(isHighlandTerrain) || secondTerrainMask.exists(isHighlandTerrain)

      val bucket = stableBorderBucket(firstProvince, secondProvince, seed)
      if hasHighland then
        val mountainThreshold = toBucketThreshold(borderSpecGenerationPolicy.highlandMountainPercent.value.value)
        val mountainPassThreshold =
          mountainThreshold + toBucketThreshold(borderSpecGenerationPolicy.highlandMountainPassPercent.value.value)
        val roadThreshold =
          mountainPassThreshold + toBucketThreshold(borderSpecGenerationPolicy.highlandRoadPercent.value.value)
        if bucket < mountainThreshold then Some(BorderFlag.Impassable)
        else if bucket < mountainPassThreshold then Some(BorderFlag.MountainPass)
        else if bucket < roadThreshold then Some(BorderFlag.Road)
        else None
      else
        val riverThreshold = toBucketThreshold(borderSpecGenerationPolicy.nonHighlandRiverPercent.value.value)
        val roadThreshold =
          riverThreshold + toBucketThreshold(borderSpecGenerationPolicy.nonHighlandRoadPercent.value.value)
        val bridgedRiverThreshold =
          roadThreshold + toBucketThreshold(borderSpecGenerationPolicy.nonHighlandBridgedRiverPercent.value.value)
        if bucket < riverThreshold then Some(BorderFlag.River)
        else if bucket < roadThreshold then Some(BorderFlag.Road)
        else if bucket < bridgedRiverThreshold then Some(BorderFlag.BridgedRiver)
        else None

  private def toBucketThreshold(percentValue: Double): Int =
    math.round(percentValue * 100.0).toInt

  private def inferFallbackBorder(
      adjacency: Vector[(ProvinceId, ProvinceId)],
      terrainMaskByProvince: Map[ProvinceId, Long]
  ): Vector[Border] =
    adjacency
      .map { case (firstProvince, secondProvince) => normalizedPair(firstProvince, secondProvince) }
      .distinct
      .collectFirst {
        case (firstProvince, secondProvince)
            if isLandProvince(firstProvince, terrainMaskByProvince) &&
              isLandProvince(secondProvince, terrainMaskByProvince) =>
          val firstTerrainMask = terrainMaskByProvince.get(firstProvince).map(TerrainMask.apply)
          val secondTerrainMask = terrainMaskByProvince.get(secondProvince).map(TerrainMask.apply)
          val hasHighland =
            firstTerrainMask.exists(isHighlandTerrain) || secondTerrainMask.exists(isHighlandTerrain)
          val borderFlag = if hasHighland then BorderFlag.MountainPass else BorderFlag.Road
          Border(firstProvince, secondProvince, borderFlag)
      }
      .toVector

  private def isLandProvince(
      provinceId: ProvinceId,
      terrainMaskByProvince: Map[ProvinceId, Long]
  ): Boolean =
    terrainMaskByProvince
      .get(provinceId)
      .map(TerrainMask.apply)
      .exists(mask => !isSeaTerrain(mask))

  private def stableBorderBucket(
      firstProvince: ProvinceId,
      secondProvince: ProvinceId,
      seed: Long
  ): Int =
    val first = firstProvince.value.toLong
    val second = secondProvince.value.toLong
    val hash = (first * 73856093L) ^ (second * 19349663L) ^ (seed * 83492791L)
    ((hash & 0x7fffffffL) % 100L).toInt

  private def isSeaTerrain(terrainMask: TerrainMask): Boolean =
    terrainMask.hasFlag(TerrainFlag.Sea) || terrainMask.hasFlag(TerrainFlag.DeepSea)

  private def isHighlandTerrain(terrainMask: TerrainMask): Boolean =
    terrainMask.hasFlag(TerrainFlag.Highlands) || terrainMask.hasFlag(TerrainFlag.Mountains)
