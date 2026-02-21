package com.crib.bills.dom6maps
package model.map.generation

import model.{ProvinceId}
import model.map.{Pb, Border, Terrain, ProvinceLocation}

final case class GeneratedGeometry(
    provincePixelRuns: Vector[Pb],
    adjacency: Vector[(ProvinceId, ProvinceId)],
    borders: Vector[Border],
    terrainByProvince: Vector[Terrain],
    provinceCentroids: Map[ProvinceId, ProvinceLocation]
)
