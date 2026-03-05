package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import model.{Nation, ProvinceId}
import model.map.generation.{AllocationPartition, CapRingSize, RegionOwner}
import weaver.SimpleIOSuite

object AllottedProvinceServiceSpec extends SimpleIOSuite:
  test("resolves capital ring and non-ring provinces") {
    val service = new AllottedProvinceServiceImpl
    val adjacency = Vector(
      (ProvinceId(1), ProvinceId(2)),
      (ProvinceId(2), ProvinceId(3)),
      (ProvinceId(3), ProvinceId(4))
    )
    val partition =
      AllocationPartition(
        ownerByProvince = Map(
          ProvinceId(1) -> RegionOwner.Player(Nation.Pythium_Middle),
          ProvinceId(2) -> RegionOwner.Player(Nation.Pythium_Middle),
          ProvinceId(3) -> RegionOwner.Player(Nation.Pythium_Middle),
          ProvinceId(4) -> RegionOwner.Player(Nation.Pythium_Middle)
        ),
        distanceByProvinceByNation = Map.empty
      )

    val result = service.resolve(
      nation = Nation.Pythium_Middle,
      startProvince = ProvinceId(1),
      capRingSize = CapRingSize.unsafe(2),
      adjacency = adjacency,
      partition = partition
    )

    IO(
      expect(result.isRight) and
        expect(result.toOption.get.capitalProvince == ProvinceId(1)) and
        expect(result.toOption.get.capRingProvinceIds == Vector(ProvinceId(2), ProvinceId(3))) and
        expect(result.toOption.get.nonRingProvinceIds == Vector(ProvinceId(4)))
    )
  }

  test("fails when cap ring request exceeds owned neighbours") {
    val service = new AllottedProvinceServiceImpl
    val adjacency = Vector((ProvinceId(1), ProvinceId(2)))
    val partition =
      AllocationPartition(
        ownerByProvince = Map(
          ProvinceId(1) -> RegionOwner.Player(Nation.Pythium_Middle),
          ProvinceId(2) -> RegionOwner.Player(Nation.Pythium_Middle)
        ),
        distanceByProvinceByNation = Map.empty
      )

    val result = service.resolve(
      nation = Nation.Pythium_Middle,
      startProvince = ProvinceId(1),
      capRingSize = CapRingSize.unsafe(3),
      adjacency = adjacency,
      partition = partition
    )

    IO(expect(result.isLeft))
  }
