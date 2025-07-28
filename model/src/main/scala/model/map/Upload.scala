package com.crib.bills.dom6maps
package model.map

final case class MapUploadConfig(mapSize: MapSize)
final case class MapUploadRequest(config: MapUploadConfig, map: String)
