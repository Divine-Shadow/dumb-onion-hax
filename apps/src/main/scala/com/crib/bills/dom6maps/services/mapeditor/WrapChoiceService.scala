package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.effect.Sync
import cats.syntax.all.*
import javax.swing.{BorderFactory, Box, BoxLayout, JCheckBox, JComponent, JOptionPane, JPanel, JScrollPane, JTextArea}
import java.awt.{Component, Dimension, Toolkit}
import java.nio.file.{Files as JFiles, Path as NioPath}
import pureconfig.*
import model.map.ThroneFeatureConfig

trait WrapChoiceService[Sequencer[_]]:
  protected def wrapChoiceService: WrapChoiceService[Sequencer] = this

  def chooseSettings[ErrorChannel[_]](
      config: ThroneFeatureConfig,
      caveAvailability: CaveLayerAvailability
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[MapEditorSettings]]

class WrapChoiceServiceImpl[Sequencer[_]](using Sync[Sequencer])
    extends WrapChoiceService[Sequencer]:
  protected val sequencer = summon[Sync[Sequencer]]

  private def sectionPanel(title: String, components: Seq[JComponent]): JPanel =
    val wrapper = new JPanel()
    wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS))
    wrapper.setBorder(BorderFactory.createTitledBorder(title))
    components.zipWithIndex.foreach { case (component, idx) =>
      component.setAlignmentX(Component.LEFT_ALIGNMENT)
      wrapper.add(component)
      if idx < components.size - 1 then wrapper.add(Box.createVerticalStrut(4))
    }
    wrapper

  override def chooseSettings[ErrorChannel[_]](
      config: ThroneFeatureConfig,
      caveAvailability: CaveLayerAvailability
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
          throneSummaryArea <- sequencer.delay {
            val area = new JTextArea(throneSummary, 3, 30)
            area.setLineWrap(true)
            area.setWrapStyleWord(true)
            area.setOpaque(false)
            area.setEditable(false)
            area.setFocusable(false)
            area
          }
          throneBox <- sequencer.delay(new JCheckBox("Override Thrones"))
          magicSitesPanel <- sequencer.delay(new MagicSiteSelectionPanel(caveAvailability))
          panel <- sequencer.delay {
            def updateCave(): Unit =
              val duel = mainPanel.choice == WrapChoice.GroundSurfaceDuel
              caveBox.setEnabled(!duel)
              cavePanel.setEnabledAll(!duel && caveBox.isSelected)
            caveBox.addActionListener(_ => updateCave())
            mainPanel.onChange(updateCave())
            val p = new JPanel()
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS))
            val surfaceSection = sectionPanel("Surface wrap", Seq(mainPanel))
            val caveSection = sectionPanel("Cave layer options", Seq(caveBox, cavePanel))
            val throneSection = sectionPanel("Throne configuration", Seq(throneSummaryArea, throneBox))
            magicSitesPanel.setAlignmentX(Component.LEFT_ALIGNMENT)
            Seq(surfaceSection, caveSection, throneSection, magicSitesPanel).foreach { section =>
              p.add(section)
              p.add(Box.createVerticalStrut(12))
            }
            if p.getComponentCount > 0 then p.remove(p.getComponentCount - 1)
            updateCave()
            p
          }
          scrollPane <- sequencer.delay {
            val preferred = panel.getPreferredSize()
            val screen = Toolkit.getDefaultToolkit.getScreenSize
            val maxWidth = (screen.width * 0.75).toInt
            val maxHeight = (screen.height * 0.75).toInt
            val width = preferred.width.max(420).min(maxWidth)
            val height = preferred.height.max(320).min(maxHeight)
            val scroll = new JScrollPane(panel)
            scroll.setPreferredSize(new Dimension(width, height))
            scroll.setBorder(BorderFactory.createEmptyBorder())
            scroll.getVerticalScrollBar.setUnitIncrement(12)
            scroll
          }
          res <- sequencer.delay(
            JOptionPane.showConfirmDialog(
              null,
              scrollPane,
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
                    else
                      sequencer
                        .delay(
                          JFiles.writeString(
                            path,
                            """overrides = [
  { x = 0, y = 0, level = 3 },
  { x = 4, y = 2, level = 1 }
]
"""
                          )
                        )
                        .void
                  loaded <- sequencer.delay(ConfigSource.file(path).load[ThroneConfiguration])
                yield loaded
                  .leftMap(f => RuntimeException(f.toString))
                  .map(cfg =>
                    MapEditorSettings(
                      wraps,
                      config.copy(randomLevelOne = Vector.empty, randomLevelTwo = Vector.empty, fixed = cfg.overrides),
                      magicSitesPanel.selection
                    )
                  )
                  .fold(errorChannel.raiseError, errorChannel.pure)
              else
                // Do not apply any preloaded throne overrides unless explicitly enabled.
                // This ensures stale/out-of-bounds configs do not trigger validation when unchecked.
                sequencer.pure(errorChannel.pure(MapEditorSettings(wraps, config.copy(fixed = Vector.empty), magicSitesPanel.selection)))
            else
              sequencer.pure(errorChannel.raiseError[MapEditorSettings](RuntimeException("Selection cancelled")))
        yield settings
    yield result
