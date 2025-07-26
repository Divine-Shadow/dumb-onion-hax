package com.crib.bills.dom6maps
package model

enum MagicType(val mask: Int):
  case Fire   extends MagicType(8192)
  case Air    extends MagicType(16384)
  case Water  extends MagicType(32768)
  case Earth  extends MagicType(65536)
  case Astral extends MagicType(131072)
  case Death  extends MagicType(262144)
  case Nature extends MagicType(524288)
  case Blood  extends MagicType(1048576)
  case Holy   extends MagicType(2097152)
