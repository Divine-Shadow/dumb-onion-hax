package com.crib.bills.dom6maps
package apps.services.mapeditor

import model.ProvinceId
import model.map.{MapHeight, MapState, MapWidth, ProvinceLocation, ProvinceLocations, WrapState}

object WrapSeverService:
  def isTopBottom(
      a: ProvinceId,
      b: ProvinceId,
      index: ProvinceLocations,
      height: MapHeight
  ): Boolean =
    (index.locationOf(a), index.locationOf(b)) match
      case (Some(locA), Some(locB)) =>
        val top = height.value - 1
        val bottom = 0
        (locA.y.value == top && locB.y.value == bottom) || (locA.y.value == bottom && locB.y.value == top)
      case _ => false

  def isLeftRight(
      a: ProvinceId,
      b: ProvinceId,
      index: ProvinceLocations,
      width: MapWidth
  ): Boolean =
    (index.locationOf(a), index.locationOf(b)) match
      case (Some(locA), Some(locB)) =>
        val left  = 0
        val right = width.value - 1
        (locA.x.value == left && locB.x.value == right) ||
          (locA.x.value == right && locB.x.value == left)
      case _ => false

  def severVertically(state: MapState): MapState =
    state.size match
      case Some(sz) =>
        // Calculate actual map height from province locations
        val maxY = state.provinceLocations.indexByProvinceId.values.map(_.y.value).maxOption.getOrElse(0)
        val height = MapHeight(maxY + 1)
        val shouldSever = state.wrap == WrapState.FullWrap || state.wrap == WrapState.VerticalWrap
        val newAdj =
          if shouldSever then
            state.adjacency.filterNot((a, b) => isTopBottom(a, b, state.provinceLocations, height))
          else state.adjacency
        val newBorders =
          if shouldSever then
            state.borders.filterNot(b => isTopBottom(b.a, b.b, state.provinceLocations, height))
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
        // Calculate actual map width from province locations
        val maxX = state.provinceLocations.indexByProvinceId.values.map(_.x.value).maxOption.getOrElse(0)
        val width = MapWidth(maxX + 1)
        val newAdj     = state.adjacency.filterNot((a, b) => isLeftRight(a, b, state.provinceLocations, width))
        val newBorders = state.borders.filterNot(b => isLeftRight(b.a, b.b, state.provinceLocations, width))
        val newWrap = state.wrap match
          case WrapState.FullWrap       => WrapState.VerticalWrap
          case WrapState.HorizontalWrap => WrapState.NoWrap
          case WrapState.VerticalWrap   => WrapState.VerticalWrap
          case _                        => WrapState.NoWrap
        state.copy(adjacency = newAdj, borders = newBorders, wrap = newWrap)
      case None => state
