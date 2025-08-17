package com.crib.bills.dom6maps
package model.map

import fs2.Stream

final case class MapLayer[F[_]](
    state: MapState,
    passThrough: Stream[F, MapDirective]
)
