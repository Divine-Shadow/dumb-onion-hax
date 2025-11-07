package com.crib.bills.dom6maps
package apps.services.mapeditor

enum MagicSiteToggle:
  case Enabled, Disabled

object MagicSiteToggle:
  def fromBoolean(value: Boolean): MagicSiteToggle =
    if value then MagicSiteToggle.Enabled else MagicSiteToggle.Disabled

final case class MagicSiteSelection(
    surface: MagicSiteToggle,
    cave: MagicSiteToggle
)

object MagicSiteSelection:
  val Disabled: MagicSiteSelection =
    MagicSiteSelection(MagicSiteToggle.Disabled, MagicSiteToggle.Disabled)
