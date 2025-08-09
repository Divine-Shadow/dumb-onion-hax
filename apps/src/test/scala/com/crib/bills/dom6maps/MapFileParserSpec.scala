package com.crib.bills.dom6maps

import cats.effect.IO
import com.crib.bills.dom6maps.model.{BorderFlag, Nation, ProvinceId}
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

  private val largeMaskParsedIO =
    MapFileParser
      .parse[IO]
      .apply(Stream.emits("#terrain 1 2147483712\n".getBytes(StandardCharsets.UTF_8)).covary[IO])
      .compile
      .toVector

  test("parses sample directives") {
    parsedIO.map { parsed =>
      expect(
        parsed == Vector(
          Dom2Title("Sample Map"),
          ImageFile("sample.tga"),
          MapSizePixels(MapWidthPixels(100), MapHeightPixels(100)),
          DomVersion(550),
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
          ProvincePixels(1, 2, 3, ProvinceId(1)),
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
    largeMaskParsedIO.map { parsed =>
      expect(parsed == Vector(Terrain(ProvinceId(1), -2147483584)))
    }
  }
