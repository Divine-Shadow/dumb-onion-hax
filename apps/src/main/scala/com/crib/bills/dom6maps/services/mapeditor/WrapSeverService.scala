package com.crib.bills.dom6maps
package apps.services.mapeditor

import model.ProvinceId
import model.map.{MapHeight, MapState, MapWidth, ProvinceLocation, WrapState}

object WrapSeverService:
  def isTopBottom(
      a: ProvinceId,
      b: ProvinceId,
      index: Map[ProvinceId, ProvinceLocation],
      height: MapHeight
  ): Boolean =
    (index.get(a), index.get(b)) match
      case (Some(locA), Some(locB)) =>
        val top = height.value - 1
        val bottom = 0
        (locA.y.value == top && locB.y.value == bottom) || (locA.y.value == bottom && locB.y.value == top)
      case _ => false

  def isLeftRight(
      a: ProvinceId,
      b: ProvinceId,
      index: Map[ProvinceId, ProvinceLocation],
      width: MapWidth
  ): Boolean =
    (index.get(a), index.get(b)) match
      case (Some(locA), Some(locB)) =>
        val left = 0
        val right = width.value - 1
        locA.y.value == locB.y.value &&
          ((locA.x.value == left && locB.x.value == right) ||
            (locA.x.value == right && locB.x.value == left))
      case _ => false

  def severVertically(state: MapState): MapState =
    state.size match
      case Some(sz) =>
        val height = MapHeight(sz.value)
        val index = state.provinceLocations.map(_.swap)
        val shouldSever = state.wrap == WrapState.FullWrap || state.wrap == WrapState.VerticalWrap
        val newAdj =
          if shouldSever then
            state.adjacency.filterNot((a, b) => isTopBottom(a, b, index, height))
          else state.adjacency
        val newBorders =
          if shouldSever then
            state.borders.filterNot(b => isTopBottom(b.a, b.b, index, height))
          else state.borders
        val newWrap = state.wrap match
          case WrapState.FullWrap     => WrapState.HorizontalWrap
          case WrapState.VerticalWrap => WrapState.NoWrap
          case WrapState.HorizontalWrap => WrapState.HorizontalWrap
          case _                      => WrapState.NoWrap
        state.copy(adjacency = newAdj, borders = newBorders, wrap = newWrap)
      case None => state

  def severHorizontally(state: MapState): MapState =
    state.size match
      case Some(sz) =>
        val width = MapWidth(sz.value)
        val index = state.provinceLocations.map(_.swap)
        val shouldSever = state.wrap == WrapState.FullWrap || state.wrap == WrapState.HorizontalWrap
        val newAdj =
          if shouldSever then
            state.adjacency.filterNot((a, b) => isLeftRight(a, b, index, width))
          else state.adjacency
        val newBorders =
          if shouldSever then
            state.borders.filterNot(b => isLeftRight(b.a, b.b, index, width))
          else state.borders
        val newWrap = state.wrap match
          case WrapState.FullWrap       => WrapState.VerticalWrap
          case WrapState.HorizontalWrap => WrapState.NoWrap
          case WrapState.VerticalWrap   => WrapState.VerticalWrap
          case _                        => WrapState.NoWrap
        state.copy(adjacency = newAdj, borders = newBorders, wrap = newWrap)
      case None => state
