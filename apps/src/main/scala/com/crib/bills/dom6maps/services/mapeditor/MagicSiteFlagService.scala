package com.crib.bills.dom6maps
package apps.services.mapeditor

import model.{TerrainFlag, TerrainMask}
import model.map.{MapState, Terrain}

trait MagicSiteFlagService:
  def apply(state: MapState, toggle: MagicSiteToggle): MapState

final class MagicSiteFlagServiceImpl extends MagicSiteFlagService:
  override def apply(state: MapState, toggle: MagicSiteToggle): MapState =
    toggle match
      case MagicSiteToggle.Enabled =>
        val updatedTerrains = state.terrains.map {
          case terrain @ Terrain(_, mask) =>
            val updatedMask = TerrainMask(mask).withFlag(TerrainFlag.ManySites)
            terrain.copy(mask = updatedMask.value)
        }
        state.copy(terrains = updatedTerrains)
      case MagicSiteToggle.Disabled =>
        state
