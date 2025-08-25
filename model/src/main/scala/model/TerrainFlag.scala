package com.crib.bills.dom6maps
package model

enum TerrainFlag(val mask: Long):
  case Plains            extends TerrainFlag(0L)
  case SmallProvince     extends TerrainFlag(1L)
  case LargeProvince     extends TerrainFlag(2L)
  case Sea               extends TerrainFlag(4L)
  case FreshWater        extends TerrainFlag(8L)
  case Highlands         extends TerrainFlag(16L)
  case Swamp             extends TerrainFlag(32L)
  case Waste             extends TerrainFlag(64L)
  case Forest            extends TerrainFlag(128L)
  case Farm              extends TerrainFlag(256L)
  case NoStart           extends TerrainFlag(512L)
  case ManySites         extends TerrainFlag(1024L)
  case DeepSea           extends TerrainFlag(2048L)
  case Cave              extends TerrainFlag(4096L)
  case Mountains         extends TerrainFlag(8388608L)
  case GoodThrone        extends TerrainFlag(33554432L)
  case GoodStart         extends TerrainFlag(67108864L)
  case BadThrone         extends TerrainFlag(134217728L)
  case Warmer            extends TerrainFlag(1073741824L)
  case Colder            extends TerrainFlag(2147483648L)
  case CaveWall          extends TerrainFlag(68719476736L)

object TerrainFlag:
  val Throne: TerrainFlag = TerrainFlag.GoodStart
