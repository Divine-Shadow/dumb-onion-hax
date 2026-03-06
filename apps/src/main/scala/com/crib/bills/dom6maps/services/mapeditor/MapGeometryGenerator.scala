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
        input.wrapState,
        input.seed,
        input.noiseScale
      )
      val remap = remapProvinceIdentifiersByFirstPixelScanOrder(
        widthPixels,
        heightPixels,
        provinceIdentifierByPixel,
        input.provinceCount
      )
      val provinceRuns = buildProvinceRuns(widthPixels, heightPixels, remap.provinceIdentifierByPixel)
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
      Right(
        GeneratedGeometry(
          provincePixelRuns = provinceRuns,
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
    val warpAmplitudePixels = 14.0 * clampedNoiseScale
    val phaseA = (splitMix64(seed ^ 0x9e3779b97f4a7c15L) & 0xffff).toDouble / 32768.0
    val phaseB = (splitMix64(seed ^ 0xbf58476d1ce4e5b9L) & 0xffff).toDouble / 32768.0
    val phaseC = (splitMix64(seed ^ 0x94d049bb133111ebL) & 0xffff).toDouble / 32768.0

    def warpedCoordinate(
        xPixel: Int,
        yPixel: Int
    ): (Double, Double) =
      val x = xPixel.toDouble
      val y = yPixel.toDouble
      val angleX = 2.0 * math.Pi * x / widthPixels.toDouble
      val angleY = 2.0 * math.Pi * y / heightPixels.toDouble
      val warpX =
        math.sin(3.0 * angleY + phaseA) * 0.40 +
          math.sin(7.0 * angleX + 5.0 * angleY + phaseB) * 0.40 +
          math.sin(13.0 * angleX - 2.0 * angleY + phaseC) * 0.20
      val warpY =
        math.sin(3.0 * angleX + phaseB) * 0.40 +
          math.sin(5.0 * angleY + 4.0 * angleX + phaseC) * 0.40 +
          math.sin(11.0 * angleY - 3.0 * angleX + phaseA) * 0.20
      (
        x + warpX * warpAmplitudePixels,
        y + warpY * warpAmplitudePixels
      )

    val provinceIdentifierByPixel = Array.ofDim[Int](widthPixels * heightPixels)
    var yPixel = 0
    while yPixel < heightPixels do
      var xPixel = 0
      while xPixel < widthPixels do
        val (warpedX, warpedY) = warpedCoordinate(xPixel, yPixel)
        var bestDistance = Double.MaxValue
        var bestProvinceIdentifier = 0
        var seedIndex = 0
        while seedIndex < provinceSeeds.length do
          val provinceSeed = provinceSeeds(seedIndex)
          val distance = wrappedSquaredDistance(
            warpedX,
            warpedY,
            provinceSeed.xPixel,
            provinceSeed.yPixel,
            widthPixels,
            heightPixels,
            wrapState
          )
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
