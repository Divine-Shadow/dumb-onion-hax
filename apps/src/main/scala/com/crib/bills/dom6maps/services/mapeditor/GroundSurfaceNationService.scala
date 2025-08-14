package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.effect.Sync
import cats.syntax.all.*
import javax.swing.JOptionPane
import model.map.{SurfaceNation, UndergroundNation, DuelNations}
import model.Nation

trait GroundSurfaceNationService[Sequencer[_]]:
  def chooseNations[ErrorChannel[_]]()(using
      MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[DuelNations]]

class GroundSurfaceNationServiceImpl[Sequencer[_]](using Sync[Sequencer])
    extends GroundSurfaceNationService[Sequencer]:
  protected val sequencer = summon[Sync[Sequencer]]

  override def chooseNations[ErrorChannel[_]]()(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[DuelNations]] =
    for
      _ <- sequencer.delay(println("Prompting for nation selection"))
      result <- sequencer.delay {
        val panel = new GroundSurfaceNationPanel(Nation.Agartha_Early, Nation.Agartha_Early)
        val res = JOptionPane.showConfirmDialog(
          null,
          panel,
          "Select nations",
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE
        )
        if res == JOptionPane.OK_OPTION then
          val surface = SurfaceNation(panel.surface)
          val underground = UndergroundNation(panel.underground)
          errorChannel.pure(DuelNations(surface, underground))
        else errorChannel.raiseError[DuelNations](RuntimeException("Nation selection cancelled"))
      }
    yield result
