package com.crib.bills.dom6maps
package model.map

sealed trait MapDirective

final case class MapWidth(value: Int) extends AnyVal
final case class MapHeight(value: Int) extends AnyVal

final case class MapWidthPixels(value: Int) extends AnyVal
final case class MapHeightPixels(value: Int) extends AnyVal

final case class ProvinceSize(width: MapWidth, height: MapHeight)

final case class MapSizePixels(width: MapWidthPixels, height: MapHeightPixels) extends MapDirective:
  def toProvinceSize: ProvinceSize =
    ProvinceSize(
      MapWidth(width.value / 256),
      MapHeight(height.value / 160)
    )

final case class Dom2Title(value: String) extends MapDirective
final case class ImageFile(value: String) extends MapDirective
final case class WinterImageFile(value: String) extends MapDirective
final case class DomVersion(value: Int) extends MapDirective
final case class Description(value: String) extends MapDirective
case object WrapAround extends MapDirective
case object HWrapAround extends MapDirective
case object VWrapAround extends MapDirective
case object NoWrapAround extends MapDirective
case object NoDeepCaves extends MapDirective
case object NoDeepChoice extends MapDirective
case object MapNoHide extends MapDirective
final case class ColorComponent(value: Double) extends AnyVal
final case class FloatColor(red: ColorComponent, green: ColorComponent, blue: ColorComponent, alpha: ColorComponent)
final case class MapTextColor(color: FloatColor) extends MapDirective
final case class MapDomColor(red: Int, green: Int, blue: Int, alpha: Int) extends MapDirective

import com.crib.bills.dom6maps.model.{Nation, ProvinceId, BorderFlag}
final case class AllowedPlayer(nation: Nation) extends MapDirective
final case class SpecStart(nation: Nation, province: ProvinceId) extends MapDirective
final case class Pb(x: Int, y: Int, length: Int, province: ProvinceId) extends MapDirective
final case class ProvincePixels(x: Int, y: Int, length: Int, province: ProvinceId) extends MapDirective
final case class Terrain(province: ProvinceId, mask: Int) extends MapDirective
final case class LandName(province: ProvinceId, name: String) extends MapDirective
final case class Gate(a: ProvinceId, b: ProvinceId) extends MapDirective
final case class Neighbour(a: ProvinceId, b: ProvinceId) extends MapDirective
final case class NeighbourSpec(a: ProvinceId, b: ProvinceId, border: BorderFlag) extends MapDirective
final case class Comment(value: String) extends MapDirective
