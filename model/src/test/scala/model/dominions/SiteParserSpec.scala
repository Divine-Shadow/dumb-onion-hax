package com.crib.bills.dom6maps
package model.dominions

import cats.effect.IO
import fs2.Stream
import java.nio.charset.StandardCharsets
import weaver.SimpleIOSuite

object SiteParserSpec extends SimpleIOSuite:
  test("parses a site block") {
    val input =
      """The Silver Throne(1364)
        |  com1: 100
        |  domspread: 2
        |  gold: 200
        |  level: 0
        |  loc: 16607
        |  path: Holy
        |  rarity: 12
        |  sprite: 95
        |  throne: 1
        |
        |""".stripMargin

    val result = Stream
      .emits(input.getBytes(StandardCharsets.UTF_8))
      .through(SiteParser.parse[IO])
      .compile
      .toList

    result.map { sites =>
      val expected = SiteParser.Site(
        SiteParser.SiteName("The Silver Throne"),
        SiteParser.SiteNumber(1364),
        SiteParser.Rarity(12),
        SiteParser.MagicPath("Holy"),
        SiteParser.IsThrone(true)
      )
      expect(sites == List(expected))
    }
  }
