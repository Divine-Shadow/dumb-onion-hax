package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.effect.Sync
import cats.syntax.all.*
import javax.swing.{BoxLayout, JCheckBox, JLabel, JOptionPane, JPanel}
import java.nio.file.{Files as JFiles, Path as NioPath}
import pureconfig.*
import model.map.ThroneFeatureConfig

trait WrapChoiceService[Sequencer[_]]:
  protected def wrapChoiceService: WrapChoiceService[Sequencer] = this

  def chooseSettings[ErrorChannel[_]](
      config: ThroneFeatureConfig
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[MapEditorSettings]]

class WrapChoiceServiceImpl[Sequencer[_]](using Sync[Sequencer])
    extends WrapChoiceService[Sequencer]:
  protected val sequencer = summon[Sync[Sequencer]]

  override def chooseSettings[ErrorChannel[_]](
      config: ThroneFeatureConfig
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[MapEditorSettings]] =
    for
      _ <- sequencer.delay(println("Prompting for editor settings"))
      result <-
        for
          mainPanel <- sequencer.delay(new WrapChoicePanel(WrapChoice.HWrap, allowGroundSurfaceDuel = true))
          cavePanel <- sequencer.delay(new WrapChoicePanel(WrapChoice.HWrap))
          _ <- sequencer.delay(cavePanel.setEnabledAll(false))
          caveBox <- sequencer.delay(new JCheckBox("modify cave layer"))
          throneSummary <- sequencer.delay {
            val fixedSummary = config.fixed
              .map { p =>
                val spec =
                  p.level.map(l => l.value.toString).orElse(p.id.map(fid => s"id=${fid.value}")).getOrElse("?")
                s"(${p.location.x.value},${p.location.y.value}):$spec"
              }
              .mkString(",")
            s"""Random L1: ${config.randomLevelOne.map(l => s"(${l.x.value},${l.y.value})").mkString(",")}
Random L2: ${config.randomLevelTwo.map(l => s"(${l.x.value},${l.y.value})").mkString(",")}
Fixed: $fixedSummary"""
          }
          throneLabel <- sequencer.delay(new JLabel(throneSummary))
          throneBox <- sequencer.delay(new JCheckBox("Override Thrones"))
          panel <- sequencer.delay {
            def updateCave(): Unit =
              val duel = mainPanel.choice == WrapChoice.GroundSurfaceDuel
              caveBox.setEnabled(!duel)
              cavePanel.setEnabledAll(!duel && caveBox.isSelected)
            caveBox.addActionListener(_ => updateCave())
            mainPanel.onChange(updateCave())
            val p = new JPanel()
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS))
            p.add(mainPanel)
            p.add(caveBox)
            p.add(cavePanel)
            p.add(throneLabel)
            p.add(throneBox)
            updateCave()
            p
          }
          res <- sequencer.delay(
            JOptionPane.showConfirmDialog(
              null,
              panel,
              "Select settings",
              JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.PLAIN_MESSAGE
            )
          )
          settings <-
            if res == JOptionPane.OK_OPTION then
              val mainChoice = mainPanel.choice
              val caveChoice =
                if caveBox.isSelected && mainChoice != WrapChoice.GroundSurfaceDuel then Some(cavePanel.choice)
                else None
              val wraps = WrapChoices(mainChoice, caveChoice)
              if throneBox.isSelected then
                val path = NioPath.of("throne-override.conf")
                for
                  exists <- sequencer.delay(JFiles.exists(path))
                  _ <-
                    if exists then sequencer.unit
                    else sequencer.delay(
                      JFiles.writeString(
                        path,
                        """overrides = [
  { x = 0, y = 0, level = 3 },
  { x = 4, y = 2, level = 1 }
]
"""
                      )
                    ).void
                  loaded <- sequencer.delay(ConfigSource.file(path).load[ThroneConfiguration])
                yield loaded
                  .leftMap(f => RuntimeException(f.toString))
                  .map(cfg => MapEditorSettings(wraps, config.copy(randomLevelOne = Vector.empty, randomLevelTwo = Vector.empty, fixed = cfg.overrides)))
                  .fold(errorChannel.raiseError, errorChannel.pure)
              else sequencer.pure(errorChannel.pure(MapEditorSettings(wraps, config)))
            else
              sequencer.pure(errorChannel.raiseError[MapEditorSettings](RuntimeException("Selection cancelled")))
        yield settings
    yield result
