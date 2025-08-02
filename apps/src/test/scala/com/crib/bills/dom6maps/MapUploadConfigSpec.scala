package com.crib.bills.dom6maps

import cats.effect.IO
import weaver.SimpleIOSuite
import io.circe.syntax.*
import io.circe.parser.decode
import model.map.MapUploadConfig
import apps.McpMapServer.given
import model.map.{MapHeight, MapSize, MapWidth}

object MapUploadConfigSpec extends SimpleIOSuite:
  test("codec round trip") {
    val cfg = MapUploadConfig(MapSize(MapWidth(10), MapHeight(20)))
    val json = cfg.asJson
    IO.pure(expect(decode[MapUploadConfig](json.noSpaces) == Right(cfg)))
  }
