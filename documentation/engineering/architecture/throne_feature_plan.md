# Throne Feature Application Plan

This plan describes how to apply throne placements to a map layer using existing services.

## Goals
- Enable random level 1 thrones in designated provinces.
- Enable random level 2 thrones in designated provinces.
- Apply specific thrones to explicitly paired provinces.

## Plan
1. **Model Input**
   - Convert province identifiers to `ProvinceId`.
   - For random lists, map each province to `ThronePlacement(province, ThroneLevel(1))` or `ThroneLevel(2)`.
   - For fixed pairs, map to `ThronePlacement` using the given level.
2. **Combine Placements**
   - Concatenate the three vectors into one `Vector[ThronePlacement]`.
3. **Load Map Layer**
   - Parse the target map layer via `MapLayerLoader` to obtain a `MapState`.
4. **Apply Thrones**
   - Call `ThronePlacementService.update` with the current `MapState` and combined placements.
   - The service toggles `TerrainFlag.Throne` on designated provinces and clears it elsewhere.
5. **Persist Changes**
   - Use `MapWriter` or the higher-level `MapModificationService` to render the updated map layer to disk.

## Alignment with Codebase Principles
- **Capability Traits** – The plan leans on `ThronePlacementService` and `MapLayerLoader`, respecting the contract/implementation split.
- **Domain-Driven Types** – `ProvinceId`, `ThroneLevel`, and `ThronePlacement` avoid primitives while modeling the domain clearly.
- **Pure Transformations** – `ThronePlacementService.update` returns a new `MapState` without performing I/O.
- **Composable Pipeline** – Integrating with `MapModificationService` keeps each responsibility focused and composable.

[Back to Architecture Guide Index](README.md)
