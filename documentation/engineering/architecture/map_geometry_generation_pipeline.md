# Map Geometry Generation Pipeline

[Back to Architecture Index](README.md)

This page documents generation of province geometry for new maps.

## Purpose
- Generate valid province ownership runs (`#pb`) from a deterministic algorithm.
- Derive map topology (`#neighbour`) and terrain masks (`#terrain`) from generated geometry.
- Keep generation strategy swappable through a service contract.

## Inputs
- `GeometryGenerationInput`
  - `mapSize`
  - `provinceCount`
  - `wrapState`
  - `seed`
  - `seaRatio`
  - `noiseScale`
  - `gridJitter`
  - `terrainDistributionPolicy`

## Contract
- `MapGeometryGenerator`
  - Capability trait for geometry generation.
- `GridNoiseMapGeometryGeneratorImpl`
  - Default implementation.
  - Uses jittered grid seeds and nearest-seed ownership assignment.

## Output
- `GeneratedGeometry`
  - `provincePixelRuns` (`#pb` source)
  - `adjacency`
  - `borders`
  - `terrainByProvince`
  - `provinceCentroids`

## Flow
1. Build deterministic province seeds from grid + seed + jitter.
2. Assign each pixel to nearest province seed.
3. Remap province identifiers by deterministic anchor scan order.
4. Compress ownership to run-length `#pb` rows.
5. Derive adjacency by scanning province boundaries.
6. Assign terrain masks per province from deterministic distribution sampling.
7. Derive province centroids and convert to province cell coordinates.

## Dominions Compatibility Invariants
- `#pb y` values are emitted in bottom-origin coordinates (0 = bottom row).
- Province identity must be remapped consistently before writing:
  - `#pb`
  - `#terrain`
  - `#neighbour`
  - derived centroid/province-location indexes
- If these outputs are not remapped from the same ownership array, gameplay identity can drift from visual borders (clicking one tile selects another province).
- Anchor and snap behavior details (including seam-split failure mode and fix): [Province Anchor Mechanics and Snap Fix](province_anchor_mechanics_and_snap_fix.md)

## Terrain Image Variants
- `TerrainImageVariantService` writes optional additional terrain look images.
- Policy is controlled by `TerrainImageVariantPolicy`:
  - `BaseOnly`
  - `BaseAndWinter`
  - `FullTerrainSet`

## Integration
- `MapGenerationService` composes:
  - `MapGeometryGenerator`
  - `GeneratedBorderSpecService` (in-memory `NeighbourSpec` enrichment before write/render)
  - `ThronePlacementService` (optional random-corner or configured throne application)
  - `MapWriter`
  - `MapImageWriter`
  - `TerrainImageVariantService`
- The generator emits `.map` and `.tga` artifacts to a target directory.
- When underground companion mode is enabled, the generator also emits `_plane2.map` and `_plane2.tga` with mirrored ownership runs and per-province tunnel gates.
- When throne generation is enabled:
  - random mode resolves corner provinces from generated ownership (`#pb`) and applies level-based random thrones
  - configured mode applies explicit throne placements
  - each layer (surface/underground) can receive independent throne inputs
- MVP required directive/file checklist: [MVP Map Set Requirements](mvp_map_set_requirements.md)

## Testing Focus
- Determinism for fixed seeds.
- `#pb` ownership validity and non-empty province output.
- Adjacency derivation under wrap and non-wrap conditions.
- End-to-end generation of map and image artifacts.
