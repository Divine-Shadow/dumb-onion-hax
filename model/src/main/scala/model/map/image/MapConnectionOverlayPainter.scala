package com.crib.bills.dom6maps
package model.map.image

import model.{BorderFlag, ProvinceId}
import model.BorderFlag.includes
import model.map.Border

trait MapConnectionOverlayPainter:
  def paint(
      image: MapImagePainter.MapImage,
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      borders: Vector[Border]
  ): MapImagePainter.MapImage

object MapConnectionOverlayPainter:
  final class NoOpMapConnectionOverlayPainter extends MapConnectionOverlayPainter:
    override def paint(
        image: MapImagePainter.MapImage,
        ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
        borders: Vector[Border]
    ): MapImagePainter.MapImage = image

final class BorderFlagMapConnectionOverlayPainter(
    style: BorderFlagMapConnectionOverlayPainter.ConnectionStyle =
      BorderFlagMapConnectionOverlayPainter.defaultStyle
) extends MapConnectionOverlayPainter:
  import BorderFlagMapConnectionOverlayPainter.*

  override def paint(
      image: MapImagePainter.MapImage,
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      borders: Vector[Border]
  ): MapImagePainter.MapImage =
    if borders.isEmpty then image
    else
      val bytes = image.redGreenBlueBytes.clone()
      val borderInfoByPair = borders.foldLeft(Map.empty[ProvincePair, BorderInfo]) {
        case (accumulator, border) =>
          val provincePair = ProvincePair.normalized(border.a, border.b)
          val existing = accumulator.getOrElse(provincePair, BorderInfo.empty)
          accumulator.updated(provincePair, existing.withBorderFlag(border.flag))
      }

      paintBoundaryOverlays(ownership, bytes, borderInfoByPair)
      paintRoadOverlays(ownership, bytes, borderInfoByPair)

      MapImagePainter.MapImage(image.widthPixels, image.heightPixels, bytes)

  private def paintBoundaryOverlays(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      bytes: Array[Byte],
      borderInfoByPair: Map[ProvincePair, BorderInfo]
  ): Unit =
    var yPixel = 0
    while yPixel < ownership.heightPixels do
      var xPixel = 0
      while xPixel < ownership.widthPixels do
        val provinceIdentifier = ownership.provinceIdentifierAt(xPixel, yPixel)
        if provinceIdentifier > 0 then
          paintBoundaryAtNeighbour(ownership, bytes, borderInfoByPair, xPixel, yPixel, xPixel + 1, yPixel)
          paintBoundaryAtNeighbour(ownership, bytes, borderInfoByPair, xPixel, yPixel, xPixel, yPixel + 1)
        xPixel += 1
      yPixel += 1

  private def paintBoundaryAtNeighbour(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      bytes: Array[Byte],
      borderInfoByPair: Map[ProvincePair, BorderInfo],
      xPixel: Int,
      yPixel: Int,
      neighbourXPixel: Int,
      neighbourYPixel: Int
  ): Unit =
    if neighbourXPixel < 0 || neighbourXPixel >= ownership.widthPixels || neighbourYPixel < 0 || neighbourYPixel >= ownership.heightPixels then
      ()
    else
      val provinceIdentifier = ownership.provinceIdentifierAt(xPixel, yPixel)
      val neighbourProvinceIdentifier = ownership.provinceIdentifierAt(neighbourXPixel, neighbourYPixel)
      if provinceIdentifier > 0 && neighbourProvinceIdentifier > 0 && provinceIdentifier != neighbourProvinceIdentifier then
        val provincePair = ProvincePair.normalized(ProvinceId(provinceIdentifier), ProvinceId(neighbourProvinceIdentifier))
        borderInfoByPair.get(provincePair).foreach { borderInfo =>
          if borderInfo.hasRiver then
            paintBoundaryStroke(
              ownership,
              bytes,
              xPixel,
              yPixel,
              neighbourXPixel,
              neighbourYPixel,
              style.riverColor,
              style.riverThickness,
              dotted = false
            )
          if borderInfo.hasImpassableMountain then
            paintBoundaryStroke(
              ownership,
              bytes,
              xPixel,
              yPixel,
              neighbourXPixel,
              neighbourYPixel,
              style.mountainColor,
              style.mountainThickness,
              dotted = false
            )
          if borderInfo.hasMountainPass then
            paintBoundaryStroke(
              ownership,
              bytes,
              xPixel,
              yPixel,
              neighbourXPixel,
              neighbourYPixel,
              style.mountainPassColor,
              style.mountainPassThickness,
              dotted = true
            )
        }

  private def paintBoundaryStroke(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      bytes: Array[Byte],
      xPixel: Int,
      yPixel: Int,
      neighbourXPixel: Int,
      neighbourYPixel: Int,
      color: MapImagePainter.RedGreenBlueColor,
      thickness: Int,
      dotted: Boolean
  ): Unit =
    val shouldPaint =
      if !dotted then true
      else ((xPixel + yPixel + neighbourXPixel + neighbourYPixel) % style.mountainPassDotPeriodPixels) < style.mountainPassDotLengthPixels
    if shouldPaint then
      paintDisk(bytes, ownership.widthPixels, ownership.heightPixels, xPixel, yPixel, thickness, color)
      paintDisk(bytes, ownership.widthPixels, ownership.heightPixels, neighbourXPixel, neighbourYPixel, thickness, color)

  private def paintRoadOverlays(
      ownership: ProvincePixelRasterizer.ProvincePixelOwnership,
      bytes: Array[Byte],
      borderInfoByPair: Map[ProvincePair, BorderInfo]
  ): Unit =
    val roadPairs = borderInfoByPair.collect { case (provincePair, borderInfo) if borderInfo.hasRoad => provincePair }.toVector
    if roadPairs.nonEmpty then
      val anchorByProvinceIdentifier = ProvinceAnchorLocator.locateAnchorPixelByProvince(ownership)
      roadPairs.foreach { provincePair =>
        val fromProvinceIdentifier = provincePair.first.value
        val toProvinceIdentifier = provincePair.second.value
        for
          fromAnchorPixel <- anchorByProvinceIdentifier.get(fromProvinceIdentifier)
          toAnchorPixel <- anchorByProvinceIdentifier.get(toProvinceIdentifier)
        do
          val fromXPixel = fromAnchorPixel % ownership.widthPixels
          val fromYPixel = fromAnchorPixel / ownership.widthPixels
          val toXPixel = toAnchorPixel % ownership.widthPixels
          val toYPixel = toAnchorPixel / ownership.widthPixels
          paintLineExcludingEndpoints(
            bytes = bytes,
            widthPixels = ownership.widthPixels,
            heightPixels = ownership.heightPixels,
            fromXPixel = fromXPixel,
            fromYPixel = fromYPixel,
            toXPixel = toXPixel,
            toYPixel = toYPixel,
            color = style.roadColor,
            thickness = style.roadThickness
          )
      }

  private def paintLineExcludingEndpoints(
      bytes: Array[Byte],
      widthPixels: Int,
      heightPixels: Int,
      fromXPixel: Int,
      fromYPixel: Int,
      toXPixel: Int,
      toYPixel: Int,
      color: MapImagePainter.RedGreenBlueColor,
      thickness: Int
  ): Unit =
    val linePixels = rasterizeLine(fromXPixel, fromYPixel, toXPixel, toYPixel)
    if linePixels.length > 2 then
      linePixels.slice(1, linePixels.length - 1).foreach { case (xPixel, yPixel) =>
        paintDisk(bytes, widthPixels, heightPixels, xPixel, yPixel, thickness, color)
      }

  private def rasterizeLine(
      fromXPixel: Int,
      fromYPixel: Int,
      toXPixel: Int,
      toYPixel: Int
  ): Vector[(Int, Int)] =
    var xPixel = fromXPixel
    var yPixel = fromYPixel
    val deltaX = math.abs(toXPixel - fromXPixel)
    val deltaYPixel = math.abs(toYPixel - fromYPixel)
    val stepX = if fromXPixel < toXPixel then 1 else -1
    val stepY = if fromYPixel < toYPixel then 1 else -1
    var error = deltaX - deltaYPixel
    val points = scala.collection.mutable.ArrayBuffer.empty[(Int, Int)]

    while
      points += ((xPixel, yPixel))
      !(xPixel == toXPixel && yPixel == toYPixel)
    do
      val doubleError = error * 2
      if doubleError > -deltaYPixel then
        error -= deltaYPixel
        xPixel += stepX
      if doubleError < deltaX then
        error += deltaX
        yPixel += stepY

    points.toVector

  private def paintDisk(
      bytes: Array[Byte],
      widthPixels: Int,
      heightPixels: Int,
      centerXPixel: Int,
      centerYPixel: Int,
      radiusPixels: Int,
      color: MapImagePainter.RedGreenBlueColor
  ): Unit =
    val radiusSquared = radiusPixels * radiusPixels
    var yOffset = -radiusPixels
    while yOffset <= radiusPixels do
      var xOffset = -radiusPixels
      while xOffset <= radiusPixels do
        val distanceSquared = xOffset * xOffset + yOffset * yOffset
        if distanceSquared <= radiusSquared then
          val xPixel = centerXPixel + xOffset
          val yPixel = centerYPixel + yOffset
          if xPixel >= 0 && xPixel < widthPixels && yPixel >= 0 && yPixel < heightPixels then
            val pixelIndex = yPixel * widthPixels + xPixel
            val byteIndex = pixelIndex * 3
            bytes(byteIndex) = color.red.toByte
            bytes(byteIndex + 1) = color.green.toByte
            bytes(byteIndex + 2) = color.blue.toByte
        xOffset += 1
      yOffset += 1

object BorderFlagMapConnectionOverlayPainter:
  final case class ConnectionStyle(
      roadColor: MapImagePainter.RedGreenBlueColor,
      riverColor: MapImagePainter.RedGreenBlueColor,
      mountainColor: MapImagePainter.RedGreenBlueColor,
      mountainPassColor: MapImagePainter.RedGreenBlueColor,
      roadThickness: Int,
      riverThickness: Int,
      mountainThickness: Int,
      mountainPassThickness: Int,
      mountainPassDotPeriodPixels: Int,
      mountainPassDotLengthPixels: Int
  )

  val defaultStyle: ConnectionStyle =
    ConnectionStyle(
      roadColor = MapImagePainter.RedGreenBlueColor(145, 92, 46),
      riverColor = MapImagePainter.RedGreenBlueColor(35, 146, 232),
      mountainColor = MapImagePainter.RedGreenBlueColor(78, 78, 78),
      mountainPassColor = MapImagePainter.RedGreenBlueColor(160, 160, 160),
      roadThickness = 2,
      riverThickness = 2,
      mountainThickness = 2,
      mountainPassThickness = 2,
      mountainPassDotPeriodPixels = 10,
      mountainPassDotLengthPixels = 5
    )

  private final case class ProvincePair(first: ProvinceId, second: ProvinceId)

  private object ProvincePair:
    def normalized(leftProvince: ProvinceId, rightProvince: ProvinceId): ProvincePair =
      if leftProvince.value <= rightProvince.value then ProvincePair(leftProvince, rightProvince)
      else ProvincePair(rightProvince, leftProvince)

  private final case class BorderInfo(
      hasRoad: Boolean,
      hasRiver: Boolean,
      hasImpassableMountain: Boolean,
      hasMountainPass: Boolean
  ):
    def withBorderFlag(borderFlag: BorderFlag): BorderInfo =
      BorderInfo(
        hasRoad = hasRoad || borderFlag.includes(BorderFlag.Road),
        hasRiver = hasRiver || borderFlag.includes(BorderFlag.River),
        hasImpassableMountain = hasImpassableMountain || borderFlag.includes(BorderFlag.Impassable),
        hasMountainPass = hasMountainPass || borderFlag.includes(BorderFlag.MountainPass)
      )

  private object BorderInfo:
    val empty: BorderInfo = BorderInfo(
      hasRoad = false,
      hasRiver = false,
      hasImpassableMountain = false,
      hasMountainPass = false
    )
