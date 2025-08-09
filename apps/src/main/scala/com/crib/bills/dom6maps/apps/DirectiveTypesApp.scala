package com.crib.bills.dom6maps
package apps

import cats.effect.{IO, IOApp}
import fs2.io.file.Path
import apps.services.map.{DirectiveTypesService, DirectiveTypesServiceImpl}

object DirectiveTypesApp extends IOApp.Simple:
  private val service: DirectiveTypesService[IO] = new DirectiveTypesServiceImpl[IO]

  private val mapPath = Path("data/duel-map-example.map")

  override def run: IO[Unit] =
    service.collect(mapPath).flatMap { directives =>
      IO.println(directives.mkString("\n"))
    }
