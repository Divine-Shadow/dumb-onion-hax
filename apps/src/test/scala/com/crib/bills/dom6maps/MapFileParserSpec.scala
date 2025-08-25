package com.crib.bills.dom6maps

import cats.effect.IO
import com.crib.bills.dom6maps.model.{BorderFlag, Nation, ProvinceId, TerrainFlag}
import com.crib.bills.dom6maps.model.map.*
import fs2.Stream
import java.nio.charset.StandardCharsets
import scala.io.Source
import weaver.SimpleIOSuite

object MapFileParserSpec extends SimpleIOSuite:
  private val parsedIO =
    MapFileParser
      .parse[IO]
      .apply(Stream.emits(Source.fromFile("data/test-map.map").mkString.getBytes(StandardCharsets.UTF_8)).covary[IO])
      .compile
      .toVector

  private val aboveIntMask = Int.MaxValue.toLong + 1
  private val aboveIntMaskParsedIO =
    MapFileParser
      .parse[IO]
      .apply(Stream.emits(s"#terrain 1 $aboveIntMask\n".getBytes(StandardCharsets.UTF_8)).covary[IO])
      .compile
      .toVector

  private val caveWallMaskParsedIO =
    MapFileParser
      .parse[IO]
      .apply(Stream.emits(s"#terrain 1 ${TerrainFlag.CaveWall.mask}\n".getBytes(StandardCharsets.UTF_8)).covary[IO])
      .compile
      .toVector

  private val glamourMask = 1L << 20
  private val glamourMaskParsedIO =
    MapFileParser
      .parse[IO]
      .apply(Stream.emits(s"#terrain 1 $glamourMask\n".getBytes(StandardCharsets.UTF_8)).covary[IO])
      .compile
      .toVector

  private val overflowMaskParsedIO =
    MapFileParser
      .parse[IO]
      .apply(Stream.emits("#terrain 1 9223372036854775808\n".getBytes(StandardCharsets.UTF_8)).covary[IO])
      .compile
      .toVector
      .attempt

  test("parses sample directives") {
    parsedIO.map { parsed =>
      expect(
        parsed == Vector(
          Comment("Minimal test map"),
          Dom2Title("Sample Map"),
          ImageFile("sample.tga"),
          MapSizePixels(MapWidthPixels(100), MapHeightPixels(100)),
          DomVersion(550),
          PlaneName("The Underworld"),
          HWrapAround,
          NoDeepCaves,
          MapNoHide,
          MapTextColor(
            FloatColor(
              ColorComponent(0.10),
              ColorComponent(0.20),
              ColorComponent(0.30),
              ColorComponent(1.00)
            )
          ),
          MapDomColor(1, 2, 3, 4),
          Pb(1, 2, 3, ProvinceId(1)),
          AllowedPlayer(Nation.Xibalba_Early),
          SpecStart(Nation.Xibalba_Early, ProvinceId(1)),
          Terrain(ProvinceId(1), 264),
          LandName(ProvinceId(1), "Province One"),
          Neighbour(ProvinceId(1), ProvinceId(2)),
          NeighbourSpec(ProvinceId(1), ProvinceId(2), BorderFlag.MountainPass)
        )
      )
    }
  }

  test("parses terrain mask above Int.MaxValue") {
    aboveIntMaskParsedIO.map { parsed =>
      expect(parsed == Vector(Terrain(ProvinceId(1), aboveIntMask)))
    }
  }

  test("parses CaveWall terrain flag") {
    caveWallMaskParsedIO.map { parsed =>
      expect(parsed == Vector(Terrain(ProvinceId(1), TerrainFlag.CaveWall.mask)))
    }
  }

  test("parses Glamour magic site mask") {
    glamourMaskParsedIO.map { parsed =>
      expect(parsed == Vector(Terrain(ProvinceId(1), glamourMask)))
    }
  }

  test("fails on terrain mask overflow") {
    overflowMaskParsedIO.map(res => expect(res.isLeft))
  }

  test("fails on unknown directives") {
    MapFileParser
      .parse[IO]
      .apply(Stream.emits("#unknown\n".getBytes(StandardCharsets.UTF_8)).covary[IO])
      .compile
      .toVector
      .attempt
      .map(result => expect(result.isLeft))
  }
