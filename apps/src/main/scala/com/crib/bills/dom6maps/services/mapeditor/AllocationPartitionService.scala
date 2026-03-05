package com.crib.bills.dom6maps
package apps.services.mapeditor

import model.{Nation, ProvinceId}
import model.map.generation.{AllocationPartition, DistanceTiePolicy, RegionOwner}

trait AllocationPartitionService:
  def partition(
      adjacency: Vector[(ProvinceId, ProvinceId)],
      allProvinceIds: Vector[ProvinceId],
      startsByNation: Map[Nation, ProvinceId],
      tiePolicy: DistanceTiePolicy
  ): Either[Throwable, AllocationPartition]

class AllocationPartitionServiceImpl extends AllocationPartitionService:
  override def partition(
      adjacency: Vector[(ProvinceId, ProvinceId)],
      allProvinceIds: Vector[ProvinceId],
      startsByNation: Map[Nation, ProvinceId],
      tiePolicy: DistanceTiePolicy
  ): Either[Throwable, AllocationPartition] =
    val distinctStarts = startsByNation.values.toVector
    if distinctStarts.distinct.size != distinctStarts.size then
      Left(IllegalArgumentException("Allocation starts must be unique per layer"))
    else
      val provinceSet = allProvinceIds.toSet
      val unknownStart = startsByNation.find { case (_, provinceId) => !provinceSet.contains(provinceId) }
      unknownStart match
        case Some((nation, provinceId)) =>
          Left(IllegalArgumentException(s"Allocation start province ${provinceId.value} for nation ${nation.id} was not found in this layer"))
        case None =>
          val neighboursByProvince =
            adjacency.foldLeft(allProvinceIds.map(_ -> Vector.empty[ProvinceId]).toMap) { case (accumulator, (a, b)) =>
              val first = accumulator.getOrElse(a, Vector.empty) :+ b
              val second = accumulator.getOrElse(b, Vector.empty) :+ a
              accumulator.updated(a, first).updated(b, second)
            }

          val distanceByNation = startsByNation.map { case (nation, startProvince) =>
            nation -> bfsDistances(startProvince, neighboursByProvince)
          }

          val ownerByProvince = allProvinceIds.map { provinceId =>
            val distancesForProvince =
              distanceByNation.flatMap { case (nation, byProvince) =>
                byProvince.get(provinceId).map(distance => nation -> distance)
              }

            val owner =
              if distancesForProvince.isEmpty then RegionOwner.Neutral
              else
                val minimumDistance = distancesForProvince.values.min
                val nearestNations = distancesForProvince.collect { case (nation, distance) if distance == minimumDistance => nation }.toVector
                if nearestNations.size == 1 then RegionOwner.Player(nearestNations.head)
                else
                  tiePolicy match
                    case DistanceTiePolicy.NeutralTie => RegionOwner.Neutral

            provinceId -> owner
          }.toMap

          val distanceByProvinceByNation = allProvinceIds.map { provinceId =>
            val map =
              distanceByNation.flatMap { case (nation, byProvince) =>
                byProvince.get(provinceId).map(distance => nation -> distance)
              }
            provinceId -> map
          }.toMap

          Right(
            AllocationPartition(
              ownerByProvince = ownerByProvince,
              distanceByProvinceByNation = distanceByProvinceByNation
            )
          )

  private def bfsDistances(
      startProvince: ProvinceId,
      neighboursByProvince: Map[ProvinceId, Vector[ProvinceId]]
  ): Map[ProvinceId, Int] =
    val queue = scala.collection.mutable.Queue[(ProvinceId, Int)]((startProvince, 0))
    val visited = scala.collection.mutable.Map(startProvince -> 0)

    while queue.nonEmpty do
      val (provinceId, distance) = queue.dequeue()
      neighboursByProvince.getOrElse(provinceId, Vector.empty).foreach { neighbour =>
        if !visited.contains(neighbour) then
          visited.update(neighbour, distance + 1)
          queue.enqueue((neighbour, distance + 1))
      }

    visited.toMap
