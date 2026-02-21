package com.crib.bills.dom6maps
package model.map.image

object ProvinceAnchorLocator:
  private final case class Candidate(
      preferInterior: Int,
      squaredDistance: Double,
      pixelIndex: Int
  )

  def locateAnchorPixelByProvince(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership
  ): Map[Int, Int] =
    val maxProvinceIdentifier = ownership.provinceIdentifierByPixel.foldLeft(0)(math.max)
    if maxProvinceIdentifier <= 0 then Map.empty
    else
      val counts = Array.fill[Long](maxProvinceIdentifier + 1)(0L)
      val sumX = Array.fill[Long](maxProvinceIdentifier + 1)(0L)
      val sumY = Array.fill[Long](maxProvinceIdentifier + 1)(0L)

      var yPixel = 0
      while yPixel < ownership.heightPixels do
        var xPixel = 0
        while xPixel < ownership.widthPixels do
          val provinceIdentifier = ownership.provinceIdentifierAt(xPixel, yPixel)
          if provinceIdentifier > 0 then
            counts(provinceIdentifier) += 1L
            sumX(provinceIdentifier) += xPixel.toLong
            sumY(provinceIdentifier) += yPixel.toLong
          xPixel += 1
        yPixel += 1

      val centroidX = Array.fill[Double](maxProvinceIdentifier + 1)(0.0)
      val centroidY = Array.fill[Double](maxProvinceIdentifier + 1)(0.0)
      var provinceIdentifier = 1
      while provinceIdentifier <= maxProvinceIdentifier do
        if counts(provinceIdentifier) > 0 then
          centroidX(provinceIdentifier) = sumX(provinceIdentifier).toDouble / counts(provinceIdentifier).toDouble
          centroidY(provinceIdentifier) = sumY(provinceIdentifier).toDouble / counts(provinceIdentifier).toDouble
        provinceIdentifier += 1

      val bestCandidate = Array.fill[Option[Candidate]](maxProvinceIdentifier + 1)(None)
      yPixel = 0
      while yPixel < ownership.heightPixels do
        var xPixel = 0
        while xPixel < ownership.widthPixels do
          val identifier = ownership.provinceIdentifierAt(xPixel, yPixel)
          if identifier > 0 then
            val deltaX = xPixel.toDouble - centroidX(identifier)
            val deltaY = yPixel.toDouble - centroidY(identifier)
            val squaredDistance = deltaX * deltaX + deltaY * deltaY
            val candidate = Candidate(
              preferInterior = if isInteriorPixel(ownership, xPixel, yPixel, identifier) then 0 else 1,
              squaredDistance = squaredDistance,
              pixelIndex = yPixel * ownership.widthPixels + xPixel
            )
            bestCandidate(identifier) =
              bestCandidate(identifier) match
                case Some(existing) if !isBetter(candidate, existing) => Some(existing)
                case _ => Some(candidate)
          xPixel += 1
        yPixel += 1

      (1 to maxProvinceIdentifier)
        .flatMap { identifier =>
          bestCandidate(identifier).map(candidate => identifier -> candidate.pixelIndex)
        }
        .toMap

  private def isBetter(candidate: Candidate, existing: Candidate): Boolean =
    if candidate.preferInterior != existing.preferInterior then candidate.preferInterior < existing.preferInterior
    else if candidate.squaredDistance != existing.squaredDistance then candidate.squaredDistance < existing.squaredDistance
    else candidate.pixelIndex < existing.pixelIndex

  private def isInteriorPixel(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      xPixel: Int,
      yPixel: Int,
      provinceIdentifier: Int
  ): Boolean =
    sameProvinceAt(ownership, xPixel - 1, yPixel, provinceIdentifier) &&
      sameProvinceAt(ownership, xPixel + 1, yPixel, provinceIdentifier) &&
      sameProvinceAt(ownership, xPixel, yPixel - 1, provinceIdentifier) &&
      sameProvinceAt(ownership, xPixel, yPixel + 1, provinceIdentifier)

  private def sameProvinceAt(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      xPixel: Int,
      yPixel: Int,
      provinceIdentifier: Int
  ): Boolean =
    if xPixel < 0 || yPixel < 0 || xPixel >= ownership.widthPixels || yPixel >= ownership.heightPixels then false
    else ownership.provinceIdentifierAt(xPixel, yPixel) == provinceIdentifier
