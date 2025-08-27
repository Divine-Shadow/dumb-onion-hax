package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import weaver.SimpleIOSuite
import pureconfig.*
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import model.ProvinceId
import model.map.{ThronePlacement, ThroneLevel}

object ThroneConfigurationSpec extends SimpleIOSuite:
  test("parses overrides config") {
    val contents =
      """overrides = [
        { province = 1, level = 2 },
        { province = 5, level = 3 }
      ]"""
    for
      file <- IO(Files.createTempFile("throne", ".conf"))
      _ <- IO(Files.writeString(file, contents, StandardCharsets.UTF_8))
      cfg <- IO(ConfigSource.file(file).loadOrThrow[ThroneConfiguration])
      expected = ThroneConfiguration(
        Vector(
          ThronePlacement(ProvinceId(1), ThroneLevel(2)),
          ThronePlacement(ProvinceId(5), ThroneLevel(3))
        )
      )
    yield expect(cfg == expected)
  }
