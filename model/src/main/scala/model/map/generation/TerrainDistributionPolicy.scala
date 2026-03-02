package com.crib.bills.dom6maps
package model.map.generation

import cats.ApplicativeError
import cats.syntax.all.*

final case class SwampPercent(value: Percent) extends AnyVal
final case class WastePercent(value: Percent) extends AnyVal
final case class HighlandPercent(value: Percent) extends AnyVal
final case class ForestPercent(value: Percent) extends AnyVal
final case class FarmPercent(value: Percent) extends AnyVal
final case class ExtraLakePercent(value: Percent) extends AnyVal

final case class TerrainDistributionPolicy private (
    swampPercent: SwampPercent,
    wastePercent: WastePercent,
    highlandPercent: HighlandPercent,
    forestPercent: ForestPercent,
    farmPercent: FarmPercent,
    extraLakePercent: ExtraLakePercent
):
  def plainPercent: Percent =
    Percent.unsafe(
      1.0 - (
        swampPercent.value.value +
          wastePercent.value.value +
          highlandPercent.value.value +
          forestPercent.value.value +
          farmPercent.value.value +
          extraLakePercent.value.value
      )
    )

object TerrainDistributionPolicy:
  val default: TerrainDistributionPolicy =
    TerrainDistributionPolicy(
      swampPercent = SwampPercent(Percent.unsafe(0.16)),
      wastePercent = WastePercent(Percent.unsafe(0.18)),
      highlandPercent = HighlandPercent(Percent.unsafe(0.16)),
      forestPercent = ForestPercent(Percent.unsafe(0.18)),
      farmPercent = FarmPercent(Percent.unsafe(0.16)),
      extraLakePercent = ExtraLakePercent(Percent.zero)
    )

  def fromRaw[ErrorChannel[_]](
      swampPercent: Double,
      wastePercent: Double,
      highlandPercent: Double,
      forestPercent: Double,
      farmPercent: Double,
      extraLakePercent: Double
  )(using errorChannel: ApplicativeError[ErrorChannel, Throwable]): ErrorChannel[TerrainDistributionPolicy] =
    (
      Percent.from[ErrorChannel](swampPercent),
      Percent.from[ErrorChannel](wastePercent),
      Percent.from[ErrorChannel](highlandPercent),
      Percent.from[ErrorChannel](forestPercent),
      Percent.from[ErrorChannel](farmPercent),
      Percent.from[ErrorChannel](extraLakePercent),
      validateTotalAtMostOne[ErrorChannel](
        swampPercent + wastePercent + highlandPercent + forestPercent + farmPercent + extraLakePercent
      )
    ).mapN {
      case (
            swamp,
            waste,
            highland,
            forest,
            farm,
            extraLake,
            _
          ) =>
        TerrainDistributionPolicy(
          swampPercent = SwampPercent(swamp),
          wastePercent = WastePercent(waste),
          highlandPercent = HighlandPercent(highland),
          forestPercent = ForestPercent(forest),
          farmPercent = FarmPercent(farm),
          extraLakePercent = ExtraLakePercent(extraLake)
        )
    }

  private def validateTotalAtMostOne[ErrorChannel[_]](
      total: Double
  )(using errorChannel: ApplicativeError[ErrorChannel, Throwable]): ErrorChannel[Unit] =
    if total <= 1.0 then errorChannel.pure(())
    else
      errorChannel.raiseError(
        IllegalArgumentException(
          s"Invalid terrain distribution policy: non-sea terrain total must be <= 1.0, received: $total"
        )
      )
