package com.crib.bills.dom6maps
package model.map.image

import weaver.SimpleIOSuite
import cats.effect.IO

object TargaImageEncoderSpec extends SimpleIOSuite:
  test("encodes raw bottom-left 24-bit targa header and payload") {
    val rgb = Array[Byte](
      10.toByte,
      20.toByte,
      30.toByte,
      40.toByte,
      50.toByte,
      60.toByte
    )

    val encoded = TargaImageEncoder.encodeRawBottomLeft24Bit(2, 1, rgb).toOption.get

    val headerChecks = expect.all(
      encoded(2) == 2.toByte,
      encoded(12) == 2.toByte,
      encoded(13) == 0.toByte,
      encoded(14) == 1.toByte,
      encoded(15) == 0.toByte,
      encoded(16) == 24.toByte,
      encoded(17) == 0.toByte
    )

    val payloadChecks =
      expect.all(
        encoded(18) == 30.toByte,
        encoded(19) == 20.toByte,
        encoded(20) == 10.toByte,
        encoded(21) == 60.toByte,
        encoded(22) == 50.toByte,
        encoded(23) == 40.toByte
      )

    IO(expect(encoded.length == 24) and headerChecks and payloadChecks)
  }

  test("encodes RLE bottom-left 24-bit targa header and payload") {
    val rgb = Array[Byte](
      10.toByte,
      20.toByte,
      30.toByte,
      40.toByte,
      50.toByte,
      60.toByte
    )

    val encoded = TargaImageEncoder.encodeRleBottomLeft24Bit(2, 1, rgb).toOption.get

    val headerChecks = expect.all(
      encoded(2) == 10.toByte,
      encoded(12) == 2.toByte,
      encoded(13) == 0.toByte,
      encoded(14) == 1.toByte,
      encoded(15) == 0.toByte,
      encoded(16) == 24.toByte,
      encoded(17) == 0.toByte
    )

    // Raw packet with two pixels (header 0x01), then BGR payload.
    val payloadChecks =
      expect.all(
        encoded(18) == 1.toByte,
        encoded(19) == 30.toByte,
        encoded(20) == 20.toByte,
        encoded(21) == 10.toByte,
        encoded(22) == 60.toByte,
        encoded(23) == 50.toByte,
        encoded(24) == 40.toByte
      )

    IO(expect(encoded.length == 25) and headerChecks and payloadChecks)
  }

  test("encodes repeated pixels as RLE packet") {
    val rgb = Array[Byte](
      10.toByte,
      20.toByte,
      30.toByte,
      10.toByte,
      20.toByte,
      30.toByte
    )
    val encoded = TargaImageEncoder.encodeRleBottomLeft24Bit(2, 1, rgb).toOption.get
    val checks =
      expect.all(
        encoded(18) == (0x80 | 1).toByte,
        encoded(19) == 30.toByte,
        encoded(20) == 20.toByte,
        encoded(21) == 10.toByte
      )
    IO(checks)
  }

  test("returns error when RGB byte length is invalid") {
    val encoded = TargaImageEncoder.encodeRleBottomLeft24Bit(2, 2, Array[Byte](1, 2, 3))
    IO(expect(encoded.isLeft))
  }

  test("preserves source row order across multiple rows") {
    val rgb = Array[Byte](
      255.toByte,
      0.toByte,
      0.toByte,
      0.toByte,
      0.toByte,
      255.toByte
    )
    val encoded = TargaImageEncoder.encodeRleBottomLeft24Bit(1, 2, rgb).toOption.get
    // Payload starts from bottom row under this encoder path.
    val checks = expect.all(
      encoded(18) == 1.toByte,
      encoded(19) == 255.toByte,
      encoded(20) == 0.toByte,
      encoded(21) == 0.toByte,
      encoded(22) == 0.toByte,
      encoded(23) == 0.toByte,
      encoded(24) == 255.toByte
    )
    IO(checks)
  }
