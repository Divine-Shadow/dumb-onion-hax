package com.crib.bills.dom6maps.apps

import cats.effect.IO
import weaver.SimpleIOSuite

object MapEditorAppSpec extends SimpleIOSuite:
  test("app runs") {
    MapEditorApp.main(Array.empty)
    IO.pure(expect(true))
  }
