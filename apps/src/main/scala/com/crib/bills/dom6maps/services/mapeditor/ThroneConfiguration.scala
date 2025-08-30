package com.crib.bills.dom6maps
package apps.services.mapeditor

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import model.map.{
  FeatureId,
  ProvinceLocation,
  ThronePlacement,
  ThroneLevel,
  XCell,
  YCell
}

final case class ThroneConfiguration(overrides: Vector[ThronePlacement]) derives ConfigReader

object ThroneConfiguration:
  given ConfigReader[XCell] = ConfigReader[Int].map(XCell(_))
  given ConfigReader[YCell] = ConfigReader[Int].map(YCell(_))
  given ConfigReader[ProvinceLocation] =
    ConfigReader.forProduct2("x", "y")((x: XCell, y: YCell) => ProvinceLocation(x, y))
  given ConfigReader[ThroneLevel] = ConfigReader[Int].map(ThroneLevel(_))
  given ConfigReader[FeatureId] = ConfigReader[Int].map(FeatureId(_))
  given ConfigReader[ThronePlacement] =
    ConfigReader.forProduct4("x", "y", "level", "id")(
      (x: XCell, y: YCell, l: Option[ThroneLevel], id: Option[FeatureId]) =>
        (l, id) match
          case (Some(level), None) => ThronePlacement(ProvinceLocation(x, y), level)
          case (None, Some(fid))   => ThronePlacement(ProvinceLocation(x, y), fid)
          case _ =>
            throw new IllegalArgumentException("Specify either level or id for throne placement")
    )

