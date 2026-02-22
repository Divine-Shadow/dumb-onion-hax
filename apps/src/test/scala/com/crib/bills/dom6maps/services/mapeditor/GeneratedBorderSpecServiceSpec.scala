package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import model.{BorderFlag, ProvinceId, TerrainFlag}
import model.map.generation.{BorderSpecGenerationPolicy, GeneratedGeometry}
import model.map.{Border, ProvinceLocation, Terrain, XCell, YCell}
import weaver.SimpleIOSuite

object GeneratedBorderSpecServiceSpec extends SimpleIOSuite:
  private val service = new GeneratedBorderSpecServiceImpl

  test("populateBorders deterministically enriches generated geometry with neighbourspec borders") {
    val geometry = GeneratedGeometry(
      provincePixelRuns = Vector.empty,
      adjacency = Vector(
        ProvinceId(1) -> ProvinceId(2),
        ProvinceId(1) -> ProvinceId(3),
        ProvinceId(2) -> ProvinceId(3),
        ProvinceId(3) -> ProvinceId(4)
      ),
      borders = Vector.empty,
      terrainByProvince = Vector(
        Terrain(ProvinceId(1), TerrainFlag.Plains.mask),
        Terrain(ProvinceId(2), TerrainFlag.Forest.mask),
        Terrain(ProvinceId(3), TerrainFlag.Highlands.mask),
        Terrain(ProvinceId(4), TerrainFlag.Plains.mask)
      ),
      provinceCentroids = Map(
        ProvinceId(1) -> ProvinceLocation(XCell(0), YCell(0)),
        ProvinceId(2) -> ProvinceLocation(XCell(1), YCell(0)),
        ProvinceId(3) -> ProvinceLocation(XCell(2), YCell(0)),
        ProvinceId(4) -> ProvinceLocation(XCell(3), YCell(0))
      )
    )

    val first = service.populateBorders(geometry, seed = 42L, borderSpecGenerationPolicy = BorderSpecGenerationPolicy.default)
    val second = service.populateBorders(geometry, seed = 42L, borderSpecGenerationPolicy = BorderSpecGenerationPolicy.default)

    IO(expect.all(
      first.borders.nonEmpty,
      first.borders == second.borders,
      first.borders.forall(border => geometry.adjacency.contains(border.a -> border.b) || geometry.adjacency.contains(border.b -> border.a))
    ))
  }

  test("populateBorders preserves pre-existing explicit border specs") {
    val geometry = GeneratedGeometry(
      provincePixelRuns = Vector.empty,
      adjacency = Vector(ProvinceId(1) -> ProvinceId(2)),
      borders = Vector(Border(ProvinceId(1), ProvinceId(2), BorderFlag.MountainPass)),
      terrainByProvince = Vector(
        Terrain(ProvinceId(1), TerrainFlag.Highlands.mask),
        Terrain(ProvinceId(2), TerrainFlag.Plains.mask)
      ),
      provinceCentroids = Map.empty
    )

    val updated = service.populateBorders(
      geometry,
      seed = 999L,
      borderSpecGenerationPolicy = BorderSpecGenerationPolicy.default
    )

    IO(expect(updated.borders.contains(Border(ProvinceId(1), ProvinceId(2), BorderFlag.MountainPass))))
  }
