package com.crib.bills.dom6maps
package apps

import cats.effect.IO
import cats.syntax.all.*
import cats.instances.either.*
import fs2.Stream
import fs2.io.file.{Files as Fs2Files, Path}
import weaver.SimpleIOSuite
import java.nio.file.{Files => JFiles}
import model.map.*
import apps.services.mapeditor.MapWriterImpl

object MapSizePassThroughSpec extends SimpleIOSuite:
  type EC[A] = Either[Throwable, A]
  test("preserves #mapsize when unencodable (even province width)") {
    val content =
      """#imagefile test.tga
#mapsize 1536 960
#wraparound
""".stripMargin
    for
      tmpDir <- IO(JFiles.createTempDirectory("mapsize-pt"))
      in  = Path.fromNioPath(tmpDir.resolve("in.map"))
      out = Path.fromNioPath(tmpDir.resolve("out.map"))
      _ <- IO(JFiles.writeString(in.toNioPath, content))
      directives <- MapFileParser.parseFile[IO](in).compile.toVector
      layer <- MapState.fromDirectivesWithPassThrough(Stream.emits(directives).covary[IO])
      _ <- new MapWriterImpl[IO].write[EC](layer, out).flatMap(IO.fromEither)
      outText <- Fs2Files[IO].readAll(out).through(fs2.text.utf8.decode).compile.string
    yield expect(outText.contains("#mapsize 1536 960"))
  }
