package com.crib.bills.dom6maps
package apps.services.config

import cats.{MonadError, Traverse}
import cats.effect.Async
import cats.syntax.all.*
import java.nio.file.{Files as JFiles, Path as NioPath}

trait DirectoryConfigService[Sequencer[_]]:
  protected def directoryConfigService: DirectoryConfigService[Sequencer] = this

  def selectAndStore[ErrorChannel[_]]()(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Unit]]

class DirectoryConfigServiceImpl[Sequencer[_]: Async](
    chooser: DirectoryChooser[Sequencer]
) extends DirectoryConfigService[Sequencer]:
  private val configFileName = "map-editor-wrap.conf"

  override def selectAndStore[ErrorChannel[_]]()(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Unit]] =
    for
      sourceEC <- chooser.chooseDirectory[ErrorChannel]("Select Map Editor export directory")
      destEC <- chooser.chooseDirectory[ErrorChannel]("Select Dominions map input directory")
      written <- (sourceEC, destEC).tupled.traverse { case (source, dest) =>
        val configPath = NioPath.of(configFileName)
        val contents =
          s"""source="${source.toString}"
dest="${dest.toString}"
"""
        (for
          exists <- Async[Sequencer].delay(JFiles.exists(configPath))
          _ <-
            if exists then Async[Sequencer].unit
            else Async[Sequencer].delay(JFiles.createFile(configPath)).void
          _ <- Async[Sequencer].delay(JFiles.writeString(configPath, contents))
        yield ()).attempt.map(
          _.fold(
            e => errorChannel.raiseError[Unit](e),
            _ => errorChannel.pure(())
          )
        )
      }
    yield errorChannel.flatMap(written)(x => x)
