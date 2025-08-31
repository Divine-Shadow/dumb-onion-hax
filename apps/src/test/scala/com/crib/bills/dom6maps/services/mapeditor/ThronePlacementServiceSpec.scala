package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.syntax.all.*
import weaver.SimpleIOSuite
import model.ProvinceId
import model.{TerrainFlag, TerrainMask}
import model.map.{
  FeatureId,
  MapState,
  ProvinceFeature,
  Terrain,
  ThronePlacement,
  ThroneLevel,
  ProvinceLocation,
  ProvinceLocations,
  XCell,
  YCell
}
import model.dominions.{Feature as DomFeature}

object ThronePlacementServiceSpec extends SimpleIOSuite:
  test("update sets and clears throne flag") {
    val service = new ThronePlacementServiceImpl[IO]
    val locations = ProvinceLocations.fromProvinceIdMap(
      Map(
        ProvinceId(1) -> ProvinceLocation(XCell(0), YCell(0)),
        ProvinceId(2) -> ProvinceLocation(XCell(1), YCell(0))
      )
    )
    val state = MapState.empty.copy(
      terrains = Vector(
        Terrain(ProvinceId(1), 0),
        Terrain(ProvinceId(2), TerrainFlag.Throne.mask)
      ),
      features = Vector(ProvinceFeature(ProvinceId(2), FeatureId(200))),
      provinceLocations = locations
    )
    val placements = Vector(ThronePlacement(ProvinceLocation(XCell(0), YCell(0)), ThroneLevel(1)))
    for
      res <- service.update(state, placements)
      mask1 = res.terrains.collectFirst { case Terrain(ProvinceId(1), m) => TerrainMask(m) }.get
      mask2 = res.terrains.collectFirst { case Terrain(ProvinceId(2), m) => TerrainMask(m) }.get
      throneIds = DomFeature.levelOneThrones.map(_.id.value).toSet
      featureIdOpt = res.features.collectFirst { case ProvinceFeature(ProvinceId(1), fid) => fid }
      feature2 = res.features.collectFirst { case ProvinceFeature(ProvinceId(2), fid) => fid }
    yield expect.all(
      mask1.hasFlag(TerrainFlag.Throne),
      !mask2.hasFlag(TerrainFlag.Throne),
      res.features.size == 2,
      featureIdOpt.exists(fid => throneIds.contains(fid.value)),
      feature2.contains(FeatureId(200))
    )
  }

  test("explicit feature id is applied") {
    val service = new ThronePlacementServiceImpl[IO]
    val locations = ProvinceLocations.fromProvinceIdMap(
      Map(
        ProvinceId(1) -> ProvinceLocation(XCell(0), YCell(0))
      )
    )
    val state = MapState.empty.copy(provinceLocations = locations)
    val placements = Vector(
      ThronePlacement(ProvinceLocation(XCell(0), YCell(0)), FeatureId(1358))
    )
    for
      res <- service.update(state, placements)
    yield expect(res.features == Vector(ProvinceFeature(ProvinceId(1), FeatureId(1358))))
  }
