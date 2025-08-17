package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.instances.either.*
import fs2.io.file.Path
import weaver.SimpleIOSuite
import model.map.{
  MapFileParser,
  MapState,
  MapDirective,
  Neighbour,
  NeighbourSpec,
  MapDirectiveCodecs
}
import model.map.MapDirectiveCodecs.Encoder
import fs2.Stream
import WrapSeverService.{severVertically, severHorizontally}

import java.nio.file.Files

object MapWriterRoundTripSpec extends SimpleIOSuite:
  type EC[A] = Either[Throwable, A]

  private def adjacencyDirectives(ds: Vector[MapDirective]) =
    ds.collect { case n: Neighbour => n; case s: NeighbourSpec => s }

  test("round-trip MapWriter with plain neighbours") {
    val writer = new MapWriterImpl[IO]
    for
      state <- MapState.fromDirectives(MapFileParser.parseFile[IO](Path("data/five-by-twelve.map")))
      severed = severVertically(state)
      encoded = Encoder[MapState].encode(severed)
      expectedState <- MapState.fromDirectives(Stream.emits(encoded).covary[IO])
      expectedAdj = adjacencyDirectives(encoded)
      tmp <- IO(Files.createTempFile("mapwriter", ".map")).map(Path.fromNioPath)
      _ <- writer.write[EC](severed, tmp).flatMap(IO.fromEither)
      parsed <- MapFileParser.parseFile[IO](tmp).compile.toVector
      roundTripped <- MapState.fromDirectives(MapFileParser.parseFile[IO](tmp))
      actualAdj = adjacencyDirectives(parsed)
    yield expect.all(
      actualAdj == expectedAdj,
      roundTripped.adjacency == expectedState.adjacency,
      roundTripped.borders == expectedState.borders
    )
  }

  test("round-trip MapWriter with neighbourspec flags") {
    val writer = new MapWriterImpl[IO]
    for
      state <- MapState.fromDirectives(MapFileParser.parseFile[IO](Path("data/test-map.map")))
      severed = severHorizontally(state)
      encoded = Encoder[MapState].encode(severed)
      expectedState <- MapState.fromDirectives(Stream.emits(encoded).covary[IO])
      expectedAdj = adjacencyDirectives(encoded)
      tmp <- IO(Files.createTempFile("mapwriter", ".map")).map(Path.fromNioPath)
      _ <- writer.write[EC](severed, tmp).flatMap(IO.fromEither)
      parsed <- MapFileParser.parseFile[IO](tmp).compile.toVector
      roundTripped <- MapState.fromDirectives(MapFileParser.parseFile[IO](tmp))
      actualAdj = adjacencyDirectives(parsed)
    yield expect.all(
      actualAdj == expectedAdj,
      roundTripped.adjacency == expectedState.adjacency,
      roundTripped.borders == expectedState.borders
    )
  }

  test("MapWriter preserves pass-through directives") {
      val writer = new MapWriterImpl[IO]
      for
        layer <- MapState.fromDirectivesWithPassThrough[
          IO
        ](MapFileParser.parseFile[IO](Path("data/test-map.map")))
        tmp <- IO(Files.createTempFile("mapwriter", ".map")).map(Path.fromNioPath)
        _ <- writer.write[EC](layer, tmp).flatMap(IO.fromEither)
        roundTripped <- MapFileParser.parseFile[IO](tmp).compile.toVector
        pass <- layer.passThrough.compile.toVector
        expected = MapDirectiveCodecs.merge(layer.state, pass)
      yield expect(roundTripped == expected)
  }
