package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import weaver.SimpleIOSuite
import pureconfig.*
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import model.map.{ThronePlacement, ThroneLevel, ProvinceLocation, XCell, YCell}

object ThroneConfigurationSpec extends SimpleIOSuite:
  test("parses overrides config") {
    val contents =
      """overrides = [
        { x = 0, y = 0, level = 2 },
        { x = 4, y = 0, level = 3 }
      ]"""
    for
      file <- IO(Files.createTempFile("throne", ".conf"))
      _ <- IO(Files.writeString(file, contents, StandardCharsets.UTF_8))
      cfg <- IO(ConfigSource.file(file).loadOrThrow[ThroneConfiguration])
      expected = ThroneConfiguration(
        Vector(
          ThronePlacement(ProvinceLocation(XCell(0), YCell(0)), ThroneLevel(2)),
          ThronePlacement(ProvinceLocation(XCell(4), YCell(0)), ThroneLevel(3))
        )
      )
    yield expect(cfg == expected)
  }
