package com.crib.bills.dom6maps.apps

import org.scalacheck.Properties

object HelloAppSpec extends Properties("HelloApp") {
  property("greeting is hello") =
    HelloApp.greeting == "hello"
}
