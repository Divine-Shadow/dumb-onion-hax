package com.crib.bills.dom6maps
package model.map

final case class MapUploadConfig(mapSize: MapSizePixels)
final case class MapUploadRequest(config: MapUploadConfig, map: String)
