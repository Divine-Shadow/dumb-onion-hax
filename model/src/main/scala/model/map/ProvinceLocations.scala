package com.crib.bills.dom6maps
package model.map

import model.ProvinceId

final case class ProvinceLocations private (
    indexByLocation: Map[ProvinceLocation, ProvinceId],
    indexByProvinceId: Map[ProvinceId, ProvinceLocation]
):
  def provinceIdAt(location: ProvinceLocation): Option[ProvinceId] =
    indexByLocation.get(location)

  def locationOf(provinceId: ProvinceId): Option[ProvinceLocation] =
    indexByProvinceId.get(provinceId)

object ProvinceLocations:
  val empty: ProvinceLocations = ProvinceLocations(Map.empty, Map.empty)

  def fromProvinceIdMap(values: Map[ProvinceId, ProvinceLocation]): ProvinceLocations =
    ProvinceLocations(values.map(_.swap), values)

  def fromLocationMap(values: Map[ProvinceLocation, ProvinceId]): ProvinceLocations =
    ProvinceLocations(values, values.map(_.swap))
