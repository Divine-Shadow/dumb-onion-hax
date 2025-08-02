package com.crib.bills.dom6maps
package apps

import cats.effect.{IO, IOApp}
import fs2.io.file.{Files, Path}
import com.crib.bills.dom6maps.model.map.{MapFileParser, MapWidth, MapHeight}
import com.crib.bills.dom6maps.model.map.Renderer.*

object WrapSeverApp extends IOApp.Simple:
  private val inputFile = Path("data") / "five-by-twelve.map"
  private val outputFile = Path("data") / "five-by-twelve.hwrap.map"

  def run: IO[Unit] =
    MapFileParser
      .parseFile[IO](inputFile)
      .through(WrapSever.verticalPipe[IO](MapWidth(5), MapHeight(12)))
      .map(_.render)
      .intersperse("\n")
      .through(fs2.text.utf8.encode)
      .through(Files[IO].writeAll(outputFile))
      .compile
      .drain

