# Map State Model Migration Plan

This document outlines how to evolve the map editing pipeline to use a compact `MapState` and directive events. The goal is to remove reliance on province-id based location data and instead pull coordinates and adjacency from a service.

## Impacted Services
- `model.map.MapFileParser` – parse raw `.map` lines into `DirectiveEvent` instead of `MapDirective`.
- `apps.services.mapeditor.MapLayerLoader` – build `MapState` from the event stream.
- `apps.services.mapeditor.MapDirectiveTransformer` and `MapProcessingService` – operate on `MapState` and filtered event streams.
- `apps.services.mapeditor.MapWriter` – emit directives from `MapState` and pass through remaining events.
- Map modification pipes (`GateDirectiveService`, `ThronePlacementService`, `SpawnPlacementService`, `WrapConversionService`, `WrapSeverService`, `MapSizeValidator`) – adjust signatures to consume event streams or `MapState`.

## Model Overview
1. **Directive Events** – sealed hierarchy representing parsed directives:
   - Known events: `MapSize`, `ProvinceAt`, `Adjacency`, `ImageRow`, `Comment`.
   - Unknown lines preserved as `UnknownDirective`.
2. **MapState** – authoritative facts derived from events:
   - Map dimensions.
   - Vector of province locations.
   - Adjacency graph.
   - Configuration flags to guide transformations.
   - Excludes heavy payloads such as image rows.
3. **ProvinceLocationService** – capability that returns province coordinates and adjacency metadata. Replaces hard coded `ProvinceId` lookups.

## Staged Migration
1. **Scaffolding**
   - Introduce `DirectiveEvent`, `MapState`, and `ProvinceLocationService` in `model.map`.
   - Provide JSON encoders/decoders following the existing renderer pattern.
2. **Parser Upgrade**
   - Extend `MapFileParser` to emit `DirectiveEvent`.
   - Supply adapters converting legacy `MapDirective` streams to the new events.
3. **State Builder**
   - Implement a fold over the event stream that accumulates `MapState` and filters out directives represented in state.
   - Replace province-id based coordinates with data from `ProvinceLocationService`.
4. **Writer & Passthrough**
   - Update `MapWriter` to render canonical directives from `MapState`.
   - Merge with a "remaining directives" stream for comments, image rows, and unknown lines.
5. **Service Refactor**
   - Modify `MapLayerLoader`, `MapDirectiveTransformer`, `MapProcessingService`, and map modification pipes to consume `MapState` and event streams.
   - Adjust tests in `apps` to use the new model.
6. **Removal & Hardening**
   - Deprecate `ProvincePixels` and related province-id coordinate logic.
   - Run full compilation and service-level tests before removing legacy pathways.

## Rollout Notes
- Each stage should ship behind a feature flag to avoid disrupting current workflows.
- Update examples such as `MapEditorWrapApp` once the pipeline stabilizes.
- Document new service contracts and encode references in API docs.

## References
- [Map Editor Processing Pipeline](map_editor_pipeline.md)
- [Map Modification Services](map_modification_services.md)
