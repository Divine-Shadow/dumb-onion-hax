package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.effect.Sync
import cats.syntax.all.*
import javax.swing.{BoxLayout, JCheckBox, JLabel, JOptionPane, JPanel}
import java.nio.file.{Files as JFiles, Path as NioPath}
import pureconfig.*
import model.map.ThroneFeatureConfig

trait ThroneFeatureView[Sequencer[_]]:
  protected def throneFeatureView: ThroneFeatureView[Sequencer] = this

  def chooseConfig[ErrorChannel[_]](
      config: ThroneFeatureConfig
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[ThroneFeatureConfig]]

class ThroneFeatureViewImpl[Sequencer[_]](using Sync[Sequencer])
    extends ThroneFeatureView[Sequencer]:
  private val configFileName = "throne-override.conf"
  private val sampleConfig =
    """overrides = [
  { province = 1, level = 3 },
  { province = 15, level = 1 }
]
"""
  private val sequencer = summon[Sync[Sequencer]]

  override def chooseConfig[ErrorChannel[_]](
      config: ThroneFeatureConfig
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[ThroneFeatureConfig]] =
    for
      panel <- sequencer.delay {
        val p = new JPanel()
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS))
        val summary =
          s"""Random L1: ${config.randomLevelOne.map(_.value).mkString(",")}
Random L2: ${config.randomLevelTwo.map(_.value).mkString(",")}
Fixed: ${config.fixed.map(p => s"${p.province.value}:${p.level.value}").mkString(",")}"""
        p.add(new JLabel(summary))
        p
      }
      box <- sequencer.delay(new JCheckBox("Override Thrones"))
      _ <- sequencer.delay(panel.add(box))
      res <- sequencer.delay(
        JOptionPane.showConfirmDialog(
          null,
          panel,
          "Throne Feature",
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE
        )
      )
      finalConfig <-
        if res == JOptionPane.OK_OPTION then
          if box.isSelected then
            val path = NioPath.of(configFileName)
            for
              exists <- sequencer.delay(JFiles.exists(path))
              _ <-
                if exists then sequencer.unit
                else sequencer.delay(JFiles.writeString(path, sampleConfig)).void
              loaded <- sequencer.delay(
                ConfigSource.file(path).load[ThroneConfiguration]
              )
            yield loaded
              .leftMap(f => RuntimeException(f.toString))
              .map(cfg => config.copy(randomLevelOne = Vector.empty, randomLevelTwo = Vector.empty, fixed = cfg.overrides))
              .fold(errorChannel.raiseError, errorChannel.pure)
          else
            sequencer.pure(errorChannel.pure(config))
        else
          sequencer.pure(errorChannel.raiseError[ThroneFeatureConfig](RuntimeException("Throne selection cancelled")))
    yield finalConfig
