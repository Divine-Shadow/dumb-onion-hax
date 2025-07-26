package com.crib.bills.dom6maps

import cats.effect.{IO, Sync}
import cats.effect.unsafe.implicits.global
import com.crib.bills.dom6maps.model.{BorderFlag, Nation, ProvinceId}
import com.crib.bills.dom6maps.model.map.*
import fs2.Stream
import org.scalacheck.Properties
import java.nio.charset.StandardCharsets
import scala.io.Source

object MapFileParserSpec extends Properties("MapFileParser") {
  private val parsed =
    MapFileParser
      .parse[IO]
      .apply(Stream.emits(Source.fromFile("data/test-map.map").mkString.getBytes(StandardCharsets.UTF_8)).covary[IO])
      .compile
      .toVector
      .unsafeRunSync()

  property("parses sample directives") =
    parsed == Vector(
      Dom2Title("Sample Map"),
      ImageFile("sample.tga"),
      MapSize(MapWidth(100), MapHeight(100)),
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
      AllowedPlayer(Nation.Xibalba_Early),
      SpecStart(Nation.Xibalba_Early, ProvinceId(1)),
      Terrain(ProvinceId(1), 264),
      LandName(ProvinceId(1), "Province One"),
      Neighbour(ProvinceId(1), ProvinceId(2)),
      NeighbourSpec(ProvinceId(1), ProvinceId(2), BorderFlag.MountainPass)
    )
}
