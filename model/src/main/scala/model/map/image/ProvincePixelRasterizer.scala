package com.crib.bills.dom6maps
package model.map.image

import model.ProvinceId
import model.map.Pb

object ProvincePixelRasterizer:
  final case class ProvincePixelOwnership(
      widthPixels: Int,
      heightPixels: Int,
      provinceIdentifierByPixel: Array[Int]
  ):
    def provinceIdentifierAt(x: Int, y: Int): Int =
      provinceIdentifierByPixel(y * widthPixels + x)

  def rasterize(
      widthPixels: Int,
      heightPixels: Int,
      provinceRuns: Vector[Pb]
  ): Either[Throwable, ProvincePixelOwnership] =
    if widthPixels <= 0 || heightPixels <= 0 then
      Left(IllegalArgumentException(s"Invalid map dimensions width=$widthPixels height=$heightPixels"))
    else
      val provinceIdentifierByPixel = Array.fill(widthPixels * heightPixels)(0)
      provinceRuns.foreach { run =>
        paintRun(
          widthPixels,
          heightPixels,
          provinceIdentifierByPixel,
          run.x,
          run.y,
          run.length,
          run.province
        )
      }
      Right(ProvincePixelOwnership(widthPixels, heightPixels, provinceIdentifierByPixel))

  private def paintRun(
      widthPixels: Int,
      heightPixels: Int,
      provinceIdentifierByPixel: Array[Int],
      x: Int,
      y: Int,
      length: Int,
      provinceId: ProvinceId
  ): Unit =
    if y >= 0 && y < heightPixels && length > 0 then
      val yPixelTopOrigin = (heightPixels - 1) - y
      val startX = math.max(0, x)
      val endExclusive = math.min(widthPixels, x + length)
      var currentX = startX
      while currentX < endExclusive do
        provinceIdentifierByPixel(yPixelTopOrigin * widthPixels + currentX) = provinceId.value
        currentX += 1
