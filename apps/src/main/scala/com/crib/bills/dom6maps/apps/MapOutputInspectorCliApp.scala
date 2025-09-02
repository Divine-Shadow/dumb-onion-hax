package com.crib.bills.dom6maps
package apps

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*
import cats.instances.either.*
import fs2.io.file.{Files, Path}
import pureconfig.*

import services.mapeditor.*
import services.mapeditor.ThroneConfiguration
import model.map.*
import model.{TerrainMask, TerrainFlag, ProvinceId}
import model.dominions.{Feature as DomFeature}

/**
 * Loads the latest generated map and prints throne placement info.
 * - Reports ProvinceFeature thrones (by feature id)
 * - Reports any terrains with the throne mask flag
 */
object MapOutputInspectorCliApp extends IOApp:
  def run(args: List[String]): IO[ExitCode] =
    given Files[IO] = Files.forAsync[IO]
    val loader  = new MapLayerLoaderImpl[IO]
    val finder  = new LatestEditorFinderImpl[IO]

    val cfg     = ConfigSource.file("map-editor-wrap.conf").loadOrThrow[PathsConfig]
    val dest    = Path.fromNioPath(cfg.dest)
    val srcRoot = Path.fromNioPath(cfg.source)
    val throneIds = DomFeature.thrones.map(_.id.value).toSet

    type ErrorOr[A] = Either[Throwable, A]

    val program = for
      latestEC <- finder.mostRecentFolder[ErrorOr](dest)
      latest   <- IO.fromEither(latestEC)
      // Identify the main .map file (not _plane2)
      entries  <- Files[IO].list(latest).compile.toList
      maybeMap  = entries.find(p => p.toString.endsWith(".map") && !p.toString.endsWith("_plane2.map"))
      mapPath  <- maybeMap match
        case Some(p) => IO.pure(p)
        case None    => IO.raiseError(new NoSuchElementException("No .map file found in output folder"))
      layerEC  <- loader.load[ErrorOr](mapPath)
      layer    <- IO.fromEither(layerEC)
      state     = layer.state
      // Reconstruct #setland + #feature sequences from passThrough
      pass      <- layer.passThrough.compile.toVector
      reconstructed =
        pass.foldLeft((Option.empty[ProvinceId], Vector.empty[(ProvinceId, FeatureId)])) {
          case ((current, acc), SetLand(p))      => (Some(p), acc)
          case ((Some(p), acc), Feature(fid))    => (Some(p), acc :+ (p -> fid))
          case ((c, acc), _)                     => (c, acc)
        }._2
      throneFeatures = reconstructed.filter { case (_, fid) => throneIds.contains(fid.value) }
      throneTerrains = state.terrains.filter(t => TerrainMask(t.mask).hasFlag(TerrainFlag.Throne))
      // Load overrides and detect out-of-bounds locations vs provinceLocations
      overrides <- IO {
        val p = java.nio.file.Path.of("throne-override.conf")
        if (java.nio.file.Files.exists(p))
          ConfigSource.file(p).load[ThroneConfiguration].left.map(f => new RuntimeException(f.toString))
        else Right(ThroneConfiguration(Vector.empty))
      }.flatMap(IO.fromEither)
      unresolved = overrides.overrides.filter { tp => state.provinceLocations.provinceIdAt(tp.location).isEmpty }
      _ <- IO.println(s"Output: ${mapPath.toString}")
      _ <- IO.println(s"Feature-based thrones: ${throneFeatures.size}")
      _ <- throneFeatures.traverse_ { case (prov, fid) =>
        val loc = state.provinceLocations.locationOf(prov)
        IO.println(s" - province=${prov.value} at ${loc} featureId=${fid.value}")
      }
      _ <- IO.println(s"Terrain mask 'throne' flags: ${throneTerrains.size}")
      _ <- IO.println(s"Out-of-bounds overrides (vs output map): ${unresolved.size}")
      _ <- unresolved.traverse_ { tp => IO.println(s" - at (${tp.location.x.value},${tp.location.y.value}) ${tp.level.fold("")(l => s"level=${l.value} ")} ${tp.id.fold("")(fid => s"id=${fid.value}")}") }
      // Cross-check against the original source map
      srcLatestEC <- finder.mostRecentFolder[ErrorOr](srcRoot)
      srcLatest   <- IO.fromEither(srcLatestEC)
      srcEntries  <- Files[IO].list(srcLatest).compile.toList
      srcMaybeMap  = srcEntries.find(p => p.toString.endsWith(".map") && !p.toString.endsWith("_plane2.map"))
      srcMap      <- srcMaybeMap match
        case Some(p) => IO.pure(p)
        case None    => IO.raiseError(new NoSuchElementException("No .map file found in source folder"))
      srcLayerEC  <- loader.load[ErrorOr](srcMap)
      srcLayer    <- IO.fromEither(srcLayerEC)
      srcState     = srcLayer.state
      srcUnresolved = overrides.overrides.filter { tp => srcState.provinceLocations.provinceIdAt(tp.location).isEmpty }
      _ <- IO.println(s"Out-of-bounds overrides (vs source map): ${srcUnresolved.size}")
      _ <- srcUnresolved.traverse_ { tp => IO.println(s" - at (${tp.location.x.value},${tp.location.y.value}) ${tp.level.fold("")(l => s"level=${l.value} ")} ${tp.id.fold("")(fid => s"id=${fid.value}")}") }
      _ <- IO.println("Done.")
    yield ExitCode.Success

    program
