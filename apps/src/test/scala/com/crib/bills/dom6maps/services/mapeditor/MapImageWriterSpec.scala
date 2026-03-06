package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.instances.either.*
import fs2.Stream
import fs2.io.file.Path
import java.nio.file.{Files => JFiles}
import model.{ProvinceId, TerrainFlag}
import model.map.*
import model.map.image.{MapImagePainter, MapTerrainPainter, ProvincePixelRasterizer}
import weaver.SimpleIOSuite

object MapImageWriterSpec extends SimpleIOSuite:
  type EC[A] = Either[Throwable, A]

  test("writeMainImage emits a targa image from map layer") {
    val writer = new MapImageWriterImpl[IO]
    val mapState = MapState.empty.copy(
      terrains = Vector(
        Terrain(ProvinceId(1), TerrainFlag.Plains.mask),
        Terrain(ProvinceId(2), TerrainFlag.Sea.mask)
      )
    )
    val passThrough = Vector(
      ImageFile("generated.tga"),
      Pb(0, 0, 2, ProvinceId(1)),
      Pb(2, 0, 1, ProvinceId(2))
    )
    val layer = MapLayer[IO](mapState, Stream.emits(passThrough).covary[IO])

    for
      directory <- IO(JFiles.createTempDirectory("map-image-writer"))
      outputPath = Path.fromNioPath(directory.resolve("generated.tga"))
      result <- writer.writeMainImage[EC](layer, outputPath)
      _ <- IO.fromEither(result)
      bytes <- IO(JFiles.readAllBytes(outputPath.toNioPath))
    yield expect.all(
      JFiles.exists(outputPath.toNioPath),
      bytes.length > 18,
      bytes(2) == 2.toByte,
      bytes(16) == 24.toByte,
      bytes(17) == 0.toByte
    )
  }

  test("resolveImagePath defaults to map file base name when image directive is absent") {
    val mapPath = Path("/tmp/example/mapfile.map")
    val resolved = MapImageWriter.resolveImagePath(mapPath, Vector.empty)
    IO(expect(resolved.toString == "/tmp/example/mapfile.tga"))
  }

  test("writeMainImage prefers dimensions inferred from #pb runs over #mapsize/state size") {
    val writer = new MapImageWriterImpl[IO]
    val mapState = MapState.empty.copy(
      size = Some(MapDimensions.square(MapSize.from(6).toOption.get)),
      terrains = Vector(Terrain(ProvinceId(1), TerrainFlag.Plains.mask))
    )
    val passThrough = Vector(
      MapSizePixels(MapWidthPixels(1536), MapHeightPixels(960)),
      Pb(0, 0, 1536, ProvinceId(1)),
      Pb(0, 799, 1536, ProvinceId(1))
    )
    val layer = MapLayer[IO](mapState, Stream.emits(passThrough).covary[IO])

    for
      directory <- IO(JFiles.createTempDirectory("map-image-writer-pb-size"))
      outputPath = Path.fromNioPath(directory.resolve("generated.tga"))
      result <- writer.writeMainImage[EC](layer, outputPath)
      _ <- IO.fromEither(result)
      bytes <- IO(JFiles.readAllBytes(outputPath.toNioPath))
    yield
      expect.all(
        bytes(12) == 0.toByte,
        bytes(13) == 6.toByte,
        bytes(14) == 32.toByte,
        bytes(15) == 3.toByte
      )
  }

  test("writeMainImage uses injected terrain painter implementation") {
    val constantPainter = new MapTerrainPainter:
      override def paint(
          ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
          terrainMaskByProvince: Map[ProvinceId, Long]
      ): MapImagePainter.MapImage =
        val bytes = Array.fill[Byte](ownership.widthPixels * ownership.heightPixels * 3)(7.toByte)
        MapImagePainter.MapImage(ownership.widthPixels, ownership.heightPixels, bytes)

    val writer = new MapImageWriterImpl[IO](constantPainter)
    val mapState = MapState.empty.copy(
      terrains = Vector(Terrain(ProvinceId(1), TerrainFlag.Plains.mask))
    )
    val passThrough = Vector(
      ImageFile("generated.tga"),
      Pb(0, 0, 2, ProvinceId(1))
    )
    val layer = MapLayer[IO](mapState, Stream.emits(passThrough).covary[IO])

    for
      directory <- IO(JFiles.createTempDirectory("map-image-writer-injected-painter"))
      outputPath = Path.fromNioPath(directory.resolve("generated.tga"))
      result <- writer.writeMainImage[EC](layer, outputPath)
      _ <- IO.fromEither(result)
      bytes <- IO(JFiles.readAllBytes(outputPath.toNioPath))
      firstPayloadByte = bytes(18) & 0xff
      secondPayloadByte = bytes(21) & 0xff
    yield expect.all(
      firstPayloadByte == 255,
      secondPayloadByte == 7
    )
  }
