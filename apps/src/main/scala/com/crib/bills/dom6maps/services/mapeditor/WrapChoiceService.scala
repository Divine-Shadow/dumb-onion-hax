package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.effect.Sync
import cats.syntax.all.*
import javax.swing.{BoxLayout, JCheckBox, JOptionPane, JPanel}

trait WrapChoiceService[Sequencer[_]]:
  protected def wrapChoiceService: WrapChoiceService[Sequencer] = this

  def chooseWraps[ErrorChannel[_]]()(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[WrapChoices]]

class WrapChoiceServiceImpl[Sequencer[_]](using Sync[Sequencer])
    extends WrapChoiceService[Sequencer]:
  protected val sequencer = summon[Sync[Sequencer]]

  override def chooseWraps[ErrorChannel[_]]()(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[WrapChoices]] =
    for
      _ <- sequencer.delay(println("Prompting for wrap selection"))
      result <- sequencer.delay {
        val mainPanel = new WrapChoicePanel(WrapChoice.HWrap, allowGroundSurfaceDuel = true)
        val cavePanel = new WrapChoicePanel(WrapChoice.HWrap)
        cavePanel.setEnabledAll(false)
        val caveBox = new JCheckBox("modify cave layer")
        def updateCave(): Unit =
          val duel = mainPanel.choice == WrapChoice.GroundSurfaceDuel
          caveBox.setEnabled(!duel)
          cavePanel.setEnabledAll(!duel && caveBox.isSelected)
        caveBox.addActionListener(_ => updateCave())
        mainPanel.onChange(updateCave())
        val panel = new JPanel()
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS))
        panel.add(mainPanel)
        panel.add(caveBox)
        panel.add(cavePanel)
        updateCave()
        val res = JOptionPane.showConfirmDialog(
          null,
          panel,
          "Select wrap",
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE
        )
        if res == JOptionPane.OK_OPTION then
          val mainChoice = mainPanel.choice
          val caveChoice =
            if caveBox.isSelected && mainChoice != WrapChoice.GroundSurfaceDuel then Some(cavePanel.choice)
            else None
          errorChannel.pure(WrapChoices(mainChoice, caveChoice))
        else errorChannel.raiseError[WrapChoices](RuntimeException("Wrap selection cancelled"))
      }
    yield result
