package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.effect.Async
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}
import model.map.{
  AllowedPlayer,
  Commander,
  ColorComponent,
  DomVersion,
  FeatureId,
  FloatColor,
  Gate,
  ImageFile,
  Land,
  MapDescription,
  MapDomColor,
  MapLayer,
  MapState,
  MapTextColor,
  MapTitle,
  NoDeepCaves,
  NoDeepChoice,
  Pb,
  PlaneName,
  ProvinceLocation,
  ProvinceLocations,
  SpecStart,
  Terrain,
  ThroneLevel,
  ThronePlacement,
  Units,
  WinterImageFile,
  XCell,
  YCell
}
import model.map.generation.{
  AllocationGenerationPolicy,
  AllocationLayer,
  BorderSpecGenerationPolicy,
  DistanceTiePolicy,
  GeometryGenerationInput,
  PlayerStartAssignment,
  PlayerStartLocationAssignment,
  ProfileEnvironment,
  RegionOwner,
  TerrainImageVariantPolicy
}
import model.{BorderFlag, Nation, ProvinceId, TerrainFlag, TerrainMask}
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
      surfaceThrones: Vector[ConfiguredThronePlacementTarget],
      undergroundThrones: Vector[ConfiguredThronePlacementTarget]
    )

final case class ConfiguredThronePlacementTarget(
    provinceId: Option[ProvinceId],
    location: Option[ProvinceLocation],
    throneLevel: Option[ThroneLevel],
    throneFeatureId: Option[FeatureId]
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
    throneDefenderSetPieces: Vector[ThroneDefenderSetPiece] = Vector.empty,
    playerStarts: Vector[PlayerStartAssignment] = Vector.empty,
    playerStartLocations: Vector[PlayerStartLocationAssignment] = Vector.empty,
    allocationGenerationPolicy: AllocationGenerationPolicy = AllocationGenerationPolicy.Disabled
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
    thronePlacementService: ThronePlacementService[Sequencer],
    mapGenerationDiagnosticsWriter: MapGenerationDiagnosticsWriter[Sequencer],
    allocationPartitionService: AllocationPartitionService = new AllocationPartitionServiceImpl,
    allottedProvinceService: AllottedProvinceService = new AllottedProvinceServiceImpl,
    allocationTerrainService: AllocationTerrainService = new AllocationTerrainServiceImpl
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
        val outputBundleDirectory = resolveOutputBundleDirectory(outputDirectory, request.mapName)
        val outputMapPath = outputBundleDirectory / s"${request.mapName}.map"
        val outputImagePath = outputBundleDirectory / s"${request.mapName}.tga"
        val outputUndergroundMapPath = outputBundleDirectory / s"${request.mapName}_plane2.map"
        val outputUndergroundImagePath = outputBundleDirectory / s"${request.mapName}_plane2.tga"
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
            val provinceIdByCell = buildProvinceIdByCell(
              terrainBorderConsistentGeometry.provincePixelRuns,
              request.geometryInput.mapDimensions
            )
            val terrainMaskByProvince = terrainBorderConsistentGeometry.terrainByProvince
              .map(terrain => terrain.province -> terrain.mask)
              .toMap
            resolveCombinedPlayerStarts(request, generatedGeometry.provinceCentroids, provinceIdByCell, terrainMaskByProvince) match
              case Left(error) =>
                summon[Async[Sequencer]].pure(errorChannel.raiseError(error))
              case Right(playerStartsResolved) =>
                val mirroredUndergroundMode =
                  request.undergroundGenerationMode match
                    case mode: UndergroundGenerationMode.MirroredPlane => Some(mode)
                    case UndergroundGenerationMode.Disabled => None
                val generatedLayer = buildLayer(request, terrainBorderConsistentGeometry, mirroredUndergroundMode, playerStartsResolved)

                for
                  surfaceAllocatedStateResult <- applyAllocationForLayer[ErrorChannel](
                    request,
                    generatedLayer.state,
                    AllocationLayer.Surface,
                    playerStartsResolved
                  )
                  surfacedAfterAllocation <- surfaceAllocatedStateResult.traverse { surfaceAllocatedState =>
                    val surfaceThrones =
                      resolveSurfaceThronePlacements(
                        request,
                        surfaceAllocatedState,
                        provinceIdByCell
                      )
                    val surfaceRandomCornerProvinceIds =
                      resolveSurfaceRandomCornerProvinceIds(
                        request,
                        terrainBorderConsistentGeometry.provincePixelRuns
                      )
                    applyThrones[ErrorChannel](
                      request.throneGenerationMode,
                      surfaceAllocatedState,
                      surfaceThrones,
                      surfaceRandomCornerProvinceIds
                    )
                  }
                  nestedImage <- surfacedAfterAllocation.flatMap(identity).flatTraverse { surfacedState =>
                val surfaceThrones = resolveSurfaceThronePlacements(request, surfacedState, provinceIdByCell)
                val surfaceRandomCornerProvinceIds =
                  resolveSurfaceRandomCornerProvinceIds(
                    request,
                    terrainBorderConsistentGeometry.provincePixelRuns
                  )
                val surfaceDefenderSetPieceDirectives = buildDefenderSetPieceDirectives(
                  request.throneGenerationMode,
                  surfaceThrones,
                  surfaceRandomCornerProvinceIds,
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
                      diagnosticsWriteResult <- imageWriteResult.traverse { _ =>
                        mapGenerationDiagnosticsWriter.write[ErrorChannel](
                          surfacedLayer,
                          request.mapName,
                          outputBundleDirectory
                        )
                      }
                      nestedVariants <- diagnosticsWriteResult.flatMap(identity).traverse { _ =>
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
                                undergroundMode,
                                playerStartsResolved
                              )
                            for
                              undergroundAllocatedStateResult <- applyAllocationForLayer[ErrorChannel](
                                request,
                                undergroundLayerBase.state,
                                AllocationLayer.Underground,
                                playerStartsResolved
                              )
                              undergroundAfterAllocation <- undergroundAllocatedStateResult.traverse { undergroundAllocatedState =>
                                val undergroundThrones =
                                  resolveUndergroundThronePlacements(
                                    request,
                                    undergroundAllocatedState,
                                    provinceIdByCell
                                  )
                                val undergroundRandomCornerProvinceIds =
                                  resolveUndergroundRandomCornerProvinceIds(
                                    request,
                                    terrainBorderConsistentGeometry.provincePixelRuns
                                  )
                                applyThrones[ErrorChannel](
                                  request.throneGenerationMode,
                                  undergroundAllocatedState,
                                  undergroundThrones,
                                  undergroundRandomCornerProvinceIds
                                )
                              }
                              undergroundWriteResult <- undergroundAfterAllocation.flatMap(identity).traverse { undergroundState =>
                                val undergroundThrones =
                                  resolveUndergroundThronePlacements(
                                    request,
                                    undergroundState,
                                    provinceIdByCell
                                  )
                                val undergroundRandomCornerProvinceIds =
                                  resolveUndergroundRandomCornerProvinceIds(
                                    request,
                                    terrainBorderConsistentGeometry.provincePixelRuns
                                  )
                                val undergroundDefenderSetPieceDirectives = buildDefenderSetPieceDirectives(
                                  request.throneGenerationMode,
                                  undergroundThrones,
                                  undergroundRandomCornerProvinceIds,
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
                yield nestedImage
          }
        yield nestedResult.flatMap(identity)

  private def resolveOutputBundleDirectory(
      configuredOutputDirectory: Path,
      mapName: String
  ): Path =
    val configuredName = Option(configuredOutputDirectory.toNioPath.getFileName)
      .map(_.toString)
      .getOrElse("")
    if configuredName == mapName then configuredOutputDirectory
    else configuredOutputDirectory / mapName

  private def validateRequest(request: MapGenerationRequest): Either[Throwable, Unit] =
    if request.mapName.trim.isEmpty then Left(IllegalArgumentException("mapName must not be empty"))
    else if request.mapTitle.trim.isEmpty then Left(IllegalArgumentException("mapTitle must not be empty"))
    else if request.playerStarts.exists(start => start.surfaceStart.isEmpty && start.undergroundStart.isEmpty) then
      Left(IllegalArgumentException("Each player start assignment must define at least one layer start"))
    else if request.playerStartLocations.exists(start => start.surfaceStart.isEmpty && start.undergroundStart.isEmpty) then
      Left(IllegalArgumentException("Each player start location assignment must define at least one layer start"))
    else
      Right(())

  private def buildLayer(
      request: MapGenerationRequest,
      generatedGeometry: model.map.generation.GeneratedGeometry,
      mirroredUndergroundMode: Option[UndergroundGenerationMode.MirroredPlane],
      playerStarts: Vector[PlayerStartAssignment]
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

    val allPlayers = playerStarts.map(_.nation).distinct.sortBy(_.id).map(AllowedPlayer.apply)
    val surfaceStarts = playerStarts.flatMap { start =>
      start.surfaceStart.map(provinceId => SpecStart(start.nation, provinceId))
    }

    val state = MapState.empty.copy(
      size = Some(request.geometryInput.mapDimensions),
      adjacency = generatedGeometry.adjacency,
      borders = generatedGeometry.borders,
      gates = surfaceGates,
      wrap = request.geometryInput.wrapState,
      title = Some(MapTitle(request.mapTitle)),
      description = request.mapDescription.map(MapDescription.apply),
      allowedPlayers = allPlayers,
      startingPositions = surfaceStarts,
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
      undergroundMode: UndergroundGenerationMode.MirroredPlane,
      playerStarts: Vector[PlayerStartAssignment]
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

    val allPlayers = playerStarts.map(_.nation).distinct.sortBy(_.id).map(AllowedPlayer.apply)
    val undergroundStarts = playerStarts.flatMap { start =>
      start.undergroundStart.map(provinceId => SpecStart(start.nation, provinceId))
    }

    val state = MapState.empty.copy(
      size = Some(request.geometryInput.mapDimensions),
      adjacency = generatedGeometry.adjacency,
      borders = Vector.empty,
      gates = undergroundGates,
      wrap = request.geometryInput.wrapState,
      title = None,
      description = request.mapDescription.map(MapDescription.apply),
      allowedPlayers = allPlayers,
      startingPositions = undergroundStarts,
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

  private def applyAllocationForLayer[ErrorChannel[_]](
      request: MapGenerationRequest,
      state: MapState,
      layer: AllocationLayer,
      playerStarts: Vector[PlayerStartAssignment]
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[MapState]] =
    request.allocationGenerationPolicy match
      case AllocationGenerationPolicy.Disabled =>
        summon[Async[Sequencer]].pure(errorChannel.pure(state))
      case enabledPolicy: AllocationGenerationPolicy.Enabled =>
        val startsByNation = collectStartsForLayer(playerStarts, layer)
        if startsByNation.isEmpty then
          summon[Async[Sequencer]].pure(errorChannel.pure(state))
        else
          val provinceIds = state.terrains.map(_.province).distinct.sortBy(_.value)
          val resolved = for
            partition <- allocationPartitionService.partition(
              adjacency = state.adjacency,
              allProvinceIds = provinceIds,
              startsByNation = startsByNation,
              tiePolicy = enabledPolicy.tiePolicy
            )
            playerAllotments <- startsByNation.toVector.sortBy { case (nation, _) => nation.id }.traverse { case (nation, startProvince) =>
              for
                profile <- enabledPolicy.profileCatalog
                  .resolve(nation, layer)
                  .toRight(
                    IllegalArgumentException(
                      s"Missing allocation profile for nation ${nation.id} on layer ${layer.toString} (exact or any environment)"
                    )
                  )
                allotted <- allottedProvinceService.resolve(
                  nation = nation,
                  startProvince = startProvince,
                  capRingSize = profile.capRingSize,
                  adjacency = state.adjacency,
                  partition = partition
                )
              yield allotted -> profile
            }
          yield
            val neutralProvinceIds = partition.ownerByProvince.collect {
              case (provinceId, RegionOwner.Neutral) => provinceId
            }.toVector.sortBy(_.value)
            val updatedTerrains = allocationTerrainService.applyAllocation(
              terrains = state.terrains,
              playerAllotments = playerAllotments,
              neutralProvinceIds = neutralProvinceIds,
              neutralProfile = enabledPolicy.neutralProfile,
              defaultTerrainDistributionPolicy = request.geometryInput.terrainDistributionPolicy,
              layer = layer,
              seed = request.geometryInput.seed + enabledPolicy.seedSalt
            )
            state.copy(terrains = updatedTerrains)

          summon[Async[Sequencer]].pure(
            resolved match
              case Left(error) => errorChannel.raiseError(error)
              case Right(value) => errorChannel.pure(value)
          )

  private def collectStartsForLayer(
      playerStarts: Vector[PlayerStartAssignment],
      layer: AllocationLayer
  ): Map[Nation, ProvinceId] =
    layer match
      case AllocationLayer.Surface =>
        playerStarts.flatMap(start => start.surfaceStart.map(start.nation -> _)).toMap
      case AllocationLayer.Underground =>
        playerStarts.flatMap(start => start.undergroundStart.map(start.nation -> _)).toMap

  private def resolveCombinedPlayerStarts(
      request: MapGenerationRequest,
      provinceCentroids: Map[ProvinceId, ProvinceLocation],
      provinceIdByCell: Map[ProvinceLocation, ProvinceId],
      terrainMaskByProvince: Map[ProvinceId, Long]
  ): Either[Throwable, Vector[PlayerStartAssignment]] =
    val resolvedFromLocations = request.playerStartLocations.traverse { start =>
      for
        resolvedSurface <- resolveStartProvinceByLocation(
          start.nation,
          start.surfaceStart,
          provinceIdByCell,
          provinceCentroids,
          terrainMaskByProvince,
          s"surface start for nation ${start.nation.id}"
        )
        resolvedUnderground <- resolveStartProvinceByLocation(
          start.nation,
          start.undergroundStart,
          provinceIdByCell,
          provinceCentroids,
          terrainMaskByProvince,
          s"underground start for nation ${start.nation.id}"
        )
      yield PlayerStartAssignment(
        nation = start.nation,
        surfaceStart = resolvedSurface,
        undergroundStart = resolvedUnderground
      )
    }

    resolvedFromLocations.flatMap { resolved =>
      val merged = request.playerStarts ++ resolved
      val duplicateNation = merged.groupBy(_.nation).collectFirst { case (nation, values) if values.size > 1 => nation }
      duplicateNation match
        case Some(nation) => Left(IllegalArgumentException(s"Duplicate player start assignment for nation: ${nation.id}"))
        case None => Right(merged)
    }

  private def resolveStartProvinceByLocation(
      nation: Nation,
      location: Option[ProvinceLocation],
      provinceIdByCell: Map[ProvinceLocation, ProvinceId],
      provinceCentroids: Map[ProvinceId, ProvinceLocation],
      terrainMaskByProvince: Map[ProvinceId, Long],
      label: String
  ): Either[Throwable, Option[ProvinceId]] =
    location match
      case None => Right(None)
      case Some(value) =>
        // Scenario/location-based starts are expected to map deterministically to the configured
        // cell whenever that cell resolves to a province.
        provinceIdByCell.get(value) match
          case Some(exactProvinceId) => Right(Some(exactProvinceId))
          case None =>
            val preferSeaStart = isSeaStartNation(nation)
            provinceIdByCell
              .get(value)
              .filter(provinceId => isProvinceTerrainCompatible(provinceId, terrainMaskByProvince, preferSeaStart))
              .orElse(nearestCompatibleProvinceIdByCell(value, provinceIdByCell, terrainMaskByProvince, preferSeaStart))
              .orElse(nearestProvinceIdForLocation(value, provinceCentroids, terrainMaskByProvince, preferSeaStart))
              .map(Some(_))
              .toRight(IllegalArgumentException(s"Could not resolve $label at (${value.x.value}, ${value.y.value}) to a province"))

  private def buildProvinceIdByCell(
      provinceRuns: Vector[Pb],
      mapDimensions: model.map.MapDimensions
  ): Map[ProvinceLocation, ProvinceId] =
    val runsByY = provinceRuns.groupBy(_.y).view.mapValues(_.sortBy(_.x)).toMap
    (for
      yCell <- 0 until mapDimensions.height.value
      xCell <- 0 until mapDimensions.width.value
      yPixel = math.max(0, math.min(mapDimensions.height.value * 160 - 1, yCell * 160 + 80))
      xPixel = math.max(0, math.min(mapDimensions.width.value * 256 - 1, xCell * 256 + 128))
      provinceId <- provinceIdAtPixel(runsByY, xPixel, yPixel)
    yield ProvinceLocation(XCell(xCell), YCell(yCell)) -> provinceId).toMap

  private def provinceIdAtPixel(
      runsByY: Map[Int, Vector[Pb]],
      xPixel: Int,
      yPixel: Int
  ): Option[ProvinceId] =
    runsByY
      .get(yPixel)
      .flatMap(_.find(run => xPixel >= run.x && xPixel < run.x + run.length))
      .map(_.province)

  private def nearestProvinceIdForLocation(
      target: ProvinceLocation,
      provinceCentroids: Map[ProvinceId, ProvinceLocation],
      terrainMaskByProvince: Map[ProvinceId, Long],
      preferSeaStart: Boolean
  ): Option[ProvinceId] =
    provinceCentroids.toVector
      .filter { case (provinceId, _) =>
        isProvinceTerrainCompatible(provinceId, terrainMaskByProvince, preferSeaStart)
      }
      .sortBy(_._1.value)
      .minByOption { case (_, location) =>
        val deltaX = location.x.value - target.x.value
        val deltaY = location.y.value - target.y.value
        (deltaX.toLong * deltaX.toLong) + (deltaY.toLong * deltaY.toLong)
      }
      .map(_._1)

  private def nearestCompatibleProvinceIdByCell(
      target: ProvinceLocation,
      provinceIdByCell: Map[ProvinceLocation, ProvinceId],
      terrainMaskByProvince: Map[ProvinceId, Long],
      preferSeaStart: Boolean
  ): Option[ProvinceId] =
    provinceIdByCell.toVector
      .filter { case (_, provinceId) =>
        isProvinceTerrainCompatible(provinceId, terrainMaskByProvince, preferSeaStart)
      }
      .sortBy { case (location, provinceId) =>
        val deltaX = location.x.value - target.x.value
        val deltaY = location.y.value - target.y.value
        val manhattan = math.abs(deltaX) + math.abs(deltaY)
        val squared = (deltaX.toLong * deltaX.toLong) + (deltaY.toLong * deltaY.toLong)
        (manhattan, squared, provinceId.value)
      }
      .headOption
      .map(_._2)

  private def isProvinceTerrainCompatible(
      provinceId: ProvinceId,
      terrainMaskByProvince: Map[ProvinceId, Long],
      preferSeaStart: Boolean
  ): Boolean =
    val terrainMask = TerrainMask(terrainMaskByProvince.getOrElse(provinceId, 0L))
    val isSea = terrainMask.hasFlag(TerrainFlag.Sea) || terrainMask.hasFlag(TerrainFlag.DeepSea)
    if preferSeaStart then isSea else !isSea

  private def isSeaStartNation(
      nation: Nation
  ): Boolean =
    nation match
      case Nation.Pelagia_Early
          | Nation.Oceania_Early
          | Nation.Therodos_Early
          | Nation.Atlantis_Early
          | Nation.Rlyeh_Early
          | Nation.Ys_Middle
          | Nation.Pelagia_Middle
          | Nation.Oceania_Middle
          | Nation.Atlantis_Middle
          | Nation.Rlyeh_Middle
          | Nation.Atlantis_Late
          | Nation.Rlyeh_Late
          | Nation.BantayTubig_Early => true
      case _ => false

  private def nearestProvinceLocation(
      target: ProvinceLocation,
      provinceLocations: ProvinceLocations
  ): Option[ProvinceLocation] =
    provinceLocations.indexByLocation.keys.toVector.minByOption { location =>
      val deltaX = location.x.value - target.x.value
      val deltaY = location.y.value - target.y.value
      (deltaX.toLong * deltaX.toLong) + (deltaY.toLong * deltaY.toLong)
    }

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
      thronePlacements: Vector[ThronePlacement],
      randomCornerProvinceIds: Vector[ProvinceId]
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[MapState]] =
    throneGenerationMode match
      case ThroneGenerationMode.RandomCorners(_, _, _) =>
        if randomCornerProvinceIds.isEmpty then summon[Async[Sequencer]].pure(errorChannel.pure(state))
        else summon[Async[Sequencer]].pure(errorChannel.pure(applyThroneTerrainFlagsOnly(state, randomCornerProvinceIds)))
      case _ =>
        if thronePlacements.isEmpty then summon[Async[Sequencer]].pure(errorChannel.pure(state))
        else thronePlacementService.update[ErrorChannel](state, thronePlacements)

  private def applyThroneTerrainFlagsOnly(
      state: MapState,
      throneProvinces: Vector[ProvinceId]
  ): MapState =
    val throneProvinceSet = throneProvinces.toSet
    val updatedTerrains = state.terrains.map {
      case terrain @ Terrain(province, mask) =>
        val updatedMask =
          if throneProvinceSet.contains(province) then TerrainMask(mask).withFlag(TerrainFlag.GoodThrone)
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
      randomCornerProvinceIds: Vector[ProvinceId],
      state: MapState,
      throneDefenderSetPieces: Vector[ThroneDefenderSetPiece]
  ): Vector[model.map.MapDirective] =
    val setPieceByLevel = throneDefenderSetPieces.map(setPiece => setPiece.throneLevel.value -> setPiece).toMap
    val provinceLevels =
      throneGenerationMode match
        case ThroneGenerationMode.RandomCorners(throneLevel, _, _) =>
          randomCornerProvinceIds.map(provinceId => (provinceId, throneLevel.value))
        case _ =>
          thronePlacements.flatMap { placement =>
            val maybeProvince = state.provinceLocations.provinceIdAt(placement.location)
            val maybeLevel =
              placement.level.map(_.value)
                .orElse(placement.id.flatMap(resolveThroneLevelForFeatureId).map(_.value))
            (maybeProvince, maybeLevel) match
              case (Some(province), Some(level)) => Some((province, level))
              case _ => None
          }

    provinceLevels.flatMap { case (province, level) =>
      setPieceByLevel.get(level).map { setPiece =>
        Vector(Land(province), Commander(setPiece.commanderType)) ++
          setPiece.units.map(unit => Units(unit.count, unit.unitType))
      }.getOrElse(Vector.empty)
    }

  private def resolveThroneLevelForFeatureId(featureId: FeatureId): Option[ThroneLevel] =
    if DomFeature.levelOneThrones.exists(_.id.value == featureId.value) then Some(ThroneLevel(1))
    else if DomFeature.levelTwoThrones.exists(_.id.value == featureId.value) then Some(ThroneLevel(2))
    else if DomFeature.levelThreeThrones.exists(_.id.value == featureId.value) then Some(ThroneLevel(3))
    else None

  private def resolveSurfaceThronePlacements(
      request: MapGenerationRequest,
      state: MapState,
      provinceIdByCell: Map[ProvinceLocation, ProvinceId]
  ): Vector[ThronePlacement] =
    request.throneGenerationMode match
      case ThroneGenerationMode.Disabled => Vector.empty
      case ThroneGenerationMode.Configured(surfaceThrones, _) =>
        resolveConfiguredThronePlacements(state, surfaceThrones, provinceIdByCell)
      case ThroneGenerationMode.RandomCorners(_, _, _) =>
        Vector.empty

  private def resolveSurfaceRandomCornerProvinceIds(
      request: MapGenerationRequest,
      provinceRuns: Vector[Pb]
  ): Vector[ProvinceId] =
    request.throneGenerationMode match
      case ThroneGenerationMode.RandomCorners(_, includeSurface, _) =>
        if includeSurface then randomCornerThroneProvinceIds(provinceRuns)
        else Vector.empty
      case _ =>
        Vector.empty

  private def resolveUndergroundThronePlacements(
      request: MapGenerationRequest,
      state: MapState,
      provinceIdByCell: Map[ProvinceLocation, ProvinceId]
  ): Vector[ThronePlacement] =
    request.throneGenerationMode match
      case ThroneGenerationMode.Disabled => Vector.empty
      case ThroneGenerationMode.Configured(_, undergroundThrones) =>
        resolveConfiguredThronePlacements(state, undergroundThrones, provinceIdByCell)
      case ThroneGenerationMode.RandomCorners(_, _, _) =>
        Vector.empty

  private def resolveUndergroundRandomCornerProvinceIds(
      request: MapGenerationRequest,
      provinceRuns: Vector[Pb]
  ): Vector[ProvinceId] =
    request.throneGenerationMode match
      case ThroneGenerationMode.RandomCorners(_, _, includeUnderground) =>
        if includeUnderground then randomCornerThroneProvinceIds(provinceRuns)
        else Vector.empty
      case _ =>
        Vector.empty

  private def resolveConfiguredThronePlacements(
      state: MapState,
      targets: Vector[ConfiguredThronePlacementTarget],
      provinceIdByCell: Map[ProvinceLocation, ProvinceId]
  ): Vector[ThronePlacement] =
    targets.map { target =>
      val resolvedLocation =
        target.location
          .flatMap(provinceIdByCell.get)
          .flatMap(state.provinceLocations.locationOf)
          .orElse(target.location)
          .flatMap(location => nearestProvinceLocation(location, state.provinceLocations))
          .orElse(target.provinceId.flatMap(state.provinceLocations.locationOf))
          .getOrElse(ProvinceLocation(XCell(-1), YCell(-1)))
      ThronePlacement(
        location = resolvedLocation,
        level = target.throneLevel,
        id = target.throneFeatureId
      )
    }

  private def randomCornerThroneProvinceIds(
      provinceRuns: Vector[Pb]
  ): Vector[ProvinceId] =
    if provinceRuns.isEmpty then Vector.empty
    else
      val (minX, maxX, minY, maxY) = provinceRuns.foldLeft((Int.MaxValue, Int.MinValue, Int.MaxValue, Int.MinValue)) {
        case ((currentMinX, currentMaxX, currentMinY, currentMaxY), run) =>
          val runStart = run.x
          val runEnd = run.x + run.length - 1
          (
            math.min(currentMinX, runStart),
            math.max(currentMaxX, runEnd),
            math.min(currentMinY, run.y),
            math.max(currentMaxY, run.y)
          )
      }

      val cornerPixelCoordinates = Vector(
        (minX, minY),
        (maxX, minY),
        (minX, maxY),
        (maxX, maxY)
      )
      val provinceIdsByCorner =
        cornerPixelCoordinates.map { case (xPixel, yPixel) =>
          rankProvincesByDistanceToPixel(provinceRuns, xPixel, yPixel)
        }

      provinceIdsByCorner.foldLeft(Vector.empty[ProvinceId]) { (accumulator, rankedProvinceIds) =>
        rankedProvinceIds.find(provinceId => !accumulator.contains(provinceId)) match
          case Some(provinceId) => accumulator :+ provinceId
          case None =>
            rankedProvinceIds.headOption match
              case Some(provinceId) if !accumulator.contains(provinceId) => accumulator :+ provinceId
              case _ => accumulator
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
