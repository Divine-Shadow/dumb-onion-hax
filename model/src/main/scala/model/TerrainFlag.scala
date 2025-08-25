package com.crib.bills.dom6maps
package model

enum TerrainFlag(val mask: Int):
  case Plains            extends TerrainFlag(0)
  case SmallProvince     extends TerrainFlag(1)
  case LargeProvince     extends TerrainFlag(2)
  case Sea               extends TerrainFlag(4)
  case FreshWater        extends TerrainFlag(8)
  case Highlands         extends TerrainFlag(16)
  case Swamp             extends TerrainFlag(32)
  case Waste             extends TerrainFlag(64)
  case Forest            extends TerrainFlag(128)
  case Farm              extends TerrainFlag(256)
  case NoStart           extends TerrainFlag(512)
  case ManySites         extends TerrainFlag(1024)
  case DeepSea           extends TerrainFlag(2048)
  case Cave              extends TerrainFlag(4096)
  case Mountains         extends TerrainFlag(8388608)
  case GoodThrone        extends TerrainFlag(33554432)
  case GoodStart         extends TerrainFlag(67108864)
  case BadThrone         extends TerrainFlag(134217728)
  case Warmer            extends TerrainFlag(1073741824)
  case Colder            extends TerrainFlag(-2147483648)

object TerrainFlag:
  val Throne: TerrainFlag = TerrainFlag.GoodStart
