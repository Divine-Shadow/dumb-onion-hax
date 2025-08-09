package com.crib.bills.dom6maps
package apps.services.map

import cats.effect.Async
import fs2.io.file.{Files, Path}
import fs2.text

trait DirectiveTypesService[Sequencer[_]]:
  def collect(path: Path): Sequencer[Set[String]]

class DirectiveTypesServiceImpl[Sequencer[_]: Async: Files] extends DirectiveTypesService[Sequencer]:
  override def collect(path: Path): Sequencer[Set[String]] =
    Files[Sequencer]
      .readAll(path)
      .through(text.utf8.decode)
      .through(text.lines)
      .map(_.takeWhile(_ != ' '))
      .filter(_.nonEmpty)
      .compile
      .fold(Set.empty[String])(_ + _)
