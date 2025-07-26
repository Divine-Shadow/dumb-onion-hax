package com.crib.bills.dom6maps.model.map

sealed trait MapDirective

final case class MapWidth(value: Int) extends AnyVal
final case class MapHeight(value: Int) extends AnyVal

final case class Dom2Title(value: String) extends MapDirective
final case class ImageFile(value: String) extends MapDirective
final case class MapSize(width: MapWidth, height: MapHeight) extends MapDirective
final case class DomVersion(value: Int) extends MapDirective
case object HWrapAround extends MapDirective
case object NoDeepCaves extends MapDirective
case object NoDeepChoice extends MapDirective
case object MapNoHide extends MapDirective
final case class ColorComponent(value: Double) extends AnyVal
final case class FloatColor(red: ColorComponent, green: ColorComponent, blue: ColorComponent, alpha: ColorComponent)
final case class MapTextColor(color: FloatColor) extends MapDirective
final case class MapDomColor(red: Int, green: Int, blue: Int, alpha: Int) extends MapDirective

import com.crib.bills.dom6maps.model.Nation
final case class AllowedPlayer(nation: Nation) extends MapDirective
final case class SpecStart(nation: Nation, province: Int) extends MapDirective
final case class Terrain(province: Int, mask: Int) extends MapDirective
final case class LandName(province: Int, name: String) extends MapDirective
