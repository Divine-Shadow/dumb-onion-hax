package com.crib.bills.dom6maps
package apps.services.mapeditor

import model.{ProvinceId, TerrainFlag, TerrainMask}
import model.map.Terrain
import model.map.generation.{AllocationLayer, NeutralAllocationProfile, PlayerAllocationProfile, TerrainDistributionPolicy}

trait AllocationTerrainService:
  def applyAllocation(
      terrains: Vector[Terrain],
      playerAllotments: Vector[(PlayerAllottedProvinces, PlayerAllocationProfile)],
      neutralProvinceIds: Vector[ProvinceId],
      neutralProfile: NeutralAllocationProfile,
      defaultTerrainDistributionPolicy: TerrainDistributionPolicy,
      layer: AllocationLayer,
      seed: Long
  ): Vector[Terrain]

class AllocationTerrainServiceImpl extends AllocationTerrainService:
  private val removablePrimaryFlags =
    Vector(
      TerrainFlag.Sea,
      TerrainFlag.DeepSea,
      TerrainFlag.FreshWater,
      TerrainFlag.Highlands,
      TerrainFlag.Swamp,
      TerrainFlag.Waste,
      TerrainFlag.Forest,
      TerrainFlag.Farm,
      TerrainFlag.Mountains
    )

  override def applyAllocation(
      terrains: Vector[Terrain],
      playerAllotments: Vector[(PlayerAllottedProvinces, PlayerAllocationProfile)],
      neutralProvinceIds: Vector[ProvinceId],
      neutralProfile: NeutralAllocationProfile,
      defaultTerrainDistributionPolicy: TerrainDistributionPolicy,
      layer: AllocationLayer,
      seed: Long
  ): Vector[Terrain] =
    val terrainByProvince = terrains.map(terrain => terrain.province -> terrain).toMap

    val playerOverrides = playerAllotments.flatMap { case (allottedProvinces, profile) =>
      val capRingOverrides = assignCapRingTerrains(
        allottedProvinces,
        profile,
        defaultTerrainDistributionPolicy,
        terrainByProvince,
        layer,
        seed
      )
      val nonRingOverrides = assignNonRingTerrains(
        allottedProvinces,
        profile,
        defaultTerrainDistributionPolicy,
        terrainByProvince,
        layer,
        seed
      )
      val capitalMask = preserveLayerFlags(
        terrainByProvince(allottedProvinces.capitalProvince).mask,
        profile.capitalTerrainMask.value,
        layer
      )

      Vector(allottedProvinces.capitalProvince -> capitalMask) ++ capRingOverrides ++ nonRingOverrides
    }.toMap

    val neutralOverrides = assignNeutralTerrains(
      neutralProvinceIds,
      terrainByProvince,
      neutralProfile,
      layer,
      seed
    ).toMap

    terrains.map { terrain =>
      val updatedMask =
        playerOverrides
          .get(terrain.province)
          .orElse(neutralOverrides.get(terrain.province))
          .getOrElse(terrain.mask)
      terrain.copy(mask = updatedMask)
    }

  private def assignCapRingTerrains(
      allottedProvinces: PlayerAllottedProvinces,
      profile: PlayerAllocationProfile,
      defaultTerrainDistributionPolicy: TerrainDistributionPolicy,
      terrainByProvince: Map[ProvinceId, Terrain],
      layer: AllocationLayer,
      seed: Long
  ): Vector[(ProvinceId, Long)] =
    val shuffledConfiguredMasks = deterministicShuffle(
      profile.capRingTerrainMasks,
      seed + allottedProvinces.nation.id.toLong * 17L + layerSeedSalt(layer)
    )

    allottedProvinces.capRingProvinceIds.zipWithIndex.map { case (provinceId, index) =>
      val configured = shuffledConfiguredMasks.lift(index).map(_.value)
      val sampledMask =
        configured.getOrElse {
          sampleLandTerrainMask(
            defaultTerrainDistributionPolicy,
            sampledDraw(seed, provinceId, 37L)
          )
        }
      val currentMask = terrainByProvince.get(provinceId).map(_.mask).getOrElse(0L)
      provinceId -> preserveLayerFlags(currentMask = currentMask, assignedMask = sampledMask, layer = layer)
    }

  private def assignNonRingTerrains(
      allottedProvinces: PlayerAllottedProvinces,
      profile: PlayerAllocationProfile,
      defaultTerrainDistributionPolicy: TerrainDistributionPolicy,
      terrainByProvince: Map[ProvinceId, Terrain],
      layer: AllocationLayer,
      seed: Long
  ): Vector[(ProvinceId, Long)] =
    val ordered = deterministicShuffle(
      allottedProvinces.nonRingProvinceIds,
      seed + allottedProvinces.nation.id.toLong * 19L + layerSeedSalt(layer)
    )
    val waterCount = math.round(ordered.size.toDouble * profile.waterPercentageOutsideCapitalRing.value.value).toInt

    ordered.zipWithIndex.map { case (provinceId, index) =>
      val assignedMask =
        if index < waterCount then waterMaskForLayer(layer)
        else sampleLandTerrainMask(defaultTerrainDistributionPolicy, sampledDraw(seed, provinceId, 53L))
      val currentMask = terrainByProvince.get(provinceId).map(_.mask).getOrElse(0L)
      provinceId -> preserveLayerFlags(currentMask = currentMask, assignedMask = assignedMask, layer = layer)
    }

  private def assignNeutralTerrains(
      neutralProvinceIds: Vector[ProvinceId],
      terrainByProvince: Map[ProvinceId, Terrain],
      neutralProfile: NeutralAllocationProfile,
      layer: AllocationLayer,
      seed: Long
  ): Vector[(ProvinceId, Long)] =
    val ordered = deterministicShuffle(
      neutralProvinceIds,
      seed + 991L + layerSeedSalt(layer)
    )
    val waterCount = math.round(ordered.size.toDouble * neutralProfile.waterPercentage.value.value).toInt

    ordered.zipWithIndex.map { case (provinceId, index) =>
      val assignedMask =
        if index < waterCount then waterMaskForLayer(layer)
        else sampleLandTerrainMask(neutralProfile.terrainDistributionPolicy, sampledDraw(seed, provinceId, 71L))
      val currentMask = terrainByProvince.get(provinceId).map(_.mask).getOrElse(0L)
      provinceId -> preserveLayerFlags(currentMask, assignedMask, layer)
    }

  private def preserveLayerFlags(
      currentMask: Long,
      assignedMask: Long,
      layer: AllocationLayer
  ): Long =
    val baseMask =
      removablePrimaryFlags.foldLeft(TerrainMask(currentMask)) { (mask, flag) =>
        mask.withoutFlag(flag)
      }

    val assigned = TerrainMask(baseMask.value | assignedMask)
    layer match
      case AllocationLayer.Surface => assigned.value
      case AllocationLayer.Underground => assigned.withFlag(TerrainFlag.Cave).value

  private def sampleLandTerrainMask(
      terrainDistributionPolicy: TerrainDistributionPolicy,
      draw: Double
  ): Long =
    val swampThreshold = terrainDistributionPolicy.swampPercent.value.value
    val wasteThreshold = swampThreshold + terrainDistributionPolicy.wastePercent.value.value
    val highlandThreshold = wasteThreshold + terrainDistributionPolicy.highlandPercent.value.value
    val forestThreshold = highlandThreshold + terrainDistributionPolicy.forestPercent.value.value
    val farmThreshold = forestThreshold + terrainDistributionPolicy.farmPercent.value.value
    val extraLakeThreshold = farmThreshold + terrainDistributionPolicy.extraLakePercent.value.value

    if draw < swampThreshold then TerrainFlag.Swamp.mask
    else if draw < wasteThreshold then TerrainFlag.Waste.mask
    else if draw < highlandThreshold then TerrainFlag.Highlands.mask
    else if draw < forestThreshold then TerrainFlag.Forest.mask
    else if draw < farmThreshold then TerrainFlag.Farm.mask
    else if draw < extraLakeThreshold then TerrainFlag.FreshWater.mask
    else TerrainFlag.Plains.mask

  private def layerSeedSalt(layer: AllocationLayer): Long =
    layer match
      case AllocationLayer.Surface => 101L
      case AllocationLayer.Underground => 202L

  private def sampledDraw(
      seed: Long,
      provinceId: ProvinceId,
      salt: Long
  ): Double =
    val mixed = splitMix64(seed + provinceId.value.toLong * 0x9e3779b97f4a7c15L + salt)
    val positiveHash = mixed & 0x7fffffffffffffffL
    positiveHash.toDouble / Long.MaxValue.toDouble

  private def splitMix64(value: Long): Long =
    var z = value + 0x9e3779b97f4a7c15L
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL
    z ^ (z >>> 31)

  private def deterministicShuffle[A](
      values: Vector[A],
      seed: Long
  ): Vector[A] =
    val random = new scala.util.Random(seed)
    random.shuffle(values)

  private def waterMaskForLayer(layer: AllocationLayer): Long =
    layer match
      case AllocationLayer.Surface => TerrainFlag.Sea.mask
      case AllocationLayer.Underground => TerrainFlag.FreshWater.mask
