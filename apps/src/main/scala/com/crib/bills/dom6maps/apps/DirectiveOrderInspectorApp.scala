package com.crib.bills.dom6maps
package apps

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*
import fs2.io.file.Path
import model.map.{MapDirective, MapFileParser}

/**
  * Prints the first-seen order of directive types in a given .map file.
  *
  * Usage:
  *   sbt "project apps" "runMain com.crib.bills.dom6maps.apps.DirectiveOrderInspectorApp <path-to-map>"
  */
object DirectiveOrderInspectorApp extends IOApp:
  private def keywordOf(d: MapDirective): String = d match
    case model.map.Dom2Title(_)         => "#dom2title"
    case model.map.ImageFile(_)         => "#imagefile"
    case model.map.WinterImageFile(_)   => "#winterimagefile"
    case model.map.MapSizePixels(_, _)  => "#mapsize"
    case model.map.DomVersion(_)        => "#domversion"
    case model.map.PlaneName(_)         => "#planename"
    case model.map.Description(_)       => "#description"
    case model.map.WrapAround           => "#wraparound"
    case model.map.HWrapAround          => "#hwraparound"
    case model.map.VWrapAround          => "#vwraparound"
    case model.map.NoWrapAround         => "#nowraparound"
    case model.map.NoDeepCaves          => "#nodeepcaves"
    case model.map.NoDeepChoice         => "#nodeepchoice"
    case model.map.MapNoHide            => "#mapnohide"
    case model.map.MapTextColor(_)      => "#maptextcol"
    case model.map.MapDomColor(_,_,_,_) => "#mapdomcol"
    case model.map.SailDist(_)          => "#saildist"
    case model.map.Features(_)          => "#features"
    case model.map.AllowedPlayer(_)     => "#allowedplayer"
    case model.map.SpecStart(_, _)      => "#specstart"
    case model.map.Start(_)             => "#start"
    case model.map.Pb(_,_,_,_)          => "#pb"
    case model.map.Terrain(_, _)        => "#terrain"
    case model.map.LandName(_, _)       => "#landname"
    case model.map.SetLand(_)           => "#setland"
    case model.map.Feature(_)           => "#feature(id)"
    case model.map.ProvinceFeature(_,_) => "#feature(province,id)"
    case model.map.Gate(_, _)           => "#gate"
    case model.map.Neighbour(_, _)      => "#neighbour"
    case model.map.NeighbourSpec(_,_,_) => "#neighbourspec"
    case model.map.Comment(_)           => "--comment"
    case model.map.LineBreak            => "<linebreak>"

  override def run(args: List[String]): IO[ExitCode] =
    val usage = IO.println(
      "Usage: DirectiveOrderInspectorApp <path-to-map>  (example: data/example-map.map)"
    )

    args.headOption match
      case None => usage.as(ExitCode(2))
      case Some(pathStr) =>
        val path = Path(pathStr)
        for
          _ <- IO.println(s"Inspecting directive order in: $pathStr")
          directives <- MapFileParser.parseFile[IO](path).compile.toList
          seen = scala.collection.mutable.LinkedHashSet.empty[String]
          _ = directives.foreach { d =>
            val k = keywordOf(d)
            if !seen.contains(k) then seen.add(k)
          }
          _ <- IO.println("First-seen directive order:")
          _ <- IO.println(seen.mkString("\n"))
          _ <- IO.println(s"\nSummary: unique=${seen.size}, total=${directives.size}")
        yield ExitCode.Success
