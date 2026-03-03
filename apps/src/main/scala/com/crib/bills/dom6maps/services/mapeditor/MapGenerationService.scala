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
  Gate,
  ImageFile,
  MapDescription,
  MapDomColor,
  MapLayer,
  MapState,
  MapTextColor,
  MapTitle,
  NoDeepCaves,
  NoDeepChoice,
  PlaneName,
  ProvinceLocations,
  Terrain,
  WinterImageFile
}
import model.{BorderFlag, ProvinceId, TerrainFlag, TerrainMask}
import model.map.generation.{GeometryGenerationInput, TerrainImageVariantPolicy}
import model.map.generation.BorderSpecGenerationPolicy

enum UndergroundGenerationMode:
  case Disabled
  case MirroredPlane(
      planeName: String = "The Underworld",
      connectEveryProvinceWithTunnel: Boolean = true
    )

final case class MapGenerationRequest(
    mapName: String,
    mapTitle: String,
    mapDescription: Option[String],
    geometryInput: GeometryGenerationInput,
    borderSpecGenerationPolicy: BorderSpecGenerationPolicy = BorderSpecGenerationPolicy.default,
    terrainImageVariantPolicy: TerrainImageVariantPolicy = TerrainImageVariantPolicy.BaseOnly,
    undergroundGenerationMode: UndergroundGenerationMode = UndergroundGenerationMode.Disabled
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
    generatedBorderSpecService: GeneratedBorderSpecService,
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
        val outputUndergroundMapPath = outputDirectory / s"${request.mapName}_plane2.map"
        val outputUndergroundImagePath = outputDirectory / s"${request.mapName}_plane2.tga"
        for
          generatedGeometryErrorChannel <- mapGeometryGenerator.generate[ErrorChannel](request.geometryInput)
          nestedResult <- generatedGeometryErrorChannel.traverse { generatedGeometry =>
            val enrichedGeometry = generatedBorderSpecService.populateBorders(
              generatedGeometry,
              request.geometryInput.seed,
              request.borderSpecGenerationPolicy
            )
            val terrainBorderConsistentGeometry = enrichedGeometry.copy(
              terrainByProvince = enforceTerrainBorderConsistency(
                enrichedGeometry.terrainByProvince,
                enrichedGeometry.borders
              )
            )
            val mirroredUndergroundMode =
              request.undergroundGenerationMode match
                case mode: UndergroundGenerationMode.MirroredPlane => Some(mode)
                case UndergroundGenerationMode.Disabled => None
            val generatedLayer = buildLayer(request, terrainBorderConsistentGeometry, mirroredUndergroundMode)
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
                    ).map(_.as(()))
                  }
                  undergroundWrite <- nestedVariants.traverse { _ =>
                    request.undergroundGenerationMode match
                      case UndergroundGenerationMode.Disabled =>
                        summon[Async[Sequencer]].pure(errorChannel.pure(()))
                      case undergroundMode: UndergroundGenerationMode.MirroredPlane =>
                        val undergroundLayer =
                          buildUndergroundLayer(
                            request,
                            terrainBorderConsistentGeometry,
                            undergroundMode
                          )
                        for
                          undergroundMapWriteResult <- mapWriter.write[ErrorChannel](undergroundLayer, outputUndergroundMapPath)
                          undergroundImageWriteResult <- undergroundMapWriteResult.traverse { _ =>
                            mapImageWriter.writeMainImage[ErrorChannel](undergroundLayer, outputUndergroundImagePath)
                          }
                        yield undergroundImageWriteResult.flatMap(identity)
                  }
                yield undergroundWrite.flatMap(identity).as(outputMapPath)
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
      generatedGeometry: model.map.generation.GeneratedGeometry,
      mirroredUndergroundMode: Option[UndergroundGenerationMode.MirroredPlane]
  ): MapLayer[Sequencer] =
    val sanitizedSurfaceTerrains = generatedGeometry.terrainByProvince.map { terrain =>
      terrain.copy(mask = sanitizeSurfaceTerrainMask(terrain.mask))
    }
    val provinceIds = collectProvinceIds(generatedGeometry)
    val surfaceGates =
      mirroredUndergroundMode
        .filter(_.connectEveryProvinceWithTunnel)
        .map(_ => provinceIds.map(provinceId => Gate(provinceId, provinceId)))
        .getOrElse(Vector.empty)

    val state = MapState.empty.copy(
      size = Some(request.geometryInput.mapSize),
      adjacency = generatedGeometry.adjacency,
      borders = generatedGeometry.borders,
      gates = surfaceGates,
      wrap = request.geometryInput.wrapState,
      title = Some(MapTitle(request.mapTitle)),
      description = request.mapDescription.map(MapDescription.apply),
      terrains = sanitizedSurfaceTerrains,
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

  private def buildUndergroundLayer(
      request: MapGenerationRequest,
      generatedGeometry: model.map.generation.GeneratedGeometry,
      undergroundMode: UndergroundGenerationMode.MirroredPlane
  ): MapLayer[Sequencer] =
    val undergroundTerrains =
      enforceTerrainBorderConsistency(
        generatedGeometry.terrainByProvince.map { terrain =>
          terrain.copy(mask = toUndergroundTerrainMask(terrain.mask))
        },
        borders = Vector.empty
      )
    val provinceIds = undergroundTerrains.map(_.province).distinct.sortBy(_.value)

    val undergroundGates =
      if undergroundMode.connectEveryProvinceWithTunnel then
        provinceIds.map(provinceId => Gate(provinceId, provinceId))
      else Vector.empty

    val state = MapState.empty.copy(
      size = Some(request.geometryInput.mapSize),
      adjacency = generatedGeometry.adjacency,
      borders = Vector.empty,
      gates = undergroundGates,
      wrap = request.geometryInput.wrapState,
      title = None,
      description = request.mapDescription.map(MapDescription.apply),
      terrains = undergroundTerrains,
      provinceLocations = ProvinceLocations.fromProvinceIdMap(generatedGeometry.provinceCentroids)
    )

    val passThrough =
      Vector(
        ImageFile(s"${request.mapName}_plane2.tga"),
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
        NoDeepChoice,
        PlaneName(undergroundMode.planeName)
      ) ++ generatedGeometry.provincePixelRuns

    MapLayer(state, Stream.emits(passThrough).covary[Sequencer])

  private def sanitizeSurfaceTerrainMask(maskValue: Long): Long =
    TerrainMask(maskValue)
      .withoutFlag(TerrainFlag.Cave)
      .withoutFlag(TerrainFlag.CaveWall)
      .value

  private def enforceTerrainBorderConsistency(
      terrains: Vector[Terrain],
      borders: Vector[model.map.Border]
  ): Vector[Terrain] =
    val borderFlagsByProvince =
      borders.foldLeft(Map.empty[ProvinceId, Vector[BorderFlag]]) { (accumulator, border) =>
        val firstFlags = accumulator.getOrElse(border.a, Vector.empty) :+ border.flag
        val secondFlags = accumulator.getOrElse(border.b, Vector.empty) :+ border.flag
        accumulator.updated(border.a, firstFlags).updated(border.b, secondFlags)
      }

    terrains.map { terrain =>
      val hasRiverBorder = borderFlagsByProvince
        .getOrElse(terrain.province, Vector.empty)
        .exists(flag => flag.includes(BorderFlag.River))
      val hasImpassableMountainBorder = borderFlagsByProvince
        .getOrElse(terrain.province, Vector.empty)
        .exists(flag => flag.includes(BorderFlag.Impassable))

      val baseMask = TerrainMask(terrain.mask)
        .withoutFlag(TerrainFlag.FreshWater)
        .withoutFlag(TerrainFlag.Mountains)

      val withRiverTerrain =
        if hasRiverBorder then baseMask.withFlag(TerrainFlag.FreshWater)
        else baseMask
      val withMountainTerrain =
        if hasImpassableMountainBorder then withRiverTerrain.withFlag(TerrainFlag.Mountains)
        else withRiverTerrain

      terrain.copy(mask = withMountainTerrain.value)
    }

  private def toUndergroundTerrainMask(surfaceMaskValue: Long): Long =
    val normalizedSurfaceMask = TerrainMask(sanitizeSurfaceTerrainMask(surfaceMaskValue))
    val undergroundBaseMask =
      if normalizedSurfaceMask.hasFlag(TerrainFlag.Sea) || normalizedSurfaceMask.hasFlag(TerrainFlag.DeepSea) then
        TerrainFlag.FreshWater.mask
      else if normalizedSurfaceMask.hasFlag(TerrainFlag.Forest) then TerrainFlag.Forest.mask
      else if normalizedSurfaceMask.hasFlag(TerrainFlag.Swamp) then TerrainFlag.Swamp.mask
      else if normalizedSurfaceMask.hasFlag(TerrainFlag.Waste) then TerrainFlag.Waste.mask
      else if normalizedSurfaceMask.hasFlag(TerrainFlag.Highlands) then TerrainFlag.Highlands.mask
      else if normalizedSurfaceMask.hasFlag(TerrainFlag.Farm) then TerrainFlag.Farm.mask
      else if normalizedSurfaceMask.hasFlag(TerrainFlag.Mountains) then TerrainFlag.Mountains.mask
      else TerrainFlag.Plains.mask

    TerrainMask(undergroundBaseMask)
      .withFlag(TerrainFlag.Cave)
      .value

  private def collectProvinceIds(
      generatedGeometry: model.map.generation.GeneratedGeometry
  ): Vector[ProvinceId] =
    generatedGeometry.terrainByProvince
      .map(_.province)
      .distinct
      .sortBy(_.value)
