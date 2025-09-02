# Throne Feature Application Plan

This plan describes how to apply throne placements to a map layer using existing services.

## Goals
- Enable random level 1 thrones in designated coordinates.
- Enable random level 2 thrones in designated coordinates.
- Apply specific thrones to explicitly paired coordinates.

## Plan
1. **Model Input**
   - Convert coordinate pairs to `ProvinceLocation`.
   - For random lists, map each location to `ThronePlacement(location, ThroneLevel(1))` or `ThroneLevel(2)`.
   - For fixed pairs, map to `ThronePlacement` using the given level.
2. **Combine Placements**
   - Concatenate the three vectors into one `Vector[ThronePlacement]`.
3. **Load Map Layer**
   - Parse the target map layer via `MapLayerLoader` to obtain a `MapState`.
4. **Apply Thrones**
   - Call `ThronePlacementService.update` with the current `MapState` and combined placements.
   - The service toggles `TerrainFlag.Throne` on provinces resolved from the designated coordinates and clears it elsewhere.
5. **Persist Changes**
   - Use `MapWriter` or the higher-level `MapModificationService` to render the updated map layer to disk.

## User Interface
- The [Wrap Selection Service](../wrap_selection_service.md) dialog also presents existing throne inputs
  with an "Override Thrones" checkbox.
  - When checked the dialog writes a sample configuration file `throne-override.conf`:
    ```
    overrides = [
      { x = 0, y = 0, level = 3 },
      { x = 4, y = 2, id = 1358 }
    ]
    ```
    Each placement may specify either a throne `level` or a specific throne `id` and is parsed into a `ThroneConfiguration` once the user confirms the dialog.
  - If the box is unchecked no file is produced and only the in-memory selections are used.
- `ThroneFeatureService` composes the resulting configuration with other chosen operations before applying and persisting changes.

## Implementation
- `ThroneFeatureService` orchestrates the above workflow by loading the map layer, applying throne placements, and writing the result.

## Alignment with Codebase Principles
- **Capability Traits** – The plan leans on `ThronePlacementService` and `MapLayerLoader`, respecting the contract/implementation split.
- **Domain-Driven Types** – `ProvinceId`, `ThroneLevel`, and `ThronePlacement` avoid primitives while modeling the domain clearly.
- **Pure Transformations** – `ThronePlacementService.update` returns a new `MapState` without performing I/O.
- **Composable Pipeline** – Integrating with `MapModificationService` keeps each responsibility focused and composable.

[Back to Architecture Guide Index](README.md)
