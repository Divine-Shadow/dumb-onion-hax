package com.crib.bills.dom6maps
package apps.services.mapeditor

import model.ProvinceId
import model.map.{MapHeight, MapState, MapWidth, WrapState}

object WrapSeverService:
  def isTopBottom(
      a: ProvinceId,
      b: ProvinceId,
      width: MapWidth,
      height: MapHeight
  ): Boolean =
    val rowA = ((a.value - 1) / width.value) + 1
    val rowB = ((b.value - 1) / width.value) + 1
    val top = height.value
    val bottom = 1
    (rowA == top && rowB == bottom) || (rowA == bottom && rowB == top)

  def isLeftRight(a: ProvinceId, b: ProvinceId, width: MapWidth): Boolean =
    val rowA = ((a.value - 1) / width.value) + 1
    val rowB = ((b.value - 1) / width.value) + 1
    val colA = ((a.value - 1) % width.value) + 1
    val colB = ((b.value - 1) % width.value) + 1
    val left = 1
    val right = width.value
    rowA == rowB && ((colA == left && colB == right) || (colA == right && colB == left))

  def severVertically(state: MapState): MapState =
    state.size match
      case Some(sz) =>
        val width = MapWidth(sz.value)
        val height = MapHeight(sz.value)
        val shouldSever = state.wrap == WrapState.FullWrap || state.wrap == WrapState.VerticalWrap
        val newAdj =
          if shouldSever then
            state.adjacency.filterNot((a, b) => isTopBottom(a, b, width, height))
          else state.adjacency
        val newBorders =
          if shouldSever then
            state.borders.filterNot(b => isTopBottom(b.a, b.b, width, height))
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
        val height = MapHeight(sz.value)
        val shouldSever = state.wrap == WrapState.FullWrap || state.wrap == WrapState.HorizontalWrap
        val newAdj =
          if shouldSever then
            state.adjacency.filterNot((a, b) => isLeftRight(a, b, width))
          else state.adjacency
        val newBorders =
          if shouldSever then
            state.borders.filterNot(b => isLeftRight(b.a, b.b, width))
          else state.borders
        val newWrap = state.wrap match
          case WrapState.FullWrap       => WrapState.VerticalWrap
          case WrapState.HorizontalWrap => WrapState.NoWrap
          case WrapState.VerticalWrap   => WrapState.VerticalWrap
          case _                        => WrapState.NoWrap
        state.copy(adjacency = newAdj, borders = newBorders, wrap = newWrap)
      case None => state
