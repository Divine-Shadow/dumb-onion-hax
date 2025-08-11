package com.crib.bills.dom6maps
package apps.services.mapeditor

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import java.nio.file.Path as NioPath

final case class PathsConfig(source: NioPath, dest: NioPath) derives ConfigReader
