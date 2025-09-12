# Ground-Surface Duel Service

The Ground-Surface Duel service composes map modification capabilities to generate a duel-style game mode. It consumes surface and cave `MapState` values, normalizes them, injects fixed gate and throne placements, places players, and severs wrap connections.

## Requirements
- Accept two `MapState` values representing the surface and cave layers.
- Accept `SurfaceNation` and `UndergroundNation` values identifying each player.
- Map dimensions must be square and share the same size.
- Remove all existing gates, thrones, `allowedplayer`, and `specstart` entries before applying new placements.
- Place gates at the midpoint of each edge on both layers and link matching provinces across layers.
- Place thrones at all four corners on both layers.
- Place each player at the center province of their respective layer, adding `allowedplayer` and `specstart` entries.
- Apply vertical and horizontal severing so the resulting maps have no wrap.

## Domain Types
- `MapSize`: value class wrapping a positive `Int` side length with constructor validation.
- `EdgeMidpoints`: pure function computing midpoint province identifiers for a given `MapSize`.
- `CornerLocations`: pure function returning the four corner coordinates.
- `CenterProvince`: pure function returning the central province identifier.
- `SurfaceNation` and `UndergroundNation`: value classes wrapping `Nation`.
- `PlayerSpawn`: nation and province pair for `allowedplayer`/`specstart` placement.
- `GroundSurfaceDuelConfig`: throne level (default `ThroneLevel(1)`) and hooks for future extension.

## Capability Sketches
1. **MapSizeValidator**
   - Computes map dimensions from `MapState` values.
   - Ensures both layers are equal and square.
2. **PlacementPlanner**
   - Uses `MapSize` to derive:
     - `Vector[GateSpec]` linking surface and cave midpoints.
     - `Vector[ThronePlacement]` for corner coordinates on each layer.
   - Pure and side-effect free.
3. **GroundSurfaceDuelPipe**
   - Orchestrates transformation of both states.
   - Contract sketch:
     ```scala
     trait GroundSurfaceDuelPipe[Sequencer[_]]:
       def apply[ErrorChannel[_]](
         surface: MapState,
         cave: MapState,
         config: GroundSurfaceDuelConfig,
         surfaceNation: SurfaceNation,
         undergroundNation: UndergroundNation
       )(using
         errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
       ): Sequencer[ErrorChannel[(MapState, MapState)]]
     ```
   - Internally invokes:
     1. `MapSizeValidator` for dimension checks.
     2. `PlacementPlanner` to compute gate and throne targets.
     3. `GateDirectiveService` on both layers.
     4. `ThronePlacementService` on both layers.
     5. `SpawnPlacementService` on both layers.
     6. `WrapSeverService.severVertically` and `WrapSeverService.severHorizontally` to remove wrap.

## Service Boundaries & Extension Points
- `GroundSurfaceDuelPipe` remains a thin orchestrator; calculation and I/O concerns live in dedicated capabilities, preserving single responsibility and testability.
- `GateDirectiveService` and `ThronePlacementService` already strip existing directives, satisfying the removal requirement without modification.
- `MapSizeValidator` can be reused by other game modes requiring size constraints.
- Future features such as variable throne levels or asymmetric gate links fit naturally into `GroundSurfaceDuelConfig` and `PlacementPlanner` without affecting the pipe contract.

## Testing Strategy
- Provide `Stub` instances for all capabilities to verify pure placement logic using in-memory `MapState` values.
- Integration tests compose real services to confirm that gates, thrones, and severing appear at the expected coordinates for a small sample map.
- Run `sbt compile` as a sanity check for documentation changes.

[Back to Architecture Guide Index](README.md)
