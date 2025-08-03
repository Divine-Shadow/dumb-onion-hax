package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.syntax.all.*
import fs2.{Pipe, Stream}
import fs2.io.file.{Files, Path}
import cats.effect.Async
import model.map.{MapDirective, MapFileParser}

trait MapProcessingService[Sequencer[_]]:
  def process[ErrorChannel[_]](
      root: Path,
      dest: Path,
      pipe: Pipe[Sequencer, MapDirective, MapDirective]
  )(using errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Path]]

class MapProcessingServiceImpl[Sequencer[_]: Async: Files](
    finder: LatestEditorFinder[Sequencer],
    copier: MapEditorCopier[Sequencer],
    transformer: MapDirectiveTransformer[Sequencer],
    writer: MapWriter[Sequencer]
) extends MapProcessingService[Sequencer]:
  override def process[ErrorChannel[_]](
      root: Path,
      dest: Path,
      pipe: Pipe[Sequencer, MapDirective, MapDirective]
  )(using errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Path]] =
    for
      folderEC <- finder.mostRecentFolder[ErrorChannel](root)
      nested <- folderEC.traverse { folder =>
                  for
                    copyEC <- copier.copyWithoutMap[ErrorChannel](folder, dest)
                    mapResult <- copyEC.traverse { case (mapBytes, outPath) =>
                                  val directives = mapBytes.through(MapFileParser.parse[Sequencer])
                                  for
                                    transformedEC <- transformer.transform[ErrorChannel](directives, pipe)
                                    writtenEC <- transformedEC.traverse(ds => writer.write[ErrorChannel](ds, outPath))
                                  yield writtenEC.as(outPath)
                                }
                  yield mapResult
                }
    yield nested.flatMap(identity).flatMap(identity)
