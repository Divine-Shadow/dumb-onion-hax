package com.crib.bills.dom6maps
package model.map.image

import java.io.ByteArrayOutputStream

object TargaImageEncoder:
  final case class TargaEncodingError(message: String) extends RuntimeException(message)

  def encodeRawBottomLeft24Bit(
      widthPixels: Int,
      heightPixels: Int,
      redGreenBlueBytes: Array[Byte]
  ): Either[Throwable, Array[Byte]] =
    val expectedLength = widthPixels * heightPixels * 3
    if widthPixels <= 0 || heightPixels <= 0 then
      Left(TargaEncodingError(s"Invalid image dimensions width=$widthPixels height=$heightPixels"))
    else if redGreenBlueBytes.length != expectedLength then
      Left(
        TargaEncodingError(
          s"RGB byte length mismatch expected=$expectedLength actual=${redGreenBlueBytes.length}"
        )
      )
    else
      val headerLength = 18
      val output = ByteArrayOutputStream(headerLength + expectedLength)

      // TGA header for type-2 (raw true-color) with bottom-left origin.
      output.write(0) // id length
      output.write(0) // color map type
      output.write(2) // image type
      output.write(0) // color map origin low
      output.write(0) // color map origin high
      output.write(0) // color map length low
      output.write(0) // color map length high
      output.write(0) // color map depth
      output.write(0) // x-origin low
      output.write(0) // x-origin high
      output.write(0) // y-origin low
      output.write(0) // y-origin high
      output.write(widthPixels & 0xff)
      output.write((widthPixels >>> 8) & 0xff)
      output.write(heightPixels & 0xff)
      output.write((heightPixels >>> 8) & 0xff)
      output.write(24) // bits per pixel
      output.write(0) // image descriptor (bottom-left)

      var yPixel = heightPixels - 1
      while yPixel >= 0 do
        var xPixel = 0
        while xPixel < widthPixels do
          val sourcePixelIndex = yPixel * widthPixels + xPixel
          val sourceByteIndex = sourcePixelIndex * 3
          val red = redGreenBlueBytes(sourceByteIndex) & 0xff
          val green = redGreenBlueBytes(sourceByteIndex + 1) & 0xff
          val blue = redGreenBlueBytes(sourceByteIndex + 2) & 0xff
          output.write(blue)
          output.write(green)
          output.write(red)
          xPixel += 1
        yPixel -= 1

      Right(output.toByteArray)

  def encodeRleBottomLeft24Bit(
      widthPixels: Int,
      heightPixels: Int,
      redGreenBlueBytes: Array[Byte]
  ): Either[Throwable, Array[Byte]] =
    val expectedLength = widthPixels * heightPixels * 3
    if widthPixels <= 0 || heightPixels <= 0 then
      Left(TargaEncodingError(s"Invalid image dimensions width=$widthPixels height=$heightPixels"))
    else if redGreenBlueBytes.length != expectedLength then
      Left(
        TargaEncodingError(
          s"RGB byte length mismatch expected=$expectedLength actual=${redGreenBlueBytes.length}"
        )
      )
    else
      val headerLength = 18
      val output = ByteArrayOutputStream(headerLength + expectedLength)
      val pixelCount = widthPixels * heightPixels

      // TGA header for type-10 (RLE true-color) with bottom-left origin.
      output.write(0) // id length
      output.write(0) // color map type
      output.write(10) // image type
      output.write(0) // color map origin low
      output.write(0) // color map origin high
      output.write(0) // color map length low
      output.write(0) // color map length high
      output.write(0) // color map depth
      output.write(0) // x-origin low
      output.write(0) // x-origin high
      output.write(0) // y-origin low
      output.write(0) // y-origin high
      output.write(widthPixels & 0xff)
      output.write((widthPixels >>> 8) & 0xff)
      output.write(heightPixels & 0xff)
      output.write((heightPixels >>> 8) & 0xff)
      output.write(24) // bits per pixel
      output.write(0) // image descriptor (bottom-left)

      val bgrPixels = Array.ofDim[Int](pixelCount)
      var writePixelIndex = 0
      var yPixel = heightPixels - 1
      while yPixel >= 0 do
        var xPixel = 0
        while xPixel < widthPixels do
          val sourcePixelIndex = yPixel * widthPixels + xPixel
          val sourceByteIndex = sourcePixelIndex * 3
          val red = redGreenBlueBytes(sourceByteIndex) & 0xff
          val green = redGreenBlueBytes(sourceByteIndex + 1) & 0xff
          val blue = redGreenBlueBytes(sourceByteIndex + 2) & 0xff
          bgrPixels(writePixelIndex) = (blue << 16) | (green << 8) | red
          writePixelIndex += 1
          xPixel += 1
        yPixel -= 1

      var readPixelIndex = 0
      while readPixelIndex < pixelCount do
        val repeatedPixelLength = repeatedRunLength(bgrPixels, readPixelIndex)
        if repeatedPixelLength >= 2 then
          output.write(0x80 | (repeatedPixelLength - 1))
          writeBgrPixel(output, bgrPixels(readPixelIndex))
          readPixelIndex += repeatedPixelLength
        else
          val rawStart = readPixelIndex
          readPixelIndex += 1
          var continueRaw = true
          while continueRaw && readPixelIndex < pixelCount && (readPixelIndex - rawStart) < 128 do
            if repeatedRunLength(bgrPixels, readPixelIndex) >= 2 then continueRaw = false
            else readPixelIndex += 1
          val rawLength = readPixelIndex - rawStart
          output.write(rawLength - 1)
          var rawIndex = rawStart
          while rawIndex < readPixelIndex do
            writeBgrPixel(output, bgrPixels(rawIndex))
            rawIndex += 1

      Right(output.toByteArray)

  private def repeatedRunLength(
      bgrPixels: Array[Int],
      startIndex: Int
  ): Int =
    val maxLength = math.min(128, bgrPixels.length - startIndex)
    val targetPixel = bgrPixels(startIndex)
    var length = 1
    while length < maxLength && bgrPixels(startIndex + length) == targetPixel do
      length += 1
    length

  private def writeBgrPixel(output: ByteArrayOutputStream, bgrPixel: Int): Unit =
    output.write((bgrPixel >>> 16) & 0xff)
    output.write((bgrPixel >>> 8) & 0xff)
    output.write(bgrPixel & 0xff)
