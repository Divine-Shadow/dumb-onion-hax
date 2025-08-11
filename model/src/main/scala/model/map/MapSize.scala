package com.crib.bills.dom6maps
package model.map

/** Square map side length. Must be positive and odd. */
final case class MapSize private (value: Int) extends AnyVal

object MapSize:
  def from(value: Int): Either[Throwable, MapSize] =
    if value > 0 && value % 2 == 1 then Right(MapSize(value))
    else Left(IllegalArgumentException("MapSize must be positive and odd"))
