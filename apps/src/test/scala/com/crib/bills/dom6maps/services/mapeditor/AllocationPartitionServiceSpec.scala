package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import model.{Nation, ProvinceId}
import model.map.generation.{DistanceTiePolicy, RegionOwner}
import weaver.SimpleIOSuite

object AllocationPartitionServiceSpec extends SimpleIOSuite:
  test("assigns tie provinces to neutral") {
    val service = new AllocationPartitionServiceImpl
    val adjacency = Vector(
      (ProvinceId(1), ProvinceId(2)),
      (ProvinceId(2), ProvinceId(3))
    )
    val provinces = Vector(ProvinceId(1), ProvinceId(2), ProvinceId(3))
    val starts = Map(
      Nation.Pythium_Middle -> ProvinceId(1),
      Nation.Sceleria_Middle -> ProvinceId(3)
    )

    val result = service.partition(adjacency, provinces, starts, DistanceTiePolicy.NeutralTie)

    IO(
      expect(result.isRight) and
        expect(result.toOption.get.ownerByProvince(ProvinceId(1)) == RegionOwner.Player(Nation.Pythium_Middle)) and
        expect(result.toOption.get.ownerByProvince(ProvinceId(3)) == RegionOwner.Player(Nation.Sceleria_Middle)) and
        expect(result.toOption.get.ownerByProvince(ProvinceId(2)) == RegionOwner.Neutral)
    )
  }

  test("rejects duplicate start provinces") {
    val service = new AllocationPartitionServiceImpl
    val adjacency = Vector((ProvinceId(1), ProvinceId(2)))
    val provinces = Vector(ProvinceId(1), ProvinceId(2))
    val starts = Map(
      Nation.Pythium_Middle -> ProvinceId(1),
      Nation.Sceleria_Middle -> ProvinceId(1)
    )

    IO(expect(service.partition(adjacency, provinces, starts, DistanceTiePolicy.NeutralTie).isLeft))
  }
