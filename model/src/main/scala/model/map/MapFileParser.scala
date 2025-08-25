package com.crib.bills.dom6maps
package model.map

import cats.effect.Sync
import fs2.{Pipe, Stream}
import fs2.io.file.{Files, Path}
import fs2.text
import com.crib.bills.dom6maps.model.{BorderFlag, Nation, ProvinceId}
import fastparse.*
import fastparse.NoWhitespace.*
import scala.math.BigInt

object MapFileParser:
  def parseFile[Sequencer[_]: Sync](path: Path)(using Files[Sequencer]): Stream[Sequencer, MapDirective] =
    Files[Sequencer].readAll(path).through(parse)

  final case class UnmappedLine(line: String) extends Exception(s"Unmapped line: $line")

  def parse[Sequencer[_]: Sync]: Pipe[Sequencer, Byte, MapDirective] =
    _.through(text.utf8.decode)
      .through(text.lines)
      .map(_.trim)
      .filter(_.nonEmpty)
      .evalMap(parseLine[Sequencer])

  private def ws[$: P]: P[Unit] = P(CharIn(" \t").rep(1))
  private def int[$: P]: P[Int] =
    P(CharIn("0-9").rep(1).!.map(_.toInt))
  private def long[$: P]: P[Long] =
    P(CharIn("0-9").rep(1).!)
      .map(BigInt(_))
      .filter(_.isValidLong)
      .map(_.toLong)
  private def dbl[$: P]: P[Double] = P(CharIn("0-9.").rep(1).!.map(_.toDouble))
  private def rest[$: P]: P[String] = P(CharsWhile(_ != '\n').!.map(_.trim))
  private def quoted[$: P]: P[String] = P("\"" ~/ CharsWhile(_ != '"').! ~ "\"")

  private def dom2TitleP[$: P]: P[Option[MapDirective]] =
    P("#dom2title" ~ ws ~ rest).map(t => Some(Dom2Title(t)))

  private def imageFileP[$: P]: P[Option[MapDirective]] =
    P("#imagefile" ~ ws ~ rest).map(f => Some(ImageFile(f)))

  private def winterImageFileP[$: P]: P[Option[MapDirective]] =
    P("#winterimagefile" ~ ws ~ rest).map(f => Some(WinterImageFile(f)))

  private def mapSizeP[$: P]: P[Option[MapDirective]] =
    P("#mapsize" ~ ws ~ int ~ ws ~ int).map { case (w, h) =>
      Some(MapSizePixels(MapWidthPixels(w), MapHeightPixels(h)))
    }

  private def domVersionP[$: P]: P[Option[MapDirective]] =
    P("#domversion" ~ ws ~ int).map(v => Some(DomVersion(v)))

  private def planeNameP[$: P]: P[Option[MapDirective]] =
    P("#planename" ~ ws ~ rest).map(n => Some(PlaneName(n)))

  private def descriptionP[$: P]: P[Option[MapDirective]] =
    P("#description" ~ ws ~ quoted).map(d => Some(Description(d)))

  private def wrapAroundP[$: P]: P[Option[MapDirective]] =
    P("#wraparound").map(_ => Some(WrapAround))

  private def hwrapAroundP[$: P]: P[Option[MapDirective]] =
    P("#hwraparound").map(_ => Some(HWrapAround))

  private def vwrapAroundP[$: P]: P[Option[MapDirective]] =
    P("#vwraparound").map(_ => Some(VWrapAround))

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

  private def saildistP[$: P]: P[Option[MapDirective]] =
    P("#saildist" ~ ws ~ int).map(d => Some(SailDist(d)))

  private def featuresP[$: P]: P[Option[MapDirective]] =
    P("#features" ~ ws ~ int).map(f => Some(Features(f)))

  private def allowedPlayerP[$: P]: P[Option[MapDirective]] =
    P("#allowedplayer" ~ ws ~ int).map { n =>
      Nation.byId.get(n).map(AllowedPlayer.apply)
    }

  private def specStartP[$: P]: P[Option[MapDirective]] =
    P("#specstart" ~ ws ~ int ~ ws ~ int).map { case (n, p) =>
      Nation.byId.get(n).map(SpecStart(_, ProvinceId(p)))
    }

  private def pbP[$: P]: P[Option[MapDirective]] =
    P("#pb" ~ ws ~ int ~ ws ~ int ~ ws ~ int ~ ws ~ int).map {
      case (x, y, len, p) => Some(Pb(x, y, len, ProvinceId(p)))
    }

  private def terrainP[$: P]: P[Option[MapDirective]] =
    P("#terrain" ~ ws ~ int ~ ws ~ long).map { case (p, m) =>
      Some(Terrain(ProvinceId(p), m))
    }

  private def landnameP[$: P]: P[Option[MapDirective]] =
    P("#landname" ~ ws ~ int ~ ws ~ quoted).map { case (p, n) =>
      Some(LandName(ProvinceId(p), n))
    }

  private def setlandP[$: P]: P[Option[MapDirective]] =
    P("#setland" ~ ws ~ int).map(p => Some(SetLand(ProvinceId(p))))

  private def provinceFeatureP[$: P]: P[Option[MapDirective]] =
    P("#feature" ~ ws ~ int ~ ws ~ int).map { case (p, f) =>
      Some(ProvinceFeature(ProvinceId(p), FeatureId(f)))
    }

  private def featureP[$: P]: P[Option[MapDirective]] =
    P("#feature" ~ ws ~ int).map(f => Some(Feature(FeatureId(f))))

  private def gateP[$: P]: P[Option[MapDirective]] =
    P("#gate" ~ ws ~ int ~ ws ~ int).map { case (a, b) =>
      Some(Gate(ProvinceId(a), ProvinceId(b)))
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

  private def commentP[$: P]: P[Option[MapDirective]] =
    P("--" ~ rest.?).map(t => Some(Comment(t.getOrElse(""))))

  private def directive[$: P]: P[Option[MapDirective]] = P(
    dom2TitleP |
      imageFileP |
      winterImageFileP |
      mapSizeP |
      domVersionP |
      planeNameP |
      descriptionP |
      wrapAroundP |
      hwrapAroundP |
      vwrapAroundP |
      nowrapAroundP |
      nodeepcavesP |
      nodeepchoiceP |
      mapnohideP |
      maptextcolP |
      mapdomcolP |
      saildistP |
      featuresP |
      allowedPlayerP |
      specStartP |
      pbP |
      terrainP |
      landnameP |
      setlandP |
      provinceFeatureP |
      featureP |
      gateP |
      neighbourP |
      neighbourspecP |
      commentP
  )

  private def parseLine[F[_]: Sync](line: String): F[MapDirective] =
    fastparse.parse(line, p => directive(using p)) match
      case Parsed.Success(Some(value), _) => Sync[F].pure(value)
      case _                              => Sync[F].raiseError(UnmappedLine(line))

