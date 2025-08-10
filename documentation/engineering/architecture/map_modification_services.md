# Map Modification Services

This plan introduces capability-based services for injecting gate and throne data into Dominions 6 maps. It extends the [Map Editor Processing Pipeline](map_editor_pipeline.md) with transformations for two map layers and a dedicated throne layer.

## Requirements
- Accept two input `.map` files representing surface and cave layers.
- Remove existing `#gate` directives from both maps and add new ones provided by a future specification.
- Update the throne layer so that designated provinces become thrones.
- Setting a throne modifies the province's `#terrain` bitmask by adding `33554432` which corresponds to `TerrainFlag.GoodStart`.
- Magic numbers are avoided by manipulating `TerrainFlag` values through domain types and helper functions.

## Domain Types
- `GateSpec`: pairs of `ProvinceId` values describing a gate connection.
- `ThronePlacement`: province identifier and throne level.
- `TerrainMask`: value class wrapping an `Int` with methods:
  - `withFlag(flag: TerrainFlag): TerrainMask`
  - `withoutFlag(flag: TerrainFlag): TerrainMask`
  - `hasFlag(flag: TerrainFlag): Boolean`
- `TerrainFlag.Throne`: alias for `TerrainFlag.GoodStart` so the throne bit is never hard coded.

## Capabilities
1. **MapLayerLoader**
   - Parses a map file into a stream of `MapDirective` using `MapFileParser`.
   - Contract sketch:
     ```scala
     trait MapLayerLoader[Sequencer[_]] {
       def load[ErrorChannel[_]](path: fs2.io.file.Path)
         (using Files[Sequencer],
                MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel],
                model.trace.Id
         ): Sequencer[ErrorChannel[Vector[MapDirective]]]
     }
     ```
2. **GateDirectiveService**
   - Removes any existing `Gate` directives and appends new ones from `Vector[GateSpec]`.
   - Exposes an FS2 `Pipe` so it composes with other transformations.
3. **ThronePlacementService**
   - Accepts `Vector[ThronePlacement]` and rewrites `Terrain` directives by toggling flags via `TerrainMask`.
   - Ensures non-specified provinces have throne flags cleared.
4. **MapModificationService**
   - Higher-order orchestrator that:
     1. Loads surface and cave layers via `MapLayerLoader`.
     2. Applies `GateDirectiveService` to both layers.
     3. Applies `ThronePlacementService` to the throne layer.
     4. Delegates rendering to the existing `MapWriter`.
   - Only coordinates the above capabilities, maintaining single responsibility.

## Testing Strategy
- Provide `Stub` implementations for each capability mirroring the [capability trait pattern](service_and_capability_patterns.md).
- Unit tests operate on in-memory streams to verify removal and addition of directives without file I/O.
- The orchestrator can be tested with sample maps to ensure composition order is correct.

## Future Work
- Define the `GateSpec` and `ThronePlacement` formats once specifications are available.
- Implement production `Impl` classes using `fs2.Stream` and the effect system.
- Extend `TerrainFlag` with the `Throne` alias and introduce `TerrainMask` utilities.
