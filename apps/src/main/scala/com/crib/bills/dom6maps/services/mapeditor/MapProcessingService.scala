package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import cats.effect.Async
import model.map.{MapFileParser, MapState}

trait MapProcessingService[Sequencer[_]]:
  def process[ErrorChannel[_]](
      root: Path,
      dest: Path,
      transform: MapState => Sequencer[MapState]
  )(using errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Path]]

class MapProcessingServiceImpl[Sequencer[_]: Async: Files](
    finder: LatestEditorFinder[Sequencer],
    copier: MapEditorCopier[Sequencer],
    writer: MapWriter[Sequencer]
) extends MapProcessingService[Sequencer]:
  override def process[ErrorChannel[_]](
      root: Path,
      dest: Path,
      transform: MapState => Sequencer[MapState]
  )(using errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Path]] =
    for
      folderEC <- finder.mostRecentFolder[ErrorChannel](root)
      nested <- folderEC.traverse { folder =>
                  for
                    copyEC <- copier.copyWithoutMaps[ErrorChannel](folder, dest)
                    mapResult <- copyEC.traverse { streams =>
                                  val (mapBytes, outPath) = streams.main
                                  for
                                    state <- MapState.fromDirectives(mapBytes.through(MapFileParser.parse[Sequencer]))
                                    transformed <- transform(state)
                                    writtenEC <- writer.write[ErrorChannel](transformed, outPath)
                                  yield writtenEC.as(outPath)
                                }
                  yield mapResult
                }
    yield nested.flatMap(identity).flatMap(identity)
