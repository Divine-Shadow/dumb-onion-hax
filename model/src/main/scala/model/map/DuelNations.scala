package com.crib.bills.dom6maps
package model.map

import model.Nation

final case class SurfaceNation(value: Nation) extends AnyVal
final case class UndergroundNation(value: Nation) extends AnyVal
final case class DuelNations(surface: SurfaceNation, underground: UndergroundNation)
