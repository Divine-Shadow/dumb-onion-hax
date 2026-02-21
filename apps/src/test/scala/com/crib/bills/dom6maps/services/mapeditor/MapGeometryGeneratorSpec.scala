package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.instances.either.*
import model.map.{MapSize, WrapState}
import model.map.generation.GeometryGenerationInput
import weaver.SimpleIOSuite

object MapGeometryGeneratorSpec extends SimpleIOSuite:
  type ErrorOr[A] = Either[Throwable, A]

  test("grid noise generator produces geometry with expected shape") {
    val generator = new GridNoiseMapGeometryGeneratorImpl[IO]
    val input = GeometryGenerationInput(
      mapSize = MapSize.from(2).toOption.get,
      provinceCount = 8,
      wrapState = WrapState.NoWrap,
      seed = 12345L,
      seaRatio = 0.35,
      noiseScale = 1.0,
      gridJitter = 0.5
    )

    for
      result <- generator.generate[ErrorOr](input)
      generatedGeometry <- IO.fromEither(result)
    yield expect.all(
      generatedGeometry.provincePixelRuns.nonEmpty,
      generatedGeometry.terrainByProvince.length == 8,
      generatedGeometry.provinceCentroids.size == 8,
      generatedGeometry.adjacency.nonEmpty
    )
  }

  test("grid noise generator is deterministic for the same seed") {
    val generator = new GridNoiseMapGeometryGeneratorImpl[IO]
    val input = GeometryGenerationInput(
      mapSize = MapSize.from(2).toOption.get,
      provinceCount = 8,
      wrapState = WrapState.HorizontalWrap,
      seed = 777L,
      seaRatio = 0.25,
      noiseScale = 1.3,
      gridJitter = 0.4
    )

    for
      firstResult <- generator.generate[ErrorOr](input)
      secondResult <- generator.generate[ErrorOr](input)
      firstGeometry <- IO.fromEither(firstResult)
      secondGeometry <- IO.fromEither(secondResult)
    yield expect.all(
      firstGeometry.provincePixelRuns == secondGeometry.provincePixelRuns,
      firstGeometry.adjacency == secondGeometry.adjacency,
      firstGeometry.terrainByProvince == secondGeometry.terrainByProvince,
      firstGeometry.provinceCentroids == secondGeometry.provinceCentroids
    )
  }
