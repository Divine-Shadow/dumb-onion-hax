# Ground-Surface Duel Service

The Ground-Surface Duel service composes map modification capabilities to generate a duel-style game mode. It consumes surface and cave map directive streams, normalizes them, and injects fixed gate and throne placements while severing wrap connections.

## Requirements
- Accept two `Stream[Sequencer, MapDirective]` values representing the surface and cave layers.
- Map dimensions must be square, share the same size, and use an odd side length.
- Remove all existing `Gate` and throne directives before applying new placements.
- Place gates at the midpoint of each edge on both layers and link matching provinces across layers.
- Place thrones at all four corners on both layers.
- Apply vertical and horizontal severing so the resulting maps have no wrap.

## Domain Types
- `MapSize`: value class wrapping an odd `Int` side length with constructor validation.
- `EdgeMidpoints`: pure function computing midpoint province identifiers for a given `MapSize`.
- `CornerProvinces`: pure function returning the four corner province identifiers.
- `GroundSurfaceDuelConfig`: optional throne level and additional gate settings for future extension.

## Capability Sketches
1. **MapSizeValidator**
   - Computes map dimensions from a directive stream.
   - Ensures both layers are equal, square, and odd-sized.
2. **PlacementPlanner**
   - Uses `MapSize` to derive:
     - `Vector[GateSpec]` linking surface and cave midpoints.
     - `Vector[ThronePlacement]` for corner provinces on each layer.
   - Pure and side-effect free.
3. **GroundSurfaceDuelPipe**
   - Orchestrates transformation of both directive streams.
   - Contract sketch:
     ```scala
     trait GroundSurfaceDuelPipe[Sequencer[_]]:
       def apply[ErrorChannel[_]](
         surface: Stream[Sequencer, MapDirective],
         cave: Stream[Sequencer, MapDirective],
         config: GroundSurfaceDuelConfig
       )(using
         errorChannel: MonadError[ErrorChannel, status.Error] & Traverse[ErrorChannel]
       ): Sequencer[ErrorChannel[(Vector[MapDirective], Vector[MapDirective])]]
     ```
   - Internally invokes:
     1. `MapSizeValidator` for dimension checks.
     2. `PlacementPlanner` to compute gate and throne targets.
     3. `GateDirectiveService` on both layers.
     4. `ThronePlacementService` on both layers.
     5. `WrapSever.severVertically` and `WrapSever.severHorizontally` to remove wrap.

## Service Boundaries & Extension Points
- `GroundSurfaceDuelPipe` remains a thin orchestrator; calculation and I/O concerns live in dedicated capabilities, preserving single responsibility and testability.
- `GateDirectiveService` and `ThronePlacementService` already strip existing directives, satisfying the removal requirement without modification.
- `MapSizeValidator` can be reused by other game modes requiring size constraints.
- Future features such as variable throne levels or asymmetric gate links fit naturally into `GroundSurfaceDuelConfig` and `PlacementPlanner` without affecting the pipe contract.

## Testing Strategy
- Provide `Stub` instances for all capabilities to verify pure placement logic using in-memory directive streams.
- Integration tests compose real services to confirm that gates, thrones, and severing appear in the expected provinces for a small sample map.
- Run `sbt compile` as a sanity check for documentation changes.

[Back to Architecture Guide Index](README.md)
