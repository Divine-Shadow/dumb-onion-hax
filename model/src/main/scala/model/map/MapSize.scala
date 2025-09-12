package com.crib.bills.dom6maps
package model.map

/** Square map side length. Must be positive. */
final case class MapSize private (value: Int) extends AnyVal

object MapSize:
  def from(value: Int): Either[Throwable, MapSize] =
    if value > 0 then Right(MapSize(value))
    else Left(IllegalArgumentException("MapSize must be positive"))
