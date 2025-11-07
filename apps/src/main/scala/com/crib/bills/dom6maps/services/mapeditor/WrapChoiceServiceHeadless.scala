package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.effect.Sync
import cats.syntax.all.*
import model.map.ThroneFeatureConfig

/**
 * Headless implementation: avoids any Swing UI and simply returns
 * a default wrap choice alongside the provided throne configuration.
 *
 * Defaults:
 * - main wrap: HWrap
 * - cave wrap: None (unchanged)
 *
 * Behavior can be influenced via optional system properties:
 * - dom6.wrap.main = hwrap|vwrap|full|none|duel
 * - dom6.wrap.cave = hwrap|vwrap|full|none  (ignored if main=duel)
 */
class WrapChoiceServiceHeadlessImpl[Sequencer[_]](using Sync[Sequencer])
    extends WrapChoiceService[Sequencer]:
  private val sequencer = summon[Sync[Sequencer]]

  private def parseWrap(s: String): Option[WrapChoice] =
    s.trim.toLowerCase match
      case "hwrap" => Some(WrapChoice.HWrap)
      case "vwrap" => Some(WrapChoice.VWrap)
      case "full"  => Some(WrapChoice.FullWrap)
      case "none"  => Some(WrapChoice.NoWrap)
      case "duel"  => Some(WrapChoice.GroundSurfaceDuel)
      case _        => None

  override def chooseSettings[ErrorChannel[_]](
      config: ThroneFeatureConfig,
      caveAvailability: CaveLayerAvailability
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[MapEditorSettings]] =
    for
      mainStr <- sequencer.delay(sys.props.getOrElse("dom6.wrap.main", "hwrap"))
      caveStr <- sequencer.delay(sys.props.get("dom6.wrap.cave"))
      main    <- sequencer.pure(parseWrap(mainStr).getOrElse(WrapChoice.HWrap))
      cave    <- sequencer.pure(caveStr.flatMap(parseWrap))
      wraps =
        if main == WrapChoice.GroundSurfaceDuel then WrapChoices(main, None)
        else WrapChoices(main, cave)
    yield errorChannel.pure(MapEditorSettings(wraps, config, MagicSiteSelection.Disabled))
