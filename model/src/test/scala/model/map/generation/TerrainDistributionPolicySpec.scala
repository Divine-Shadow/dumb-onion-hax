package com.crib.bills.dom6maps
package model.map.generation

import cats.effect.IO
import cats.instances.either.*
import weaver.SimpleIOSuite

object TerrainDistributionPolicySpec extends SimpleIOSuite:
  type ErrorOr[A] = Either[Throwable, A]

  test("creates policy from valid raw percentages") {
    val result = TerrainDistributionPolicy.fromRaw[ErrorOr](
      swampPercent = 0.07,
      wastePercent = 0.07,
      highlandPercent = 0.06,
      forestPercent = 0.17,
      farmPercent = 0.15,
      extraLakePercent = 0.07
    )

    IO(expect(result.isRight))
  }

  test("rejects invalid terrain distribution total above one") {
    val result = TerrainDistributionPolicy.fromRaw[ErrorOr](
      swampPercent = 0.20,
      wastePercent = 0.20,
      highlandPercent = 0.20,
      forestPercent = 0.20,
      farmPercent = 0.20,
      extraLakePercent = 0.20
    )

    IO(expect(result.isLeft))
  }
