package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.syntax.all.*
import cats.effect.Async
import model.{ProvinceId, TerrainFlag}
import model.map.{Border, Pb, ProvinceLocation, Terrain, WrapState, XCell, YCell}
import model.map.generation.{GeneratedGeometry, GeometryGenerationInput, TerrainDistributionPolicy}
import model.map.image.{ProvinceAnchorLocator, ProvincePixelRasterizer}

import scala.util.Random
import scala.collection.mutable

trait MapGeometryGenerator[Sequencer[_]]:
  def generate[ErrorChannel[_]](
      input: GeometryGenerationInput
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[GeneratedGeometry]]

class GridNoiseMapGeometryGeneratorImpl[Sequencer[_]: Async] extends MapGeometryGenerator[Sequencer]:
  private val sequencer = summon[Async[Sequencer]]

  private final case class ProvinceSeed(
      provinceId: ProvinceId,
      xPixel: Double,
      yPixel: Double
  )

  override def generate[ErrorChannel[_]](
      input: GeometryGenerationInput
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[GeneratedGeometry]] =
    sequencer.delay(runGeneration(input)).map {
      case Left(error) => errorChannel.raiseError[GeneratedGeometry](error)
      case Right(value) => errorChannel.pure(value)
    }

  private def runGeneration(input: GeometryGenerationInput): Either[Throwable, GeneratedGeometry] =
    if input.provinceCount <= 0 then
      Left(IllegalArgumentException("provinceCount must be positive"))
    else if input.seaRatio < 0.0 || input.seaRatio > 1.0 then
      Left(IllegalArgumentException("seaRatio must be in range [0.0, 1.0]"))
    else
      val widthPixels = input.mapDimensions.width.value * 256
      val heightPixels = input.mapDimensions.height.value * 160
      val random = Random(input.seed)
      // Generate province ownership in unwrapped raster space to avoid seam-split provinces
      // on wrapped maps. Dominions center placement behaves poorly when a province is split
      // into disjoint components across the wrap seam.
      val ownershipWrapState = WrapState.NoWrap
      val provinceSeeds = generateSeeds(
        widthPixels,
        heightPixels,
        input.mapDimensions.height.value,
        input.provinceCount,
        input.gridJitter,
        random
      )
      val provinceIdentifierByPixel = rasterizeOwnership(
        widthPixels,
        heightPixels,
        provinceSeeds,
        ownershipWrapState,
        input.seed,
        input.noiseScale
      )
      val smoothedProvinceIdentifierByPixel = smoothOwnership(
        widthPixels,
        heightPixels,
        provinceIdentifierByPixel,
        provinceSeeds,
        ownershipWrapState
      )
      val remap = remapProvinceIdentifiersByFirstPixelScanOrder(
        widthPixels,
        heightPixels,
        smoothedProvinceIdentifierByPixel,
        input.provinceCount
      )
      val adjacency = deriveAdjacency(widthPixels, heightPixels, remap.provinceIdentifierByPixel, input.wrapState)
      val terrainByProvince = assignTerrainMasks(input, provinceSeeds, remap.oldIdentifierToNewIdentifier)
      val centroids = deriveCentroids(
        widthPixels,
        heightPixels,
        input.provinceCount,
        remap.provinceIdentifierByPixel,
        provinceSeeds,
        remap.oldIdentifierToNewIdentifier
      )
      val provinceRuns = buildProvinceRuns(widthPixels, heightPixels, remap.provinceIdentifierByPixel)
      val ownership = ProvincePixelRasterizer.ProvincePixelOwnership(
        widthPixels = widthPixels,
        heightPixels = heightPixels,
        provinceIdentifierByPixel = remap.provinceIdentifierByPixel
      )
      val anchorPixelByProvince = ProvinceAnchorLocator
        .locateAnchorPixelByProvince(ownership)
        .map { case (provinceIdentifier, pixelIndex) =>
          val xPixel = pixelIndex % widthPixels
          val yPixelTopOrigin = pixelIndex / widthPixels
          val yPixelBottomOrigin = (heightPixels - 1) - yPixelTopOrigin
          ProvinceId(provinceIdentifier) -> (xPixel, yPixelBottomOrigin)
        }
      Right(
        GeneratedGeometry(
          provincePixelRuns = prependAnchorPixels(provinceRuns, anchorPixelByProvince),
          adjacency = adjacency,
          borders = Vector.empty[Border],
          terrainByProvince = terrainByProvince,
          provinceCentroids = centroids
        )
      )

  private final case class IdentifierRemap(
      provinceIdentifierByPixel: Array[Int],
      oldIdentifierToNewIdentifier: Map[Int, Int]
  )

  private def remapProvinceIdentifiersByFirstPixelScanOrder(
      widthPixels: Int,
      heightPixels: Int,
      provinceIdentifierByPixel: Array[Int],
      provinceCount: Int
  ): IdentifierRemap =
    val ownership = ProvincePixelRasterizer.ProvincePixelOwnership(
      widthPixels = widthPixels,
      heightPixels = heightPixels,
      provinceIdentifierByPixel = provinceIdentifierByPixel
    )
    val anchorPixelByIdentifier = ProvinceAnchorLocator.locateAnchorPixelByProvince(ownership)

    val allIdentifiers = (1 to provinceCount).toVector
    val sortedIdentifiers =
      allIdentifiers.sortBy { identifier =>
        anchorPixelByIdentifier
          .get(identifier)
          .map(bottomLeftScanPixelIndex(widthPixels, heightPixels, _))
          .getOrElse(Int.MaxValue)
      }
    val oldToNew = sortedIdentifiers.zipWithIndex.map { case (oldIdentifier, index) =>
      oldIdentifier -> (index + 1)
    }.toMap

    val remappedOwnership = Array.ofDim[Int](provinceIdentifierByPixel.length)
    var ownershipIndex = 0
    while ownershipIndex < provinceIdentifierByPixel.length do
      val oldIdentifier = provinceIdentifierByPixel(ownershipIndex)
      remappedOwnership(ownershipIndex) = oldToNew.getOrElse(oldIdentifier, oldIdentifier)
      ownershipIndex += 1

    IdentifierRemap(remappedOwnership, oldToNew)

  private def bottomLeftScanPixelIndex(
      widthPixels: Int,
      heightPixels: Int,
      rowMajorPixelIndex: Int
  ): Int =
    val xPixel = rowMajorPixelIndex % widthPixels
    val yPixelTopOrigin = rowMajorPixelIndex / widthPixels
    val yPixelBottomOrigin = (heightPixels - 1) - yPixelTopOrigin
    yPixelBottomOrigin * widthPixels + xPixel

  private def generateSeeds(
      widthPixels: Int,
      heightPixels: Int,
      mapHeightInCells: Int,
      provinceCount: Int,
      gridJitter: Double,
      random: Random
  ): Vector[ProvinceSeed] =
    val jitterFactor = math.max(0.0, math.min(1.0, gridJitter))
    val aspectRatio = widthPixels.toDouble / heightPixels.toDouble
    val computedRowCount = math.max(1, math.ceil(math.sqrt(provinceCount.toDouble / aspectRatio)).toInt)
    val minimumRowCount = math.max(1, math.min(mapHeightInCells, provinceCount))
    val rowCount = math.max(minimumRowCount, computedRowCount)
    val rowHeight = heightPixels.toDouble / rowCount.toDouble
    val baseSeedsPerRow = provinceCount / rowCount
    val extraSeedRows = provinceCount % rowCount

    val result = Vector.newBuilder[ProvinceSeed]
    var provinceIdentifier = 1
    var rowIndex = 0
    while rowIndex < rowCount do
      val seedsInRow = baseSeedsPerRow + (if rowIndex < extraSeedRows then 1 else 0)
      if seedsInRow > 0 then
        val rowCellWidth = widthPixels.toDouble / seedsInRow.toDouble
        val centerY = (rowIndex + 0.5) * rowHeight
        val rowOffsetX = (random.nextDouble() - 0.5) * rowCellWidth * 0.8
        var columnIndex = 0
        while columnIndex < seedsInRow do
          val centerX = (columnIndex + 0.5) * rowCellWidth + rowOffsetX
          val jitterX = (random.nextDouble() - 0.5) * rowCellWidth * jitterFactor
          val jitterY = (random.nextDouble() - 0.5) * rowHeight * jitterFactor
          val boundedX = math.max(0.0, math.min(widthPixels - 1.0, centerX + jitterX))
          val boundedY = math.max(0.0, math.min(heightPixels - 1.0, centerY + jitterY))
          result += ProvinceSeed(ProvinceId(provinceIdentifier), boundedX, boundedY)
          provinceIdentifier += 1
          columnIndex += 1
      rowIndex += 1

    result.result()
  
  private def rasterizeOwnership(
      widthPixels: Int,
      heightPixels: Int,
      provinceSeeds: Vector[ProvinceSeed],
      wrapState: WrapState,
      seed: Long,
      noiseScale: Double
  ): Array[Int] =
    val clampedNoiseScale = math.max(0.25, math.min(4.0, noiseScale))
    val boundaryNoiseAmplitude = 70.0 * clampedNoiseScale
    val boundaryNoiseFrequency = 0.14 / clampedNoiseScale

    val provinceIdentifierByPixel = Array.ofDim[Int](widthPixels * heightPixels)
    var yPixel = 0
    while yPixel < heightPixels do
      var xPixel = 0
      while xPixel < widthPixels do
        var bestDistance = Double.MaxValue
        var bestProvinceIdentifier = 0
        var seedIndex = 0
        while seedIndex < provinceSeeds.length do
          val provinceSeed = provinceSeeds(seedIndex)
          val baseDistance = wrappedSquaredDistance(
            xPixel.toDouble,
            yPixel.toDouble,
            provinceSeed.xPixel,
            provinceSeed.yPixel,
            widthPixels,
            heightPixels,
            wrapState
          )
          val relativeDeltaX = wrappedDeltaSigned(
            xPixel.toDouble,
            provinceSeed.xPixel,
            widthPixels,
            wrapState == WrapState.HorizontalWrap || wrapState == WrapState.FullWrap
          )
          val relativeDeltaY = wrappedDeltaSigned(
            yPixel.toDouble,
            provinceSeed.yPixel,
            heightPixels,
            wrapState == WrapState.VerticalWrap || wrapState == WrapState.FullWrap
          )
          val relativeRadius = math.sqrt(relativeDeltaX * relativeDeltaX + relativeDeltaY * relativeDeltaY)
          val relativeAngle = math.atan2(relativeDeltaY, relativeDeltaX)
          val provincePhase = (splitMix64(seed + provinceSeed.provinceId.value.toLong * 0x9e3779b97f4a7c15L) & 0xffff).toDouble / 4096.0
          val radiusFactor = math.min(1.0, relativeRadius / 72.0)
          val boundaryNoise =
            (math.sin(relativeAngle * 7.0 + relativeRadius * boundaryNoiseFrequency + provincePhase) * 0.60 +
              math.sin(relativeAngle * 11.0 - relativeRadius * boundaryNoiseFrequency * 0.7 + provincePhase * 1.7) * 0.40) *
              boundaryNoiseAmplitude *
              radiusFactor
          val distance = baseDistance + boundaryNoise
          if distance < bestDistance then
            bestDistance = distance
            bestProvinceIdentifier = provinceSeed.provinceId.value
          seedIndex += 1
        provinceIdentifierByPixel(yPixel * widthPixels + xPixel) = bestProvinceIdentifier
        xPixel += 1
      yPixel += 1
    provinceIdentifierByPixel

  private def wrappedSquaredDistance(
      sourceX: Double,
      sourceY: Double,
      targetX: Double,
      targetY: Double,
      widthPixels: Int,
      heightPixels: Int,
      wrapState: WrapState
  ): Double =
    val deltaX = math.abs(sourceX - targetX)
    val deltaY = math.abs(sourceY - targetY)
    val wrappedDeltaX =
      wrapState match
        case WrapState.HorizontalWrap | WrapState.FullWrap => math.min(deltaX, widthPixels.toDouble - deltaX)
        case _ => deltaX
    val wrappedDeltaY =
      wrapState match
        case WrapState.VerticalWrap | WrapState.FullWrap => math.min(deltaY, heightPixels.toDouble - deltaY)
        case _ => deltaY
    wrappedDeltaX * wrappedDeltaX + wrappedDeltaY * wrappedDeltaY

  private def wrappedDeltaSigned(
      source: Double,
      target: Double,
      sizePixels: Int,
      wrapEnabled: Boolean
  ): Double =
    val direct = source - target
    if !wrapEnabled then direct
    else
      val minusWrap = direct - sizePixels.toDouble
      val plusWrap = direct + sizePixels.toDouble
      Vector(direct, minusWrap, plusWrap).minBy(delta => math.abs(delta))

  private def buildProvinceRuns(
      widthPixels: Int,
      heightPixels: Int,
      provinceIdentifierByPixel: Array[Int]
  ): Vector[Pb] =
    val maxRunLength = 255
    val result = Vector.newBuilder[Pb]
    var yPixel = 0
    while yPixel < heightPixels do
      var runStartX = 0
      var currentProvinceIdentifier = provinceIdentifierByPixel(yPixel * widthPixels)
      var xPixel = 1
      while xPixel <= widthPixels do
        val provinceIdentifier =
          if xPixel < widthPixels then provinceIdentifierByPixel(yPixel * widthPixels + xPixel)
          else Int.MinValue
        if provinceIdentifier != currentProvinceIdentifier then
          val runLength = xPixel - runStartX
          var emittedLength = 0
          while emittedLength < runLength do
            val chunkLength = math.min(maxRunLength, runLength - emittedLength)
            val yPixelBottomOrigin = (heightPixels - 1) - yPixel
            result += Pb(runStartX + emittedLength, yPixelBottomOrigin, chunkLength, ProvinceId(currentProvinceIdentifier))
            emittedLength += chunkLength
          runStartX = xPixel
          currentProvinceIdentifier = provinceIdentifier
        xPixel += 1
      yPixel += 1
    result.result()

  private def buildAnchorPreludeRuns(
      provinceRuns: Vector[Pb]
  ): Vector[Pb] =
    provinceRuns
      .groupBy(_.province)
      .toVector
      .sortBy(_._1.value)
      .flatMap { case (provinceId, runsForProvince) =>
        runsForProvince
          .sortBy(run => (-run.length, run.y, run.x))
          .headOption
          .map { run =>
            val anchorX = run.x + (run.length / 2)
            Pb(anchorX, run.y, 1, provinceId)
          }
      }

  private def prependAnchorPixels(
      provinceRuns: Vector[Pb],
      anchorPixelByProvince: Map[ProvinceId, (Int, Int)]
  ): Vector[Pb] =
    val anchorRuns =
      anchorPixelByProvince.toVector
        .sortBy(_._1.value)
        .map { case (provinceId, (xPixel, yPixelBottomOrigin)) =>
          Pb(xPixel, yPixelBottomOrigin, 1, provinceId)
        }
    anchorRuns ++ provinceRuns

  private def selectAnchorPixelByProvince(
      widthPixels: Int,
      heightPixels: Int,
      provinceIdentifierByPixel: Array[Int],
      centroids: Map[ProvinceId, ProvinceLocation],
      wrapState: WrapState
  ): Map[ProvinceId, (Int, Int)] =
    final case class Candidate(xPixel: Int, yPixelBottomOrigin: Int, distanceScore: Long)

    def centroidPixelFor(provinceId: ProvinceId): (Int, Int) =
      centroids.get(provinceId) match
        case Some(location) =>
          (location.x.value * 256 + 128, location.y.value * 160 + 80)
        case None =>
          (widthPixels / 2, heightPixels / 2)

    def wrapX(xPixel: Int): Option[Int] =
      if xPixel >= 0 && xPixel < widthPixels then Some(xPixel)
      else if wrapState == WrapState.HorizontalWrap || wrapState == WrapState.FullWrap then
        Some((xPixel % widthPixels + widthPixels) % widthPixels)
      else None

    def wrapY(yPixel: Int): Option[Int] =
      if yPixel >= 0 && yPixel < heightPixels then Some(yPixel)
      else if wrapState == WrapState.VerticalWrap || wrapState == WrapState.FullWrap then
        Some((yPixel % heightPixels + heightPixels) % heightPixels)
      else None

    def ownerAt(xPixel: Int, yPixel: Int): Option[Int] =
      for
        resolvedX <- wrapX(xPixel)
        resolvedY <- wrapY(yPixel)
      yield provinceIdentifierByPixel(resolvedY * widthPixels + resolvedX)

    def isInteriorPixel(xPixel: Int, yPixel: Int, provinceIdentifier: Int): Boolean =
      val neighborOffsets = Vector((0, -1), (0, 1), (-1, 0), (1, 0))
      neighborOffsets.forall { case (xOffset, yOffset) =>
        ownerAt(xPixel + xOffset, yPixel + yOffset).contains(provinceIdentifier)
      }

    val bestInterior = mutable.HashMap.empty[Int, Candidate]
    val bestAny = mutable.HashMap.empty[Int, Candidate]

    var yPixel = 0
    while yPixel < heightPixels do
      var xPixel = 0
      while xPixel < widthPixels do
        val provinceIdentifier = provinceIdentifierByPixel(yPixel * widthPixels + xPixel)
        val provinceId = ProvinceId(provinceIdentifier)
        val (centroidXPixel, centroidYPixel) = centroidPixelFor(provinceId)
        val deltaX = xPixel - centroidXPixel
        val deltaY = yPixel - centroidYPixel
        val distanceScore = deltaX.toLong * deltaX.toLong + deltaY.toLong * deltaY.toLong
        val yBottom = (heightPixels - 1) - yPixel
        val candidate = Candidate(xPixel, yBottom, distanceScore)

        val existingAny = bestAny.get(provinceIdentifier)
        if existingAny.forall(_.distanceScore > distanceScore) then
          bestAny.update(provinceIdentifier, candidate)

        if isInteriorPixel(xPixel, yPixel, provinceIdentifier) then
          val existingInterior = bestInterior.get(provinceIdentifier)
          if existingInterior.forall(_.distanceScore > distanceScore) then
            bestInterior.update(provinceIdentifier, candidate)
        xPixel += 1
      yPixel += 1

    centroids.keys.toVector.flatMap { provinceId =>
      val identifier = provinceId.value
      bestInterior
        .get(identifier)
        .orElse(bestAny.get(identifier))
        .map(candidate => provinceId -> (candidate.xPixel, candidate.yPixelBottomOrigin))
    }.toMap

  private def enforceSeedCoreOwnership(
      widthPixels: Int,
      heightPixels: Int,
      ownershipByPixel: Array[Int],
      provinceSeeds: Vector[ProvinceSeed],
      wrapState: WrapState
  ): Array[Int] =
    val result = ownershipByPixel.clone()
    val coreRadiusPixels = 20
    val coreRadiusSquared = coreRadiusPixels * coreRadiusPixels

    def wrapCoordinate(value: Int, size: Int): Int =
      ((value % size) + size) % size

    def resolveX(xPixel: Int): Option[Int] =
      if xPixel >= 0 && xPixel < widthPixels then Some(xPixel)
      else if wrapState == WrapState.HorizontalWrap || wrapState == WrapState.FullWrap then
        Some(wrapCoordinate(xPixel, widthPixels))
      else None

    def resolveY(yPixel: Int): Option[Int] =
      if yPixel >= 0 && yPixel < heightPixels then Some(yPixel)
      else if wrapState == WrapState.VerticalWrap || wrapState == WrapState.FullWrap then
        Some(wrapCoordinate(yPixel, heightPixels))
      else None

    def ownerAt(xPixel: Int, yPixel: Int): Option[Int] =
      for
        resolvedX <- resolveX(xPixel)
        resolvedY <- resolveY(yPixel)
      yield result(resolvedY * widthPixels + resolvedX)

    def localInteriorDepth(xPixel: Int, yPixel: Int, provinceIdentifier: Int): Int =
      val maxRadius = 12
      var radius = 1
      var accepted = 0
      while radius <= maxRadius do
        val checks = Vector(
          (xPixel - radius, yPixel),
          (xPixel + radius, yPixel),
          (xPixel, yPixel - radius),
          (xPixel, yPixel + radius),
          (xPixel - radius, yPixel - radius),
          (xPixel + radius, yPixel - radius),
          (xPixel - radius, yPixel + radius),
          (xPixel + radius, yPixel + radius)
        )
        val allSameProvince = checks.forall { case (candidateX, candidateY) =>
          ownerAt(candidateX, candidateY).contains(provinceIdentifier)
        }
        if allSameProvince then
          accepted = radius
          radius += 1
        else
          radius = maxRadius + 1
      accepted

    def climbToInteriorAnchor(
        seedX: Int,
        seedY: Int,
        provinceIdentifier: Int
    ): (Int, Int) =
      val candidateOffsets = Vector(
        (-1, -1), (0, -1), (1, -1),
        (-1, 0),           (1, 0),
        (-1, 1),  (0, 1),  (1, 1)
      )
      var currentX = seedX
      var currentY = seedY
      var steps = 0
      while steps < 64 do
        val currentDepth = localInteriorDepth(currentX, currentY, provinceIdentifier)
        var bestX = currentX
        var bestY = currentY
        var bestDepth = currentDepth
        candidateOffsets.foreach { case (xOffset, yOffset) =>
          val neighborX = currentX + xOffset
          val neighborY = currentY + yOffset
          val resolvedNeighbor =
            for
              resolvedX <- resolveX(neighborX)
              resolvedY <- resolveY(neighborY)
            yield (resolvedX, resolvedY)
          resolvedNeighbor match
            case Some((resolvedX, resolvedY)) if ownerAt(resolvedX, resolvedY).contains(provinceIdentifier) =>
              val neighborDepth = localInteriorDepth(resolvedX, resolvedY, provinceIdentifier)
              if neighborDepth > bestDepth then
                bestX = resolvedX
                bestY = resolvedY
                bestDepth = neighborDepth
            case _ => ()
        }
        if bestX == currentX && bestY == currentY then
          steps = 64
        else
          currentX = bestX
          currentY = bestY
          steps += 1
      (currentX, currentY)

    provinceSeeds.foreach { provinceSeed =>
      val centerX = math.max(0, math.min(widthPixels - 1, math.round(provinceSeed.xPixel).toInt))
      val centerY = math.max(0, math.min(heightPixels - 1, math.round(provinceSeed.yPixel).toInt))
      val (anchorX, anchorY) = climbToInteriorAnchor(centerX, centerY, provinceSeed.provinceId.value)
      var yOffset = -coreRadiusPixels
      while yOffset <= coreRadiusPixels do
        var xOffset = -coreRadiusPixels
        while xOffset <= coreRadiusPixels do
          if xOffset * xOffset + yOffset * yOffset <= coreRadiusSquared then
            val candidateX = anchorX + xOffset
            val candidateY = anchorY + yOffset
            val resolvedX = resolveX(candidateX)
            val resolvedY = resolveY(candidateY)
            (resolvedX, resolvedY) match
              case (Some(xPixel), Some(yPixel)) =>
                result(yPixel * widthPixels + xPixel) = provinceSeed.provinceId.value
              case _ => ()
          xOffset += 1
        yOffset += 1
    }
    result

  private def enforceCentroidCoreOwnership(
      widthPixels: Int,
      heightPixels: Int,
      ownershipByPixel: Array[Int],
      wrapState: WrapState
  ): Array[Int] =
    val result = ownershipByPixel.clone()
    val maxProvinceIdentifier = result.foldLeft(0)(math.max)
    if maxProvinceIdentifier <= 0 then result
    else
      val counts = Array.fill[Long](maxProvinceIdentifier + 1)(0L)
      val sumX = Array.fill[Long](maxProvinceIdentifier + 1)(0L)
      val sumY = Array.fill[Long](maxProvinceIdentifier + 1)(0L)

      var yPixel = 0
      while yPixel < heightPixels do
        var xPixel = 0
        while xPixel < widthPixels do
          val provinceIdentifier = result(yPixel * widthPixels + xPixel)
          if provinceIdentifier > 0 then
            counts(provinceIdentifier) += 1L
            sumX(provinceIdentifier) += xPixel.toLong
            sumY(provinceIdentifier) += yPixel.toLong
          xPixel += 1
        yPixel += 1

      val coreRadiusPixels = 12
      val coreRadiusSquared = coreRadiusPixels * coreRadiusPixels

      def wrapCoordinate(value: Int, size: Int): Int =
        ((value % size) + size) % size

      def resolveX(xPixel: Int): Option[Int] =
        if xPixel >= 0 && xPixel < widthPixels then Some(xPixel)
        else if wrapState == WrapState.HorizontalWrap || wrapState == WrapState.FullWrap then
          Some(wrapCoordinate(xPixel, widthPixels))
        else None

      def resolveY(yPixel: Int): Option[Int] =
        if yPixel >= 0 && yPixel < heightPixels then Some(yPixel)
        else if wrapState == WrapState.VerticalWrap || wrapState == WrapState.FullWrap then
          Some(wrapCoordinate(yPixel, heightPixels))
        else None

      var provinceIdentifier = 1
      while provinceIdentifier <= maxProvinceIdentifier do
        val count = counts(provinceIdentifier)
        if count > 0 then
          val centerX = math.round(sumX(provinceIdentifier).toDouble / count.toDouble).toInt
          val centerY = math.round(sumY(provinceIdentifier).toDouble / count.toDouble).toInt
          var yOffset = -coreRadiusPixels
          while yOffset <= coreRadiusPixels do
            var xOffset = -coreRadiusPixels
            while xOffset <= coreRadiusPixels do
              if xOffset * xOffset + yOffset * yOffset <= coreRadiusSquared then
                val candidateX = centerX + xOffset
                val candidateY = centerY + yOffset
                val resolvedX = resolveX(candidateX)
                val resolvedY = resolveY(candidateY)
                (resolvedX, resolvedY) match
                  case (Some(xResolved), Some(yResolved)) =>
                    result(yResolved * widthPixels + xResolved) = provinceIdentifier
                  case _ => ()
              xOffset += 1
            yOffset += 1
        provinceIdentifier += 1

      result

  private def smoothOwnership(
      widthPixels: Int,
      heightPixels: Int,
      ownershipByPixel: Array[Int],
      provinceSeeds: Vector[ProvinceSeed],
      wrapState: WrapState
  ): Array[Int] =
    val seedCoordinates = provinceSeeds.map { provinceSeed =>
      val xPixel = math.max(0, math.min(widthPixels - 1, math.round(provinceSeed.xPixel).toInt))
      val yPixel = math.max(0, math.min(heightPixels - 1, math.round(provinceSeed.yPixel).toInt))
      (xPixel, yPixel)
    }.toSet

    def wrapX(xPixel: Int): Option[Int] =
      if xPixel >= 0 && xPixel < widthPixels then Some(xPixel)
      else if wrapState == WrapState.HorizontalWrap || wrapState == WrapState.FullWrap then
        Some((xPixel % widthPixels + widthPixels) % widthPixels)
      else None

    def wrapY(yPixel: Int): Option[Int] =
      if yPixel >= 0 && yPixel < heightPixels then Some(yPixel)
      else if wrapState == WrapState.VerticalWrap || wrapState == WrapState.FullWrap then
        Some((yPixel % heightPixels + heightPixels) % heightPixels)
      else None

    var current = ownershipByPixel.clone()
    val neighborOffsets = Vector(
      (-1, -1), (0, -1), (1, -1),
      (-1, 0),           (1, 0),
      (-1, 1),  (0, 1),  (1, 1)
    )
    var pass = 0
    while pass < 1 do
      val next = current.clone()
      var yPixel = 0
      while yPixel < heightPixels do
        var xPixel = 0
        while xPixel < widthPixels do
          if !seedCoordinates.contains((xPixel, yPixel)) then
            val centerOwner = current(yPixel * widthPixels + xPixel)
            val neighborCounts = scala.collection.mutable.HashMap.empty[Int, Int]
            neighborOffsets.foreach { case (xOffset, yOffset) =>
              val wrappedX = wrapX(xPixel + xOffset)
              val wrappedY = wrapY(yPixel + yOffset)
              (wrappedX, wrappedY) match
                case (Some(resolvedX), Some(resolvedY)) =>
                  val owner = current(resolvedY * widthPixels + resolvedX)
                  neighborCounts.update(owner, neighborCounts.getOrElse(owner, 0) + 1)
                case _ => ()
            }
            val bestNeighborOwner = neighborCounts.toVector.sortBy { case (owner, count) => (-count, owner) }.headOption
            bestNeighborOwner match
              case Some((owner, count)) if owner != centerOwner && count >= 5 =>
                next(yPixel * widthPixels + xPixel) = owner
              case _ => ()
          xPixel += 1
        yPixel += 1
      current = next
      pass += 1
    current

  private def deriveAdjacency(
      widthPixels: Int,
      heightPixels: Int,
      provinceIdentifierByPixel: Array[Int],
      wrapState: WrapState
  ): Vector[(ProvinceId, ProvinceId)] =
    val adjacencyPairs = mutable.HashSet.empty[(Int, Int)]

    def addPair(firstIdentifier: Int, secondIdentifier: Int): Unit =
      if firstIdentifier != secondIdentifier then
        val ordered =
          if firstIdentifier < secondIdentifier then (firstIdentifier, secondIdentifier)
          else (secondIdentifier, firstIdentifier)
        adjacencyPairs += ordered

    var yPixel = 0
    while yPixel < heightPixels do
      var xPixel = 0
      while xPixel < widthPixels do
        val currentIdentifier = provinceIdentifierByPixel(yPixel * widthPixels + xPixel)
        if xPixel + 1 < widthPixels then
          val rightIdentifier = provinceIdentifierByPixel(yPixel * widthPixels + xPixel + 1)
          addPair(currentIdentifier, rightIdentifier)
        if yPixel + 1 < heightPixels then
          val downIdentifier = provinceIdentifierByPixel((yPixel + 1) * widthPixels + xPixel)
          addPair(currentIdentifier, downIdentifier)
        xPixel += 1
      yPixel += 1

    if wrapState == WrapState.HorizontalWrap || wrapState == WrapState.FullWrap then
      var yPixelWrap = 0
      while yPixelWrap < heightPixels do
        val leftIdentifier = provinceIdentifierByPixel(yPixelWrap * widthPixels)
        val rightIdentifier = provinceIdentifierByPixel(yPixelWrap * widthPixels + (widthPixels - 1))
        addPair(leftIdentifier, rightIdentifier)
        yPixelWrap += 1

    if wrapState == WrapState.VerticalWrap || wrapState == WrapState.FullWrap then
      var xPixelWrap = 0
      while xPixelWrap < widthPixels do
        val topIdentifier = provinceIdentifierByPixel(xPixelWrap)
        val bottomIdentifier = provinceIdentifierByPixel((heightPixels - 1) * widthPixels + xPixelWrap)
        addPair(topIdentifier, bottomIdentifier)
        xPixelWrap += 1

    adjacencyPairs
      .toVector
      .sortBy { case (firstIdentifier, secondIdentifier) => (firstIdentifier, secondIdentifier) }
      .map { case (firstIdentifier, secondIdentifier) => (ProvinceId(firstIdentifier), ProvinceId(secondIdentifier)) }

  private def assignTerrainMasks(
      input: GeometryGenerationInput,
      provinceSeeds: Vector[ProvinceSeed],
      oldIdentifierToNewIdentifier: Map[Int, Int]
  ): Vector[Terrain] =
    provinceSeeds.map { provinceSeed =>
      val randomDraw = sampledProvinceTerrainDraw(provinceSeed.provinceId, input.seed)
      val terrainMask = buildTerrainMask(randomDraw, input.seaRatio, input.terrainDistributionPolicy)
      val remappedIdentifier = oldIdentifierToNewIdentifier.getOrElse(provinceSeed.provinceId.value, provinceSeed.provinceId.value)
      Terrain(ProvinceId(remappedIdentifier), terrainMask)
    }

  private def sampledProvinceTerrainDraw(
      provinceId: ProvinceId,
      seed: Long
  ): Double =
    val mixed = splitMix64(seed + provinceId.value.toLong * 0x9e3779b97f4a7c15L)
    val positiveHash = mixed & 0x7fffffffffffffffL
    positiveHash.toDouble / Long.MaxValue.toDouble

  private def splitMix64(value: Long): Long =
    val step1 = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L
    val step2 = (step1 ^ (step1 >>> 27)) * 0x94d049bb133111ebL
    step2 ^ (step2 >>> 31)

  private def buildTerrainMask(
      noiseValue: Double,
      seaRatio: Double,
      terrainDistributionPolicy: TerrainDistributionPolicy
  ): Long =
    if noiseValue < seaRatio then
      TerrainFlag.Sea.mask
    else
      val normalizedLandValue =
        if seaRatio >= 1.0 then 1.0
        else (noiseValue - seaRatio) / (1.0 - seaRatio)
      val swampThreshold = terrainDistributionPolicy.swampPercent.value.value
      val wasteThreshold = swampThreshold + terrainDistributionPolicy.wastePercent.value.value
      val highlandThreshold = wasteThreshold + terrainDistributionPolicy.highlandPercent.value.value
      val forestThreshold = highlandThreshold + terrainDistributionPolicy.forestPercent.value.value
      val farmThreshold = forestThreshold + terrainDistributionPolicy.farmPercent.value.value
      val extraLakeThreshold = farmThreshold + terrainDistributionPolicy.extraLakePercent.value.value

      if normalizedLandValue < swampThreshold then TerrainFlag.Swamp.mask
      else if normalizedLandValue < wasteThreshold then TerrainFlag.Waste.mask
      else if normalizedLandValue < highlandThreshold then TerrainFlag.Highlands.mask
      else if normalizedLandValue < forestThreshold then TerrainFlag.Forest.mask
      else if normalizedLandValue < farmThreshold then TerrainFlag.Farm.mask
      else if normalizedLandValue < extraLakeThreshold then TerrainFlag.FreshWater.mask
      else TerrainFlag.Plains.mask

  private def deriveCentroids(
      widthPixels: Int,
      heightPixels: Int,
      provinceCount: Int,
      provinceIdentifierByPixel: Array[Int],
      provinceSeeds: Vector[ProvinceSeed],
      oldIdentifierToNewIdentifier: Map[Int, Int]
  ): Map[ProvinceId, ProvinceLocation] =
    val counts = Array.fill[Long](provinceCount + 1)(0L)
    val sumX = Array.fill[Long](provinceCount + 1)(0L)
    val sumY = Array.fill[Long](provinceCount + 1)(0L)

    var yPixel = 0
    while yPixel < heightPixels do
      var xPixel = 0
      while xPixel < widthPixels do
        val provinceIdentifier = provinceIdentifierByPixel(yPixel * widthPixels + xPixel)
        counts(provinceIdentifier) += 1L
        sumX(provinceIdentifier) += xPixel.toLong
        sumY(provinceIdentifier) += yPixel.toLong
        xPixel += 1
      yPixel += 1

    provinceSeeds.map { provinceSeed =>
      val provinceIdentifier = oldIdentifierToNewIdentifier.getOrElse(provinceSeed.provinceId.value, provinceSeed.provinceId.value)
      val location =
        if counts(provinceIdentifier) > 0 then
          val centroidXPixel = sumX(provinceIdentifier).toDouble / counts(provinceIdentifier).toDouble
          val centroidYPixel = sumY(provinceIdentifier).toDouble / counts(provinceIdentifier).toDouble
          ProvinceLocation(
            XCell(math.floor(centroidXPixel / 256.0).toInt),
            YCell(math.floor(centroidYPixel / 160.0).toInt)
          )
        else
          ProvinceLocation(
            XCell(math.floor(provinceSeed.xPixel / 256.0).toInt),
            YCell(math.floor(provinceSeed.yPixel / 160.0).toInt)
          )
      ProvinceId(provinceIdentifier) -> location
    }.toMap
