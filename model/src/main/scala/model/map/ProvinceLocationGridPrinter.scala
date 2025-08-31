package com.crib.bills.dom6maps
package model.map

import cats.Applicative
import cats.effect.{Concurrent, Sync}
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import model.Printer

object ProvinceLocationGridPrinter:
  def print[F[_]](path: Path, printer: Printer[F])(using concurrent: Concurrent[F], sync: Sync[F], files: Files[F]): F[Unit] =
    given cats.Applicative[F] = sync
    concurrent.flatMap(MapState.fromDirectives(MapFileParser.parseFile[F](path))) { state =>
      state.size match
        case Some(size) => render(size, state.provinceLocations, printer)
        case None       => concurrent.unit
    }

  def render[F[_]: Applicative](size: MapSize, locations: ProvinceLocations, printer: Printer[F]): F[Unit] =
    val side = size.value
    val lines = (0 until side).toVector.map { y =>
      (0 until side).toVector
        .map { x =>
          val loc = ProvinceLocation(XCell(x), YCell(y))
          locations.provinceIdAt(loc).fold(".")(_.value.toString)
        }
        .mkString(" ")
    }
    lines.traverse_(printer.println)
