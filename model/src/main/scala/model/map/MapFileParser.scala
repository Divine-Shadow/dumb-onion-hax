package com.crib.bills.dom6maps
package model.map

import cats.effect.Sync
import fs2.{Pipe, Stream}
import fs2.io.file.{Files, Path}
import fs2.text
import com.crib.bills.dom6maps.model.{BorderFlag, Nation, ProvinceId}

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

  private val Dom2TitleRegex     = """#dom2title\s+(.+)""".r
  private val ImageFileRegex     = """#imagefile\s+(.+)""".r
  private val MapSizeRegex       = """#mapsize\s+(\d+)\s+(\d+)""".r
  private val DomVersionRegex    = """#domversion\s+(\d+)""".r
  private val MapTextColRegex    = """#maptextcol\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)""".r
  private val MapDomColRegex     = """#mapdomcol\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)""".r
  private val AllowedPlayerRegex = """#allowedplayer\s+(\d+)""".r
  private val SpecStartRegex     = """#specstart\s+(\d+)\s+(\d+)""".r
  private val TerrainRegex       = """#terrain\s+(\d+)\s+(\d+)""".r
  private val LandNameRegex      = raw"""#landname\s+(\d+)\s+"([^"]+)""".r
  private val NeighbourRegex     = """#neighbour\s+(\d+)\s+(\d+)""".r
  private val NeighbourSpecRegex = """#neighbourspec\s+(\d+)\s+(\d+)\s+(\d+)""".r

  private def parseLine(line: String): Option[MapDirective] =
    line match
      case Dom2TitleRegex(t)    => Some(Dom2Title(t))
      case ImageFileRegex(f)    => Some(ImageFile(f))
      case MapSizeRegex(w, h)   => Some(MapSize(MapWidth(w.toInt), MapHeight(h.toInt)))
      case DomVersionRegex(v)   => Some(DomVersion(v.toInt))
      case "#hwraparound"       => Some(HWrapAround)
      case "#nodeepcaves"       => Some(NoDeepCaves)
      case "#nodeepchoice"      => Some(NoDeepChoice)
      case "#mapnohide"         => Some(MapNoHide)
      case MapTextColRegex(r,g,b,a) =>
        Some(
          MapTextColor(
            FloatColor(
              ColorComponent(r.toDouble),
              ColorComponent(g.toDouble),
              ColorComponent(b.toDouble),
              ColorComponent(a.toDouble)
            )
          )
        )
      case MapDomColRegex(r,g,b,a) =>
        Some(MapDomColor(r.toInt, g.toInt, b.toInt, a.toInt))
      case AllowedPlayerRegex(n) =>
        Nation.values.find(_.id == n.toInt).map(AllowedPlayer.apply)
      case SpecStartRegex(n, p) =>
        Nation.values.find(_.id == n.toInt).map(SpecStart(_, ProvinceId(p.toInt)))
      case TerrainRegex(p, m)   => Some(Terrain(ProvinceId(p.toInt), m.toInt))
      case LandNameRegex(p, n)  => Some(LandName(ProvinceId(p.toInt), n))
      case NeighbourRegex(a, b) => Some(Neighbour(ProvinceId(a.toInt), ProvinceId(b.toInt)))
      case NeighbourSpecRegex(a, b, flg) =>
        BorderFlag.values
          .find(_.mask == flg.toInt)
          .map(NeighbourSpec(ProvinceId(a.toInt), ProvinceId(b.toInt), _))
      case _ => None

