package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.effect.Async
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}
import model.map.{
  Commander,
  ColorComponent,
  DomVersion,
  FeatureId,
  FloatColor,
  Gate,
  ImageFile,
  MapSize,
  MapDescription,
  MapDomColor,
  MapLayer,
  MapState,
  MapTextColor,
  Pb,
  MapTitle,
  NoDeepCaves,
  NoDeepChoice,
  PlaneName,
  ProvinceLocations,
  SetLand,
  Terrain,
  ThroneLevel,
  ThronePlacement,
  Units,
  WinterImageFile
}
import model.{BorderFlag, ProvinceId, TerrainFlag, TerrainMask}
import model.map.generation.{GeometryGenerationInput, TerrainImageVariantPolicy}
import model.map.generation.BorderSpecGenerationPolicy
import model.dominions.{Feature as DomFeature}

enum UndergroundGenerationMode:
  case Disabled
  case MirroredPlane(
      planeName: String = "The Underworld",
      connectEveryProvinceWithTunnel: Boolean = true
    )

enum ThroneGenerationMode:
  case Disabled
  case RandomCorners(
      throneLevel: ThroneLevel = ThroneLevel(1),
      includeSurface: Boolean = true,
      includeUnderground: Boolean = true
    )
  case Configured(
      surfaceThrones: Vector[ThronePlacement],
      undergroundThrones: Vector[ThronePlacement]
    )

final case class ThroneDefenderUnit(
    count: Int,
    unitType: String
)

final case class ThroneDefenderSetPiece(
    throneLevel: ThroneLevel,
    commanderType: String,
    units: Vector[ThroneDefenderUnit]
)

final case class MapGenerationRequest(
    mapName: String,
    mapTitle: String,
    mapDescription: Option[String],
    geometryInput: GeometryGenerationInput,
    borderSpecGenerationPolicy: BorderSpecGenerationPolicy = BorderSpecGenerationPolicy.default,
    terrainImageVariantPolicy: TerrainImageVariantPolicy = TerrainImageVariantPolicy.BaseOnly,
    undergroundGenerationMode: UndergroundGenerationMode = UndergroundGenerationMode.Disabled,
    throneGenerationMode: ThroneGenerationMode = ThroneGenerationMode.Disabled,
    throneDefenderSetPieces: Vector[ThroneDefenderSetPiece] = Vector.empty
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
    terrainImageVariantService: TerrainImageVariantService[Sequencer],
    thronePlacementService: ThronePlacementService[Sequencer]
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
            val surfaceThrones =
              resolveSurfaceThronePlacements(
                request,
                terrainBorderConsistentGeometry,
                generatedLayer.state
              )
            for
              surfacedStateResult <- applyThrones[ErrorChannel](request.throneGenerationMode, generatedLayer.state, surfaceThrones)
              nestedImage <- surfacedStateResult.traverse { surfacedState =>
                val surfaceDefenderSetPieceDirectives = buildDefenderSetPieceDirectives(
                  request.throneGenerationMode,
                  surfaceThrones,
                  surfacedState,
                  request.throneDefenderSetPieces
                )
                val surfacedLayer = appendPassThroughDirectives(
                  generatedLayer.copy(state = surfacedState),
                  surfaceDefenderSetPieceDirectives
                )
                for
                  mapWriteResult <- mapWriter.write[ErrorChannel](surfacedLayer, outputMapPath)
                  nestedWrite <- mapWriteResult.traverse { _ =>
                    for
                      imageWriteResult <- mapImageWriter.writeMainImage[ErrorChannel](surfacedLayer, outputImagePath)
                      nestedVariants <- imageWriteResult.traverse { _ =>
                        terrainImageVariantService.writeVariants[ErrorChannel](
                          surfacedLayer,
                          outputImagePath,
                          request.terrainImageVariantPolicy
                        ).map(_.as(()))
                      }
                      undergroundWrite <- nestedVariants.traverse { _ =>
                        request.undergroundGenerationMode match
                          case UndergroundGenerationMode.Disabled =>
                            summon[Async[Sequencer]].pure(errorChannel.pure(()))
                          case undergroundMode: UndergroundGenerationMode.MirroredPlane =>
                            val undergroundLayerBase =
                              buildUndergroundLayer(
                                request,
                                terrainBorderConsistentGeometry,
                                undergroundMode
                              )
                            val undergroundThrones =
                              resolveUndergroundThronePlacements(
                                request,
                                terrainBorderConsistentGeometry,
                                undergroundLayerBase.state
                              )
                            for
                              undergroundStateResult <- applyThrones[ErrorChannel](request.throneGenerationMode, undergroundLayerBase.state, undergroundThrones)
                              undergroundWriteResult <- undergroundStateResult.traverse { undergroundState =>
                                val undergroundDefenderSetPieceDirectives = buildDefenderSetPieceDirectives(
                                  request.throneGenerationMode,
                                  undergroundThrones,
                                  undergroundState,
                                  request.throneDefenderSetPieces
                                )
                                val undergroundLayer = appendPassThroughDirectives(
                                  undergroundLayerBase.copy(state = undergroundState),
                                  undergroundDefenderSetPieceDirectives
                                )
                                for
                                  undergroundMapWriteResult <- mapWriter.write[ErrorChannel](undergroundLayer, outputUndergroundMapPath)
                                  undergroundImageWriteResult <- undergroundMapWriteResult.traverse { _ =>
                                    mapImageWriter.writeMainImage[ErrorChannel](undergroundLayer, outputUndergroundImagePath)
                                  }
                                yield undergroundImageWriteResult.flatMap(identity)
                              }
                            yield undergroundWriteResult.flatMap(identity)
                      }
                    yield undergroundWrite.flatMap(identity)
                  }
                yield nestedWrite.flatMap(identity).as(outputMapPath)
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

  private def applyThrones[ErrorChannel[_]](
      throneGenerationMode: ThroneGenerationMode,
      state: MapState,
      thronePlacements: Vector[ThronePlacement]
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[MapState]] =
    if thronePlacements.isEmpty then summon[Async[Sequencer]].pure(errorChannel.pure(state))
    else
      throneGenerationMode match
        case ThroneGenerationMode.RandomCorners(_, _, _) =>
          summon[Async[Sequencer]].pure(errorChannel.pure(applyThroneTerrainFlagsOnly(state, thronePlacements)))
        case _ =>
          thronePlacementService.update[ErrorChannel](state, thronePlacements)

  private def applyThroneTerrainFlagsOnly(
      state: MapState,
      thronePlacements: Vector[ThronePlacement]
  ): MapState =
    val throneProvinces = thronePlacements.flatMap(tp => state.provinceLocations.provinceIdAt(tp.location)).toSet
    val updatedTerrains = state.terrains.map {
      case terrain @ Terrain(province, mask) =>
        val updatedMask =
          if throneProvinces.contains(province) then TerrainMask(mask).withFlag(TerrainFlag.GoodThrone)
          else TerrainMask(mask).withoutFlag(TerrainFlag.GoodThrone)
        terrain.copy(mask = updatedMask.value)
    }
    state.copy(terrains = updatedTerrains)

  private def appendPassThroughDirectives(
      layer: MapLayer[Sequencer],
      directives: Vector[model.map.MapDirective]
  ): MapLayer[Sequencer] =
    if directives.isEmpty then layer
    else layer.copy(passThrough = layer.passThrough ++ Stream.emits(directives).covary[Sequencer])

  private def buildDefenderSetPieceDirectives(
      throneGenerationMode: ThroneGenerationMode,
      thronePlacements: Vector[ThronePlacement],
      state: MapState,
      throneDefenderSetPieces: Vector[ThroneDefenderSetPiece]
  ): Vector[model.map.MapDirective] =
    throneGenerationMode match
      case ThroneGenerationMode.Configured(_, _) =>
        val setPieceByLevel = throneDefenderSetPieces.map(setPiece => setPiece.throneLevel.value -> setPiece).toMap
        thronePlacements.flatMap { placement =>
          val maybeProvince = state.provinceLocations.provinceIdAt(placement.location)
          val maybeLevel =
            placement.level.map(_.value)
              .orElse(placement.id.flatMap(resolveThroneLevelForFeatureId).map(_.value))
          (maybeProvince, maybeLevel) match
            case (Some(province), Some(level)) =>
              setPieceByLevel.get(level).map { setPiece =>
                Vector(SetLand(province), Commander(setPiece.commanderType)) ++
                  setPiece.units.map(unit => Units(unit.count, unit.unitType))
              }.getOrElse(Vector.empty)
            case _ => Vector.empty
        }
      case _ =>
        Vector.empty

  private def resolveThroneLevelForFeatureId(featureId: FeatureId): Option[ThroneLevel] =
    if DomFeature.levelOneThrones.exists(_.id.value == featureId.value) then Some(ThroneLevel(1))
    else if DomFeature.levelTwoThrones.exists(_.id.value == featureId.value) then Some(ThroneLevel(2))
    else if DomFeature.levelThreeThrones.exists(_.id.value == featureId.value) then Some(ThroneLevel(3))
    else None

  private def resolveSurfaceThronePlacements(
      request: MapGenerationRequest,
      generatedGeometry: model.map.generation.GeneratedGeometry,
      state: MapState
  ): Vector[ThronePlacement] =
    request.throneGenerationMode match
      case ThroneGenerationMode.Disabled => Vector.empty
      case ThroneGenerationMode.Configured(surfaceThrones, _) => surfaceThrones
      case ThroneGenerationMode.RandomCorners(throneLevel, includeSurface, _) =>
        if includeSurface then randomCornerThronePlacements(request.geometryInput.mapSize, generatedGeometry.provincePixelRuns, state, throneLevel)
        else Vector.empty

  private def resolveUndergroundThronePlacements(
      request: MapGenerationRequest,
      generatedGeometry: model.map.generation.GeneratedGeometry,
      state: MapState
  ): Vector[ThronePlacement] =
    request.throneGenerationMode match
      case ThroneGenerationMode.Disabled => Vector.empty
      case ThroneGenerationMode.Configured(_, undergroundThrones) => undergroundThrones
      case ThroneGenerationMode.RandomCorners(throneLevel, _, includeUnderground) =>
        if includeUnderground then randomCornerThronePlacements(request.geometryInput.mapSize, generatedGeometry.provincePixelRuns, state, throneLevel)
        else Vector.empty

  private def randomCornerThronePlacements(
      mapSize: MapSize,
      provinceRuns: Vector[Pb],
      state: MapState,
      throneLevel: ThroneLevel
  ): Vector[ThronePlacement] =
    val widthPixels = mapSize.value * 256
    val heightPixels = mapSize.value * 160
    val cornerPixelCoordinates = Vector(
      (0, 0),
      (widthPixels - 1, 0),
      (0, heightPixels - 1),
      (widthPixels - 1, heightPixels - 1)
    )
    val provinceIdsByCorner =
      cornerPixelCoordinates.map { case (xPixel, yPixel) =>
        rankProvincesByDistanceToPixel(provinceRuns, xPixel, yPixel)
      }

    val selectedProvinceIds = provinceIdsByCorner.foldLeft(Vector.empty[ProvinceId]) { (accumulator, rankedProvinceIds) =>
      rankedProvinceIds.find(provinceId => !accumulator.contains(provinceId)) match
        case Some(provinceId) => accumulator :+ provinceId
        case None =>
          rankedProvinceIds.headOption match
            case Some(provinceId) if !accumulator.contains(provinceId) => accumulator :+ provinceId
            case _ => accumulator
    }

    selectedProvinceIds.flatMap { provinceId =>
      state.provinceLocations.locationOf(provinceId).map(location => ThronePlacement(location, throneLevel))
    }

  private def rankProvincesByDistanceToPixel(
      provinceRuns: Vector[Pb],
      xPixel: Int,
      yPixel: Int
  ): Vector[ProvinceId] =
    val minimumDistanceByProvince = provinceRuns.foldLeft(Map.empty[ProvinceId, Int]) { (accumulator, run) =>
      val runStart = run.x
      val runEnd = run.x + run.length - 1
      val deltaX =
        if xPixel < runStart then runStart - xPixel
        else if xPixel > runEnd then xPixel - runEnd
        else 0
      val deltaY = math.abs(yPixel - run.y)
      val distance = deltaX + deltaY
      val current = accumulator.getOrElse(run.province, Int.MaxValue)
      if distance < current then accumulator.updated(run.province, distance)
      else accumulator
    }
    minimumDistanceByProvince.toVector
      .sortBy { case (provinceId, distance) => (distance, provinceId.value) }
      .map(_._1)
