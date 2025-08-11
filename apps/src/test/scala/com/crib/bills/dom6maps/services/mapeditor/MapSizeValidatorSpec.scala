package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.instances.either.*
import weaver.SimpleIOSuite
import model.map.*

object MapSizeValidatorSpec extends SimpleIOSuite:
  type EC[A] = Either[Throwable, A]

  test("validate returns map size when surface and cave sizes match") {
    val validator = new MapSizeValidatorImpl[IO]
    for
      size <- IO.fromEither(MapSize.from(5))
      surface = MapState.empty.copy(size = Some(size))
      cave = MapState.empty.copy(size = Some(size))
      result <- validator.validate[EC](surface, cave)
      (returnedSize, _, _) <- IO.fromEither(result)
    yield expect(returnedSize == size)
  }

  test("validate raises IllegalArgumentException when sizes differ") {
    val validator = new MapSizeValidatorImpl[IO]
    for
      surfSize <- IO.fromEither(MapSize.from(5))
      caveSize <- IO.fromEither(MapSize.from(7))
      surface = MapState.empty.copy(size = Some(surfSize))
      cave = MapState.empty.copy(size = Some(caveSize))
      result <- validator.validate[EC](surface, cave)
    yield expect(result match
      case Left(_: IllegalArgumentException) => true
      case _                                 => false
    )
  }

  test("validate raises IllegalArgumentException when size missing") {
    val validator = new MapSizeValidatorImpl[IO]
    for
      size <- IO.fromEither(MapSize.from(5))
      surface = MapState.empty
      cave = MapState.empty.copy(size = Some(size))
      result <- validator.validate[EC](surface, cave)
    yield expect(result match
      case Left(_: IllegalArgumentException) => true
      case _                                 => false
    )
  }
