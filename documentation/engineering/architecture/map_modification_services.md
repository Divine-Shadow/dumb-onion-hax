# Map Modification Services

Map modification services inject gate and throne data into Dominions 6 maps. They extend the [Map Editor Processing Pipeline](map_editor_pipeline.md) with transformations for two map layers and a throne-aware layer.

## Requirements
- Accept two input `.map` files representing surface and cave layers.
- Remove existing `#gate` directives from both maps and append new ones.
- Update the throne layer so that designated provinces become thrones.
- Setting a throne modifies the province's `#terrain` bitmask by adding `67108864` which corresponds to `TerrainFlag.GoodStart`.
- Magic numbers are avoided by manipulating `TerrainFlag` values through domain types and helper functions.

## Domain Types
- `GateSpec`: pairs of `ProvinceId` values describing a gate connection.
- `ThroneLevel`: value class wrapping an `Int` representing throne strength.
- `ThronePlacement`: province identifier and throne level.
- `TerrainMask`: value class wrapping a `Long` with methods:
  - `withFlag(flag: TerrainFlag): TerrainMask`
  - `withoutFlag(flag: TerrainFlag): TerrainMask`
  - `hasFlag(flag: TerrainFlag): Boolean`
- `TerrainFlag.Throne`: alias for `TerrainFlag.GoodStart` so the throne bit is never hard coded.

## Capabilities
1. **MapLayerLoader**
   - Parses a map file into a [`MapLayer`](map_state_model_migration.md#maplayer-abstraction) holding `MapState` and the remaining directive stream.
   - Contract sketch:
     ```scala
     trait MapLayerLoader[Sequencer[_]] {
       def load[ErrorChannel[_]](path: fs2.io.file.Path)(using
         fs2.io.file.Files[Sequencer],
         cats.MonadError[ErrorChannel, Throwable] & cats.Traverse[ErrorChannel]
       ): Sequencer[ErrorChannel[model.map.MapLayer[Sequencer]]]
     }
     ```
2. **GateDirectiveService**
   - Replaces existing gates in a `MapState` with new ones from `Vector[GateSpec]`.
3. **ThronePlacementService**
   - Accepts `Vector[ThronePlacement]` and rewrites `Terrain` entries in `MapState` by toggling flags via `TerrainMask`.
   - Ensures non-specified provinces have throne flags cleared.
4. **MapModificationService**
   - Higher-order orchestrator that:
     1. Loads surface and cave layers via `MapLayerLoader`.
     2. Applies `GateDirectiveService` to both layers.
     3. Applies `ThronePlacementService` to the throne layer.
     4. Delegates rendering to `MapWriter`, preserving pass-through directives.
   - Coordinates the above capabilities, maintaining single responsibility.

## Testing Strategy
- Provide `Stub` implementations for each capability mirroring the [capability trait pattern](service_and_capability_patterns.md).
- Unit tests operate on in-memory `MapState` values to verify removal and addition of directives without file I/O.
- The orchestrator can be tested with sample maps to ensure composition order is correct.

## Future Work
- Integrate external specifications for gate and throne data.
