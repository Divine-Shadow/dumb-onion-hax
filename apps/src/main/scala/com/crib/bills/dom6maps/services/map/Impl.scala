package com.crib.bills.dom6maps
package apps.services.map

import cats.effect.Async
import cats.syntax.all.*
import fs2.Stream
import java.nio.charset.StandardCharsets
import model.map.*
import model.map.Renderer.*

trait Impl[Sequencer[_]] extends Service[Sequencer]:
  protected given sequencer: Async[Sequencer]

  override def processUpload(request: MapUploadRequest): Sequencer[String] =
    for
      bytes <- sequencer.delay(request.map.getBytes(StandardCharsets.UTF_8))
      directives <- MapFileParser
                      .parse[Sequencer]
                      .apply(Stream.emits(bytes).covary[Sequencer])
                      .compile
                      .toVector
      rest = directives.filterNot(_.isInstanceOf[MapSizePixels])
      newDirectives = MapSizePixels(request.config.mapSize.width, request.config.mapSize.height) +: rest
    yield newDirectives.map(_.render).mkString("\n")
