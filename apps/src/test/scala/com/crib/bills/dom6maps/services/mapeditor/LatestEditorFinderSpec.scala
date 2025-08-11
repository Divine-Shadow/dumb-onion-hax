package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.instances.either.*
import fs2.io.file.Path
import weaver.SimpleIOSuite
import java.nio.file.Files
import java.nio.file.attribute.FileTime

object LatestEditorFinderSpec extends SimpleIOSuite:
  type EC[A] = Either[Throwable, A]

  test("selects newest directory") {
    val finder = new LatestEditorFinderImpl[IO]
    for
      root <- IO(Files.createTempDirectory("editor-root"))
      older <- IO(Files.createDirectory(root.resolve("older")))
      _ <- IO(Files.setLastModifiedTime(older, FileTime.fromMillis(1000)))
      newer <- IO(Files.createDirectory(root.resolve("newer")))
      _ <- IO(Files.setLastModifiedTime(newer, FileTime.fromMillis(2000)))
      resultEC <- finder.mostRecentFolder[EC](Path.fromNioPath(root))
      result <- IO.fromEither(resultEC)
    yield expect(result.fileName.toString == "newer")
  }

  test("throws when no directories found") {
    val finder = new LatestEditorFinderImpl[IO]
    for
      root <- IO(Files.createTempDirectory("empty-root"))
      resultEC <- finder.mostRecentFolder[EC](Path.fromNioPath(root))
    yield expect(resultEC.fold(_.isInstanceOf[NoSuchElementException], _ => false))
  }
