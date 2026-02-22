package com.crib.bills.dom6maps
package model.map.generation

import cats.effect.IO
import cats.instances.either.*
import weaver.SimpleIOSuite

object BorderSpecGenerationPolicySpec extends SimpleIOSuite:
  type ErrorOr[A] = Either[Throwable, A]

  test("creates policy from valid raw percentages") {
    val result = BorderSpecGenerationPolicy.fromRaw[ErrorOr](
      nonHighlandRiverPercent = 0.20,
      nonHighlandRoadPercent = 0.20,
      nonHighlandBridgedRiverPercent = 0.0,
      highlandMountainPercent = 0.30,
      highlandMountainPassPercent = 0.20,
      highlandRoadPercent = 0.15
    )

    IO(expect(result == Right(BorderSpecGenerationPolicy.default)))
  }

  test("rejects invalid percentage range") {
    val result = BorderSpecGenerationPolicy.fromRaw[ErrorOr](
      nonHighlandRiverPercent = 1.4,
      nonHighlandRoadPercent = 0.10,
      nonHighlandBridgedRiverPercent = 0.0,
      highlandMountainPercent = 0.20,
      highlandMountainPassPercent = 0.16,
      highlandRoadPercent = 0.07
    )

    IO(expect(result.isLeft))
  }

  test("rejects invalid mutually exclusive mountain and river combination") {
    val result = BorderSpecGenerationPolicy.fromRaw[ErrorOr](
      nonHighlandRiverPercent = 0.60,
      nonHighlandRoadPercent = 0.10,
      nonHighlandBridgedRiverPercent = 0.0,
      highlandMountainPercent = 0.50,
      highlandMountainPassPercent = 0.10,
      highlandRoadPercent = 0.07
    )

    IO(expect(result.isLeft))
  }
