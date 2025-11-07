package com.crib.bills.dom6maps
package apps.services.mapeditor

final case class CaveLayerAvailability(isAvailable: Boolean)

object CaveLayerAvailability:
  val Available: CaveLayerAvailability   = CaveLayerAvailability(true)
  val Unavailable: CaveLayerAvailability = CaveLayerAvailability(false)

  def fromOption[A](maybeLayer: Option[A]): CaveLayerAvailability =
    if maybeLayer.isDefined then Available else Unavailable
