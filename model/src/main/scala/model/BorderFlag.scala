package com.crib.bills.dom6maps
package model

/**
  * Bitmask values describing province borders.
  *
  * Mirrors the values accepted by the `#neighbourspec` command.
  */
enum BorderFlag(val mask: Int):
  case Standard    extends BorderFlag(0)
  case MountainPass extends BorderFlag(1)
  case River       extends BorderFlag(2)
  case Impassable  extends BorderFlag(4)
  case Road        extends BorderFlag(8)
  case BridgedRiver extends BorderFlag(10)

object BorderFlag:
  extension (borderFlag: BorderFlag)
    def includes(requiredFlag: BorderFlag): Boolean =
      (borderFlag.mask & requiredFlag.mask) == requiredFlag.mask
