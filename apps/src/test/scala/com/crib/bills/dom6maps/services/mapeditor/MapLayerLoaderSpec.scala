package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.instances.either.*
import fs2.io.file.Path
import weaver.SimpleIOSuite
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import model.map.MapState

object MapLayerLoaderSpec extends SimpleIOSuite:
  type EC[A] = Either[Throwable, A]

  test("load returns MapState for valid map") {
    val loader = new MapLayerLoaderImpl[IO]
    for
        result <- loader.load[EC](Path("data/test-map.map"))
        layer  <- IO.fromEither(result)
        pass   <- layer.passThrough.compile.toVector
      yield expect.all(
        layer.state.title.exists(_.value == "Sample Map"),
        pass.nonEmpty
      )
  }

  test("load returns error for missing path") {
    val loader = new MapLayerLoaderImpl[IO]
    loader.load[EC](Path("data/missing.map")).map { result =>
      expect(result.isLeft)
    }
  }

  test("load fails to parse malformed content") {
    val loader = new MapLayerLoaderImpl[IO]
    for
      tmp <- IO(Files.createTempFile("malformed", ".map"))
      _ <- IO(Files.write(tmp, "#terrain 1".getBytes(StandardCharsets.UTF_8)))
      result <- loader.load[EC](Path.fromNioPath(tmp))
    yield expect(result.isLeft)
  }
