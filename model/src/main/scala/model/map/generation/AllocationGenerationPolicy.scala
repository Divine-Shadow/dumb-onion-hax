package com.crib.bills.dom6maps
package model.map.generation

import cats.ApplicativeError
import cats.syntax.all.*
import model.{Nation, TerrainMask}

enum AllocationLayer:
  case Surface
  case Underground

enum ProfileEnvironment:
  case Surface
  case Underground
  case Any

enum DistanceTiePolicy:
  case NeutralTie

enum RegionOwner:
  case Player(nation: Nation)
  case Neutral

final case class CapRingSize private (value: Int) extends AnyVal

object CapRingSize:
  def from[ErrorChannel[_]](
      value: Int
  )(using errorChannel: ApplicativeError[ErrorChannel, Throwable]): ErrorChannel[CapRingSize] =
    if value >= 1 && value <= 6 then errorChannel.pure(CapRingSize(value))
    else
      errorChannel.raiseError(
        IllegalArgumentException(s"capRingSize must be in range [1, 6], received: $value")
      )

  def unsafe(value: Int): CapRingSize =
    CapRingSize(value)

final case class WaterPercentage(value: Percent) extends AnyVal

object WaterPercentage:
  def from[ErrorChannel[_]](
      value: Double
  )(using errorChannel: ApplicativeError[ErrorChannel, Throwable]): ErrorChannel[WaterPercentage] =
    Percent.from[ErrorChannel](value).map(WaterPercentage.apply)

  def unsafe(value: Double): WaterPercentage =
    WaterPercentage(Percent.unsafe(value))

final case class PlayerStartAssignment(
    nation: Nation,
    surfaceStart: Option[model.ProvinceId],
    undergroundStart: Option[model.ProvinceId]
)

final case class AllocationPartition(
    ownerByProvince: Map[model.ProvinceId, RegionOwner],
    distanceByProvinceByNation: Map[model.ProvinceId, Map[Nation, Int]]
)

final case class PlayerAllocationProfile(
    capRingSize: CapRingSize,
    capitalTerrainMask: TerrainMask,
    capRingTerrainMasks: Vector[TerrainMask],
    waterPercentageOutsideCapitalRing: WaterPercentage,
    startsUnderground: Boolean,
    hasCaveEntranceInCapRing: Boolean
)

final case class NeutralAllocationProfile(
    waterPercentage: WaterPercentage,
    terrainDistributionPolicy: TerrainDistributionPolicy
)

final case class AllocationProfileCatalog(
    profiles: Map[(Nation, ProfileEnvironment), PlayerAllocationProfile]
):
  def resolve(
      nation: Nation,
      layer: AllocationLayer
  ): Option[PlayerAllocationProfile] =
    val exactEnvironment =
      layer match
        case AllocationLayer.Surface => ProfileEnvironment.Surface
        case AllocationLayer.Underground => ProfileEnvironment.Underground

    profiles
      .get((nation, exactEnvironment))
      .orElse(profiles.get((nation, ProfileEnvironment.Any)))

sealed trait AllocationGenerationPolicy

object AllocationGenerationPolicy:
  case object Disabled extends AllocationGenerationPolicy

  final case class Enabled(
      tiePolicy: DistanceTiePolicy,
      profileCatalog: AllocationProfileCatalog,
      neutralProfile: NeutralAllocationProfile,
      seedSalt: Long = 0L
  ) extends AllocationGenerationPolicy
