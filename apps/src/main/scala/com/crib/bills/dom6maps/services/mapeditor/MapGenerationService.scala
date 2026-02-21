package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.effect.Async
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}
import model.map.{
  ColorComponent,
  DomVersion,
  FloatColor,
  ImageFile,
  MapDescription,
  MapDomColor,
  MapLayer,
  MapState,
  MapTextColor,
  MapTitle,
  NoDeepCaves,
  NoDeepChoice,
  ProvinceLocations,
  WinterImageFile
}
import model.map.generation.{GeometryGenerationInput, TerrainImageVariantPolicy}

final case class MapGenerationRequest(
    mapName: String,
    mapTitle: String,
    mapDescription: Option[String],
    geometryInput: GeometryGenerationInput,
    terrainImageVariantPolicy: TerrainImageVariantPolicy = TerrainImageVariantPolicy.BaseOnly
)

trait MapGenerationService[Sequencer[_]]:
  def generate[ErrorChannel[_]](
      request: MapGenerationRequest,
      outputDirectory: Path
  )(using
      files: Files[Sequencer],
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Path]]

class MapGenerationServiceImpl[Sequencer[_]: Async: Files](
    mapGeometryGenerator: MapGeometryGenerator[Sequencer],
    mapWriter: MapWriter[Sequencer],
    mapImageWriter: MapImageWriter[Sequencer],
    terrainImageVariantService: TerrainImageVariantService[Sequencer]
) extends MapGenerationService[Sequencer]:
  override def generate[ErrorChannel[_]](
      request: MapGenerationRequest,
      outputDirectory: Path
  )(using
      files: Files[Sequencer],
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Path]] =
    validateRequest(request) match
      case Left(error) =>
        summon[Async[Sequencer]].pure(errorChannel.raiseError[Path](error))
      case Right(_) =>
        val outputMapPath = outputDirectory / s"${request.mapName}.map"
        val outputImagePath = outputDirectory / s"${request.mapName}.tga"
        for
          generatedGeometryErrorChannel <- mapGeometryGenerator.generate[ErrorChannel](request.geometryInput)
          nestedResult <- generatedGeometryErrorChannel.traverse { generatedGeometry =>
            val generatedLayer = buildLayer(request, generatedGeometry)
            for
              mapWriteResult <- mapWriter.write[ErrorChannel](generatedLayer, outputMapPath)
              nestedImage <- mapWriteResult.traverse { _ =>
                for
                  imageWriteResult <- mapImageWriter.writeMainImage[ErrorChannel](generatedLayer, outputImagePath)
                  nestedVariants <- imageWriteResult.traverse { _ =>
                    terrainImageVariantService.writeVariants[ErrorChannel](
                      generatedLayer,
                      outputImagePath,
                      request.terrainImageVariantPolicy
                    ).map(_.as(outputMapPath))
                  }
                yield nestedVariants.flatMap(identity)
              }
            yield nestedImage.flatMap(identity)
          }
        yield nestedResult.flatMap(identity)

  private def validateRequest(request: MapGenerationRequest): Either[Throwable, Unit] =
    if request.mapName.trim.isEmpty then Left(IllegalArgumentException("mapName must not be empty"))
    else if request.mapTitle.trim.isEmpty then Left(IllegalArgumentException("mapTitle must not be empty"))
    else Right(())

  private def buildLayer(
      request: MapGenerationRequest,
      generatedGeometry: model.map.generation.GeneratedGeometry
  ): MapLayer[Sequencer] =
    val state = MapState.empty.copy(
      size = Some(request.geometryInput.mapSize),
      adjacency = generatedGeometry.adjacency,
      borders = generatedGeometry.borders,
      wrap = request.geometryInput.wrapState,
      title = Some(MapTitle(request.mapTitle)),
      description = request.mapDescription.map(MapDescription.apply),
      terrains = generatedGeometry.terrainByProvince,
      provinceLocations = ProvinceLocations.fromProvinceIdMap(generatedGeometry.provinceCentroids)
    )

    val winterImageDirectives =
      request.terrainImageVariantPolicy match
        case TerrainImageVariantPolicy.BaseOnly => Vector.empty
        case _ => Vector(WinterImageFile(s"${request.mapName}_winter.tga"))

    val passThrough =
      Vector(
        ImageFile(s"${request.mapName}.tga"),
        MapTextColor(
          FloatColor(
            ColorComponent(0.2),
            ColorComponent(0.0),
            ColorComponent(0.0),
            ColorComponent(1.0)
          )
        ),
        MapDomColor(255, 255, 30, 38),
        DomVersion(575),
        NoDeepCaves,
        NoDeepChoice
      ) ++
        winterImageDirectives ++
        generatedGeometry.provincePixelRuns

    MapLayer(state, Stream.emits(passThrough).covary[Sequencer])
