package com.crib.bills.dom6maps
package model.map

import cats.effect.Sync
import fs2.{Pipe, Stream}
import fs2.io.file.{Files, Path}
import fs2.text
import com.crib.bills.dom6maps.model.{BorderFlag, Nation, ProvinceId}
import fastparse._
import fastparse.NoWhitespace._

object MapFileParser:
  def parseFile[Sequencer[_]: Sync](path: Path)(using Files[Sequencer]): Stream[Sequencer, MapDirective] =
    Files[Sequencer].readAll(path).through(parse)

  def parse[Sequencer[_]: Sync]: Pipe[Sequencer, Byte, MapDirective] =
    _.through(text.utf8.decode)
      .through(text.lines)
      .map(_.trim)
      .filter(line => line.nonEmpty && !line.startsWith("--"))
      .map(parseLine)
      .collect { case Some(directive) => directive }

  private def ws[$: P]: P[Unit] = P(CharIn(" \t").rep(1))
  private def int[$: P]: P[Int] = P(CharIn("0-9").rep(1).!.map(_.toInt))
  private def dbl[$: P]: P[Double] = P(CharIn("0-9.").rep(1).!.map(_.toDouble))
  private def rest[$: P]: P[String] = P(CharsWhile(_ != '\n').!.map(_.trim))
  private def quoted[$: P]: P[String] = P("\"" ~/ CharsWhile(_ != '"').! ~ "\"")

  private def dom2TitleP[$: P]: P[Option[MapDirective]] =
    P("#dom2title" ~ ws ~ rest).map(t => Some(Dom2Title(t)))

  private def imageFileP[$: P]: P[Option[MapDirective]] =
    P("#imagefile" ~ ws ~ rest).map(f => Some(ImageFile(f)))

  private def mapSizeP[$: P]: P[Option[MapDirective]] =
    P("#mapsize" ~ ws ~ int ~ ws ~ int).map { case (w, h) =>
      Some(MapSize(MapWidth(w), MapHeight(h)))
    }

  private def domVersionP[$: P]: P[Option[MapDirective]] =
    P("#domversion" ~ ws ~ int).map(v => Some(DomVersion(v)))

  private def hwrapAroundP[$: P]: P[Option[MapDirective]] =
    P("#hwraparound").map(_ => Some(HWrapAround))

  private def nowrapAroundP[$: P]: P[Option[MapDirective]] =
    P("#nowraparound").map(_ => Some(NoWrapAround))

  private def nodeepcavesP[$: P]: P[Option[MapDirective]] =
    P("#nodeepcaves").map(_ => Some(NoDeepCaves))

  private def nodeepchoiceP[$: P]: P[Option[MapDirective]] =
    P("#nodeepchoice").map(_ => Some(NoDeepChoice))

  private def mapnohideP[$: P]: P[Option[MapDirective]] =
    P("#mapnohide").map(_ => Some(MapNoHide))

  private def maptextcolP[$: P]: P[Option[MapDirective]] =
    P("#maptextcol" ~ ws ~ dbl ~ ws ~ dbl ~ ws ~ dbl ~ ws ~ dbl).map {
      case (r, g, b, a) =>
        Some(
          MapTextColor(
            FloatColor(
              ColorComponent(r),
              ColorComponent(g),
              ColorComponent(b),
              ColorComponent(a)
            )
          )
        )
    }

  private def mapdomcolP[$: P]: P[Option[MapDirective]] =
    P("#mapdomcol" ~ ws ~ int ~ ws ~ int ~ ws ~ int ~ ws ~ int).map {
      case (r, g, b, a) => Some(MapDomColor(r, g, b, a))
    }

  private def allowedPlayerP[$: P]: P[Option[MapDirective]] =
    P("#allowedplayer" ~ ws ~ int).map { n =>
      Nation.values.find(_.id == n).map(AllowedPlayer.apply)
    }

  private def specStartP[$: P]: P[Option[MapDirective]] =
    P("#specstart" ~ ws ~ int ~ ws ~ int).map { case (n, p) =>
      Nation.values.find(_.id == n).map(SpecStart(_, ProvinceId(p)))
    }

  private def terrainP[$: P]: P[Option[MapDirective]] =
    P("#terrain" ~ ws ~ int ~ ws ~ int).map { case (p, m) =>
      Some(Terrain(ProvinceId(p), m))
    }

  private def landnameP[$: P]: P[Option[MapDirective]] =
    P("#landname" ~ ws ~ int ~ ws ~ quoted).map { case (p, n) =>
      Some(LandName(ProvinceId(p), n))
    }

  private def neighbourP[$: P]: P[Option[MapDirective]] =
    P("#neighbour" ~ ws ~ int ~ ws ~ int).map { case (a, b) =>
      Some(Neighbour(ProvinceId(a), ProvinceId(b)))
    }

  private def neighbourspecP[$: P]: P[Option[MapDirective]] =
    P("#neighbourspec" ~ ws ~ int ~ ws ~ int ~ ws ~ int).map {
      case (a, b, flg) =>
        BorderFlag.values
          .find(_.mask == flg)
          .map(NeighbourSpec(ProvinceId(a), ProvinceId(b), _))
    }

  private def directive[$: P]: P[Option[MapDirective]] = P(
    dom2TitleP |
      imageFileP |
      mapSizeP |
      domVersionP |
      hwrapAroundP |
      nowrapAroundP |
      nodeepcavesP |
      nodeepchoiceP |
      mapnohideP |
      maptextcolP |
      mapdomcolP |
      allowedPlayerP |
      specStartP |
      terrainP |
      landnameP |
      neighbourP |
      neighbourspecP
  )

  private def parseLine(line: String): Option[MapDirective] =
    fastparse.parse(line, p => directive(using p)) match
      case Parsed.Success(value, _) => value
      case _                        => None

