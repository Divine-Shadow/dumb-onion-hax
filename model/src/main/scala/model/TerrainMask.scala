package com.crib.bills.dom6maps
package model

final case class TerrainMask(value: Long) extends AnyVal:
  def withFlag(flag: TerrainFlag): TerrainMask =
    TerrainMask(value | flag.mask)
  def withoutFlag(flag: TerrainFlag): TerrainMask =
    TerrainMask(value & ~flag.mask)
  def hasFlag(flag: TerrainFlag): Boolean =
    (value & flag.mask) != 0L
