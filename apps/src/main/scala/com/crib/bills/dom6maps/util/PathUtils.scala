package com.crib.bills.dom6maps
package apps.util

import java.nio.file.Path as NioPath

object PathUtils:
  private val winDriveRegex = """^([A-Za-z]):[\\/](.*)$""".r

  private def isWSL: Boolean =
    // Common WSL env markers; avoids reading /proc
    sys.env.contains("WSL_DISTRO_NAME") || sys.env.contains("WSL_INTEROP")

  private def looksLikeWindowsPath(s: String): Boolean =
    s match
      case winDriveRegex(_, _) => true
      case _                   => false

  private def windowsToWslString(s: String): String =
    s match
      case winDriveRegex(drive, rest) =>
        val lower = drive.toLowerCase
        val unixy = rest.replace('\\', '/')
        s"/mnt/$lower/$unixy"
      case _ => s

  def normalizeForWSL(p: NioPath): NioPath =
    val s = p.toString
    if isWSL && looksLikeWindowsPath(s) then NioPath.of(windowsToWslString(s))
    else p
