package com.crib.bills.dom6maps
package apps.services.mapeditor

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import model.ProvinceId
import model.map.{ThronePlacement, ThroneLevel}

final case class ThroneConfiguration(overrides: Vector[ThronePlacement]) derives ConfigReader

object ThroneConfiguration:
  given ConfigReader[ProvinceId] = ConfigReader[Int].map(ProvinceId(_))
  given ConfigReader[ThroneLevel] = ConfigReader[Int].map(ThroneLevel(_))
  given ConfigReader[ThronePlacement] =
    ConfigReader.forProduct2("province", "level")((p: ProvinceId, l: ThroneLevel) => ThronePlacement(p, l))
