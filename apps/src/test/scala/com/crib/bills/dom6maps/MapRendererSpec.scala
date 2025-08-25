package com.crib.bills.dom6maps

import cats.effect.IO
import cats.Show
import fs2.Stream
import java.nio.charset.StandardCharsets
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers
import model.*
import model.map.*
import Arbitraries.given
import Renderer.*

object MapRendererSpec extends SimpleIOSuite with Checkers:
  given Show[MapDirective] = Show.show(_.toString)
  private def parseOne(line: String): IO[Option[MapDirective]] =
    MapFileParser
      .parse[IO]
      .apply(Stream.emits((line + "\n").getBytes(StandardCharsets.UTF_8)).covary[IO])
      .compile
      .last

  test("render round trip") {
    forall { (directive: MapDirective) =>
      parseOne(directive.render).map(result => expect(result.contains(directive)))
    }
  }

  test("sample rendering") {
    val e1 = expect(Dom2Title("foo").render == "#dom2title foo")
    val e2 = expect(ImageFile("bar.tga").render == "#imagefile bar.tga")
    val e3 = expect(WinterImageFile("baz.tga").render == "#winterimagefile baz.tga")
    val e4 = expect(MapSizePixels(MapWidthPixels(1), MapHeightPixels(2)).render == "#mapsize 1 2")
    val e5 = expect(PlaneName("p").render == "#planename p")
    val e6 = expect(Description("d").render == "#description \"d\"")
    val e7 = expect(LandName(ProvinceId(1), "name").render == "#landname 1 \"name\"")
    val e8 = expect(Gate(ProvinceId(1), ProvinceId(2)).render == "#gate 1 2")
    val e9 = expect(Pb(1, 2, 3, ProvinceId(4)).render == "#pb 1 2 3 4")
    val e10 = expect(SetLand(ProvinceId(1)).render == "#setland 1")
    val e11 = expect(Feature(FeatureId(2)).render == "#feature 2")
    val e12 = expect(ProvinceFeature(ProvinceId(1), FeatureId(2)).render == "#feature 1 2")
    val e13 = expect(Comment("note").render == "--note")
    IO.pure(e1 and e2 and e3 and e4 and e5 and e6 and e7 and e8 and e9 and e10 and e11 and e12 and e13)
  }
