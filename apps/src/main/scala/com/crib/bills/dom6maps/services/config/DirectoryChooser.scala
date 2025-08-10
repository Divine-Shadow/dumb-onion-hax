package com.crib.bills.dom6maps
package apps.services.config

import cats.{MonadError, Traverse}
import cats.effect.Sync
import cats.syntax.all.*
import java.nio.file.Path as NioPath
import javax.swing.JFileChooser

trait DirectoryChooser[Sequencer[_]]:
  protected def directoryChooser: DirectoryChooser[Sequencer] = this

  def chooseDirectory[ErrorChannel[_]](
      title: String
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[NioPath]]

class DirectoryChooserImpl[Sequencer[_]](using Sync[Sequencer])
    extends DirectoryChooser[Sequencer]:

  override def chooseDirectory[ErrorChannel[_]](
      title: String
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[NioPath]] =
    for
      chooser <- Sync[Sequencer].delay(new JFileChooser())
      _ <- Sync[Sequencer].delay(chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY))
      _ <- Sync[Sequencer].delay(chooser.setDialogTitle(title))
      result <- Sync[Sequencer].delay(chooser.showOpenDialog(null))
      selected <-
        if result == JFileChooser.APPROVE_OPTION then
          Sync[Sequencer]
            .delay(chooser.getSelectedFile.toPath)
            .map(errorChannel.pure)
        else
          errorChannel
            .raiseError[NioPath](RuntimeException("Directory selection cancelled"))
            .pure[Sequencer]
    yield selected
