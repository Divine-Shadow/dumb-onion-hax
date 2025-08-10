package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.effect.Sync
import cats.syntax.all.*
import javax.swing.{ButtonGroup, JOptionPane, JPanel, JRadioButton}

trait WrapChoiceService[Sequencer[_]]:
  protected def wrapChoiceService: WrapChoiceService[Sequencer] = this

  def chooseWrap[ErrorChannel[_]]()(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[WrapChoice]]

class WrapChoiceServiceImpl[Sequencer[_]](using Sync[Sequencer])
    extends WrapChoiceService[Sequencer]:

  override def chooseWrap[ErrorChannel[_]]()(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[WrapChoice]] =
    Sync[Sequencer].delay {
      val h = new JRadioButton("hwrap")
      val v = new JRadioButton("vwrap")
      val n = new JRadioButton("no-wrap")
      val group = new ButtonGroup()
      group.add(h); group.add(v); group.add(n)
      h.setSelected(true)
      val panel = new JPanel()
      panel.add(h); panel.add(v); panel.add(n)
      val result = JOptionPane.showConfirmDialog(
        null,
        panel,
        "Select wrap",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE
      )
      if result == JOptionPane.OK_OPTION then
        val choice =
          if h.isSelected then WrapChoice.HWrap
          else if v.isSelected then WrapChoice.VWrap
          else WrapChoice.NoWrap
        errorChannel.pure(choice)
      else errorChannel.raiseError[WrapChoice](RuntimeException("Wrap selection cancelled"))
    }
