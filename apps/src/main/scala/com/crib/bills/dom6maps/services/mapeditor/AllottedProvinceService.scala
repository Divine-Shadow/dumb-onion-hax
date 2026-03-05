package com.crib.bills.dom6maps
package apps.services.mapeditor

import model.{Nation, ProvinceId}
import model.map.generation.{AllocationPartition, CapRingSize, RegionOwner}

final case class PlayerAllottedProvinces(
    nation: Nation,
    capitalProvince: ProvinceId,
    capRingProvinceIds: Vector[ProvinceId],
    nonRingProvinceIds: Vector[ProvinceId]
)

trait AllottedProvinceService:
  def resolve(
      nation: Nation,
      startProvince: ProvinceId,
      capRingSize: CapRingSize,
      adjacency: Vector[(ProvinceId, ProvinceId)],
      partition: AllocationPartition
  ): Either[Throwable, PlayerAllottedProvinces]

class AllottedProvinceServiceImpl extends AllottedProvinceService:
  override def resolve(
      nation: Nation,
      startProvince: ProvinceId,
      capRingSize: CapRingSize,
      adjacency: Vector[(ProvinceId, ProvinceId)],
      partition: AllocationPartition
  ): Either[Throwable, PlayerAllottedProvinces] =
    val ownedProvinceIds = partition.ownerByProvince.collect {
      case (provinceId, RegionOwner.Player(ownerNation)) if ownerNation == nation => provinceId
    }.toSet

    if !ownedProvinceIds.contains(startProvince) then
      Left(IllegalArgumentException(s"Start province ${startProvince.value} for nation ${nation.id} is not owned by that nation after allocation"))
    else
      val neighboursByProvince =
        adjacency.foldLeft(Map.empty[ProvinceId, Vector[ProvinceId]]) { case (accumulator, (a, b)) =>
          val first = accumulator.getOrElse(a, Vector.empty) :+ b
          val second = accumulator.getOrElse(b, Vector.empty) :+ a
          accumulator.updated(a, first).updated(b, second)
        }

      val ringCandidates = bfsOwnedNeighbours(startProvince, ownedProvinceIds, neighboursByProvince)
      if ringCandidates.size < capRingSize.value then
        Left(
          IllegalArgumentException(
            s"Nation ${nation.id} requested cap ring size ${capRingSize.value}, but only ${ringCandidates.size} owned provinces are available in ring for start ${startProvince.value}"
          )
        )
      else
        val capRingProvinceIds = ringCandidates.take(capRingSize.value)
        val nonRingProvinceIds = ownedProvinceIds.toVector.sorted(using Ordering.by[ProvinceId, Int](_.value)).filterNot { provinceId =>
          provinceId == startProvince || capRingProvinceIds.contains(provinceId)
        }

        Right(
          PlayerAllottedProvinces(
            nation = nation,
            capitalProvince = startProvince,
            capRingProvinceIds = capRingProvinceIds,
            nonRingProvinceIds = nonRingProvinceIds
          )
        )

  private def bfsOwnedNeighbours(
      startProvince: ProvinceId,
      ownedProvinceIds: Set[ProvinceId],
      neighboursByProvince: Map[ProvinceId, Vector[ProvinceId]]
  ): Vector[ProvinceId] =
    val queue = scala.collection.mutable.Queue[(ProvinceId, Int)]((startProvince, 0))
    val visited = scala.collection.mutable.Set(startProvince)
    val candidates = scala.collection.mutable.ArrayBuffer.empty[(ProvinceId, Int)]

    while queue.nonEmpty do
      val (provinceId, distance) = queue.dequeue()
      neighboursByProvince.getOrElse(provinceId, Vector.empty).sorted(using Ordering.by[ProvinceId, Int](_.value)).foreach { neighbour =>
        if !visited.contains(neighbour) && ownedProvinceIds.contains(neighbour) then
          visited += neighbour
          queue.enqueue((neighbour, distance + 1))
          candidates += ((neighbour, distance + 1))
      }

    candidates.toVector.sortBy { case (provinceId, distance) => (distance, provinceId.value) }.map(_._1)
