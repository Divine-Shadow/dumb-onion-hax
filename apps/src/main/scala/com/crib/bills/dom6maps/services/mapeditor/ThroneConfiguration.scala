package com.crib.bills.dom6maps
package apps.services.mapeditor

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import model.map.{ProvinceLocation, ThronePlacement, ThroneLevel, XCell, YCell}

final case class ThroneConfiguration(overrides: Vector[ThronePlacement]) derives ConfigReader

object ThroneConfiguration:
  given ConfigReader[XCell] = ConfigReader[Int].map(XCell(_))
  given ConfigReader[YCell] = ConfigReader[Int].map(YCell(_))
  given ConfigReader[ProvinceLocation] =
    ConfigReader.forProduct2("x", "y")((x: XCell, y: YCell) => ProvinceLocation(x, y))
  given ConfigReader[ThroneLevel] = ConfigReader[Int].map(ThroneLevel(_))
  given ConfigReader[ThronePlacement] =
    ConfigReader.forProduct3("x", "y", "level")((x: XCell, y: YCell, l: ThroneLevel) => ThronePlacement(ProvinceLocation(x, y), l))
