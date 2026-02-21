package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.instances.either.*
import fs2.Stream
import fs2.io.file.Path
import java.nio.file.{Files => JavaFiles}
import model.{ProvinceId, TerrainFlag}
import model.map.{ImageFile, MapLayer, MapState, Pb, Terrain}
import model.map.generation.TerrainImageVariantPolicy
import weaver.SimpleIOSuite

object TerrainImageVariantServiceSpec extends SimpleIOSuite:
  type ErrorOr[A] = Either[Throwable, A]

  test("writes winter image for base-and-winter policy") {
    val service = new TerrainImageVariantServiceImpl[IO]
    val mapLayer = MapLayer[IO](
      MapState.empty.copy(terrains = Vector(Terrain(ProvinceId(1), TerrainFlag.Forest.mask))),
      Stream.emits(
        Vector(
          ImageFile("example.tga"),
          Pb(0, 0, 2, ProvinceId(1))
        )
      ).covary[IO]
    )

    for
      directory <- IO(JavaFiles.createTempDirectory("terrain-variant-winter"))
      baseImagePath = Path.fromNioPath(directory.resolve("example.tga"))
      result <- service.writeVariants[ErrorOr](mapLayer, baseImagePath, TerrainImageVariantPolicy.BaseAndWinter)
      _ <- IO.fromEither(result)
    yield expect(JavaFiles.exists(directory.resolve("example_winter.tga")))
  }

  test("writes full terrain set variants") {
    val service = new TerrainImageVariantServiceImpl[IO]
    val mapLayer = MapLayer[IO](
      MapState.empty.copy(terrains = Vector(Terrain(ProvinceId(1), TerrainFlag.Forest.mask))),
      Stream.emits(
        Vector(
          ImageFile("example.tga"),
          Pb(0, 0, 2, ProvinceId(1))
        )
      ).covary[IO]
    )

    for
      directory <- IO(JavaFiles.createTempDirectory("terrain-variant-full"))
      baseImagePath = Path.fromNioPath(directory.resolve("example.tga"))
      result <- service.writeVariants[ErrorOr](mapLayer, baseImagePath, TerrainImageVariantPolicy.FullTerrainSet)
      _ <- IO.fromEither(result)
    yield expect.all(
      JavaFiles.exists(directory.resolve("example_forest.tga")),
      JavaFiles.exists(directory.resolve("example_forestw.tga")),
      JavaFiles.exists(directory.resolve("example_winter.tga"))
    )
  }
