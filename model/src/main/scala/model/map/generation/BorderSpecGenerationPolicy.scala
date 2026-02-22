package com.crib.bills.dom6maps
package model.map.generation

import cats.ApplicativeError
import cats.syntax.all.*

final case class Percent private (value: Double)

object Percent:
  val zero: Percent = Percent(0.0)
  def unsafe(value: Double): Percent = Percent(value)

  def from[ErrorChannel[_]](
      value: Double
  )(using errorChannel: ApplicativeError[ErrorChannel, Throwable]): ErrorChannel[Percent] =
    if value >= 0.0 && value <= 1.0 then errorChannel.pure(Percent(value))
    else errorChannel.raiseError(
      IllegalArgumentException(s"Percent value must be in range [0.0, 1.0], received: $value")
    )

final case class NonHighlandRiverPercent(value: Percent) extends AnyVal
final case class NonHighlandRoadPercent(value: Percent) extends AnyVal
final case class NonHighlandBridgedRiverPercent(value: Percent) extends AnyVal
final case class HighlandMountainPercent(value: Percent) extends AnyVal
final case class HighlandMountainPassPercent(value: Percent) extends AnyVal
final case class HighlandRoadPercent(value: Percent) extends AnyVal

final case class BorderSpecGenerationPolicy private (
    nonHighlandRiverPercent: NonHighlandRiverPercent,
    nonHighlandRoadPercent: NonHighlandRoadPercent,
    nonHighlandBridgedRiverPercent: NonHighlandBridgedRiverPercent,
    highlandMountainPercent: HighlandMountainPercent,
    highlandMountainPassPercent: HighlandMountainPassPercent,
    highlandRoadPercent: HighlandRoadPercent
)

object BorderSpecGenerationPolicy:
  val default: BorderSpecGenerationPolicy =
    BorderSpecGenerationPolicy(
      nonHighlandRiverPercent = NonHighlandRiverPercent(Percent.unsafe(0.14)),
      nonHighlandRoadPercent = NonHighlandRoadPercent(Percent.unsafe(0.10)),
      nonHighlandBridgedRiverPercent = NonHighlandBridgedRiverPercent(Percent.unsafe(0.0)),
      highlandMountainPercent = HighlandMountainPercent(Percent.unsafe(0.20)),
      highlandMountainPassPercent = HighlandMountainPassPercent(Percent.unsafe(0.16)),
      highlandRoadPercent = HighlandRoadPercent(Percent.unsafe(0.07))
    )

  def fromRaw[ErrorChannel[_]](
      nonHighlandRiverPercent: Double,
      nonHighlandRoadPercent: Double,
      nonHighlandBridgedRiverPercent: Double,
      highlandMountainPercent: Double,
      highlandMountainPassPercent: Double,
      highlandRoadPercent: Double
  )(using errorChannel: ApplicativeError[ErrorChannel, Throwable]): ErrorChannel[BorderSpecGenerationPolicy] =
    (
      Percent.from[ErrorChannel](nonHighlandRiverPercent),
      Percent.from[ErrorChannel](nonHighlandRoadPercent),
      Percent.from[ErrorChannel](nonHighlandBridgedRiverPercent),
      Percent.from[ErrorChannel](highlandMountainPercent),
      Percent.from[ErrorChannel](highlandMountainPassPercent),
      Percent.from[ErrorChannel](highlandRoadPercent),
      validateTotalAtMostOne[ErrorChannel](
        nonHighlandRiverPercent + nonHighlandRoadPercent + nonHighlandBridgedRiverPercent,
        "non-highland river+road+bridged-river"
      ),
      validateTotalAtMostOne[ErrorChannel](
        highlandMountainPercent + highlandMountainPassPercent + highlandRoadPercent,
        "highland mountain+mountain-pass+road"
      ),
      validateTotalAtMostOne[ErrorChannel](
        highlandMountainPercent + nonHighlandRiverPercent,
        "mountain+rivers"
      )
    ).mapN {
      case (
            nonHighlandRiver,
            nonHighlandRoad,
            nonHighlandBridgedRiver,
            highlandMountain,
            highlandMountainPass,
            highlandRoad,
            _,
            _,
            _
          ) =>
        BorderSpecGenerationPolicy(
          nonHighlandRiverPercent = NonHighlandRiverPercent(nonHighlandRiver),
          nonHighlandRoadPercent = NonHighlandRoadPercent(nonHighlandRoad),
          nonHighlandBridgedRiverPercent = NonHighlandBridgedRiverPercent(nonHighlandBridgedRiver),
          highlandMountainPercent = HighlandMountainPercent(highlandMountain),
          highlandMountainPassPercent = HighlandMountainPassPercent(highlandMountainPass),
          highlandRoadPercent = HighlandRoadPercent(highlandRoad)
        )
    }

  private def validateTotalAtMostOne[ErrorChannel[_]](
      total: Double,
      label: String
  )(using errorChannel: ApplicativeError[ErrorChannel, Throwable]): ErrorChannel[Unit] =
    if total <= 1.0 then errorChannel.pure(())
    else
      errorChannel.raiseError(
        IllegalArgumentException(
          s"Invalid border generation policy: $label must be <= 1.0, received: $total"
        )
      )
