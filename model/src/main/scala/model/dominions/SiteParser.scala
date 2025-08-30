package com.crib.bills.dom6maps
package model.dominions

import cats.effect.Sync
import fs2.{Pipe, Pull, Stream}
import fs2.io.file.{Files, Path}
import fs2.text

object SiteParser:
  final case class SiteName(value: String) extends AnyVal
  final case class SiteNumber(value: Int) extends AnyVal
  final case class Rarity(value: Int) extends AnyVal
  final case class MagicPath(value: String) extends AnyVal
  final case class Site(name: SiteName, number: SiteNumber, rarity: Rarity, path: MagicPath)

  def parseFile[F[_]: Sync](path: Path)(using Files[F]): Stream[F, Site] =
    Files[F].readAll(path).through(parse)

  def parse[F[_]: Sync]: Pipe[F, Byte, Site] =
    _.through(text.utf8.decode)
      .through(text.lines)
      .through(lines)

  private def lines[F[_]]: Pipe[F, String, Site] = in =>
    def go(s: Stream[F, String], acc: Partial): Pull[F, Site, Unit] =
      s.pull.uncons1.flatMap {
        case Some((line, tail)) =>
          val trimmed = line.trim
          if trimmed.isEmpty then
            acc.toSite match
              case Some(site) => Pull.output1(site) >> go(tail, Partial())
              case None       => go(tail, Partial())
          else
            go(tail, Partial.update(acc, trimmed))
        case None =>
          acc.toSite match
            case Some(site) => Pull.output1(site)
            case None       => Pull.done
      }
    go(in, Partial()).stream

  private final case class Partial(
      name: Option[SiteName] = None,
      number: Option[SiteNumber] = None,
      rarity: Option[Rarity] = None,
      path: Option[MagicPath] = None
  ):
    def toSite: Option[Site] =
      for
        n <- name
        num <- number
        r <- rarity
        p <- path
      yield Site(n, num, r, p)

  private object Partial:
    private val namePattern = raw"^(.*)\((\d+)\)$$".r
    private val pathPattern = raw"^path:\s*(.*)$$".r
    private val rarityPattern = raw"^rarity:\s*(\d+)$$".r

    def apply(): Partial = new Partial()

    def update(partial: Partial, line: String): Partial =
      line match
        case namePattern(n, num) =>
          partial.copy(name = Some(SiteName(n.trim)), number = Some(SiteNumber(num.toInt)))
        case pathPattern(p) =>
          partial.copy(path = Some(MagicPath(p.trim)))
        case rarityPattern(r) =>
          partial.copy(rarity = Some(Rarity(r.toInt)))
        case _ => partial
