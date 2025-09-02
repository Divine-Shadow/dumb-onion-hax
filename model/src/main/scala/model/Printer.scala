package com.crib.bills.dom6maps
package model

trait Printer[F[_]]:
  def println(value: String): F[Unit]
