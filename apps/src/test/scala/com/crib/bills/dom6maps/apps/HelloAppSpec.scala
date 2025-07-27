package com.crib.bills.dom6maps.apps

import cats.effect.IO
import weaver.SimpleIOSuite

object HelloAppSpec extends SimpleIOSuite:
  test("greeting is hello") {
    IO.pure(expect(HelloApp.greeting == "hello"))
  }
