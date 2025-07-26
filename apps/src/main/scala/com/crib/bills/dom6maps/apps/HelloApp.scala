package com.crib.bills.dom6maps.apps

object HelloApp extends Greeting with App {
  println(greeting)
}

trait Greeting {
  lazy val greeting: String = "hello"
}
