package com.crib.bills.dom6maps
package apps

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*
import fs2.io.file.Path
import cats.instances.either.*
import apps.services.mapeditor.{MapLayerLoaderImpl, MapWriterImpl}

/**
  * Rewrites a map file using current parsing and merge/ordering rules without modifying the state.
  * Usage:
  *   sbt "project apps" "runMain com.crib.bills.dom6maps.apps.MapRewriterApp <in> <out>"
  */
object MapRewriterApp extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    args match
      case in :: out :: Nil =>
        val loader = new MapLayerLoaderImpl[IO]
        val writer = new MapWriterImpl[IO]
        type EC[A] = Either[Throwable, A]
        for
          layerEC <- loader.load[EC](Path(in))
          layer   <- IO.fromEither(layerEC)
          _       <- writer.write[EC](layer, Path(out)).flatMap(IO.fromEither)
        yield ExitCode.Success
      case _ =>
        IO.println("Usage: MapRewriterApp <input.map> <output.map>").as(ExitCode(2))
