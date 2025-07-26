package com.crib.bills.dom6maps

import cats.effect.{IO, Sync}
import cats.effect.unsafe.implicits.global
import fs2.Stream
import java.nio.charset.StandardCharsets
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import model.*
import model.map.*
import Arbitraries.given
import Renderer.*

object MapRendererSpec extends Properties("MapRenderer"):
  private def parseOne(line: String): Option[MapDirective] =
    MapFileParser
      .parse[IO]
      .apply(Stream.emits((line + "\n").getBytes(StandardCharsets.UTF_8)).covary[IO])
      .compile
      .last
      .unsafeRunSync()

  property("render round trip") =
    forAll { (directive: MapDirective) =>
      parseOne(directive.render).contains(directive)
    }

  property("sample rendering") =
    Dom2Title("foo").render == "#dom2title foo" &&
    ImageFile("bar.tga").render == "#imagefile bar.tga" &&
    MapSize(MapWidth(1), MapHeight(2)).render == "#mapsize 1 2" &&
    LandName(ProvinceId(1), "name").render == "#landname 1 \"name\""
