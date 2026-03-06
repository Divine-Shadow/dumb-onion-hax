package com.crib.bills.dom6maps
package apps.services.mapeditor

import model.ProvinceId
import model.map.{ProvinceLocation, ProvinceLocations}

trait ScenarioCoordinateResolverService:
  def resolve(
      locations: ProvinceLocations,
      coordinate: ProvinceLocation,
      label: String
  ): Either[Throwable, ProvinceId]

class ScenarioCoordinateResolverServiceImpl extends ScenarioCoordinateResolverService:
  override def resolve(
      locations: ProvinceLocations,
      coordinate: ProvinceLocation,
      label: String
  ): Either[Throwable, ProvinceId] =
    locations
      .provinceIdAt(coordinate)
      .toRight(
        IllegalArgumentException(
          s"Could not resolve $label at (${coordinate.x.value}, ${coordinate.y.value}) to a province"
        )
      )
