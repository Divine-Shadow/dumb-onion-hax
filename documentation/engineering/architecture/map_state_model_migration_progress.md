# Map State Model Migration Progress

This living document tracks implementation against the [Map State Model Migration Plan](map_state_model_migration.md).

## Status Summary
- **MapState & ProvinceLocationService** – implemented (`model/src/main/scala/model/map/MapState.scala`, `model/src/main/scala/model/map/ProvinceLocationService.scala`).
- **MapDirective coverage** – incomplete (`model/src/main/scala/model/map/MapDirective.scala` missing `#pb` and comment variants).
- **Parser** – still drops unknown lines (`model/src/main/scala/model/map/MapFileParser.scala`).
- **Pass 1 builder** – buffers full stream and loses pass-through directives (`model/src/main/scala/model/map/MapState.scala`).
- **Pass 2 writer** – renders only state-owned directives; pass-through lost (`model/src/main/scala/model/map/MapDirectiveCodecs.scala`, `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/MapWriter.scala`).
- **Services lacking MapDirective-stream integration** – `MapLayerLoader.scala`, `MapProcessingService.scala`, `GateDirectiveService.scala`, `ThronePlacementService.scala`, `SpawnPlacementService.scala`, `WrapConversionService.scala`, `WrapSeverService.scala`, `MapSizeValidator.scala`.
- **Legacy province-id logic** – `ProvincePixels` directive still defined (`model/src/main/scala/model/map/MapDirective.scala`).

## Blockers
- Missing feature flag to toggle the two-pass pipeline.
- Tests and adapters still consume direct `MapDirective` streams.
- Parser does not surface unmapped `MapDirective` lines as defects.
- Writer lacks pass-through re-emission.
