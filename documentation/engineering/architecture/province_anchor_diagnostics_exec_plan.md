# Province Anchor Diagnostics and Surgical Fix Plan

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document is maintained in accordance with `.agent/PLANS.md`.

## Purpose / Big Picture

The current map output can show deterministic province-center edge clinging in Dominions even when province ownership polygons look correct. After this plan, developers can generate a map bundle that includes machine-readable province anchor diagnostics and a visual debug overlay, then use that evidence to implement one targeted fix instead of repeating geometry guesses.

## Progress

- [x] (2026-03-06 23:12Z) Captured reset objective: stop blind geometry tuning and move to diagnostics-first workflow.
- [x] (2026-03-06 23:12Z) Added this ExecPlan and linked it from the architecture index.
- [x] (2026-03-06 23:26Z) Implemented `MapGenerationDiagnosticsWriter` to emit per-province anchor report and debug image.
- [x] (2026-03-06 23:28Z) Wired diagnostics writer into `MapGenerationService` surface generation flow.
- [x] (2026-03-06 23:39Z) Replaced synthetic anchor prelude from longest-run midpoint with `ProvinceAnchorLocator`-derived anchor pixels.
- [x] (2026-03-06 23:44Z) Generated scenario-based diagnostic bundle at `/mnt/c/Users/Shadow's Throne/AppData/Roaming/Dominions6/maps/anti_kaiju_3p_anchor_diag`.
- [x] (2026-03-06 23:47Z) Passed compile and targeted test suite: `MapGeneratorCliAppSpec` and `MapGenerationServiceSpec`.

## Surprises & Discoveries

- Observation: Province id/name changes across runs, but the problematic geometric locus remains stable and continues to cling to an edge.
  Evidence: User screenshots across variants show identical in-world position while ids changed (for example `47` to `45`).
- Observation: A synthetic ownership intervention produced a transient pseudo-center artifact ("micro-lake" style detached behavior), proving that non-conservative ownership edits can corrupt center behavior.
  Evidence: User screenshot and follow-up report where province showed dual interpretation between apparent tiny area and actual selectable polygon.
- Observation: For a problematic province, computed area centroid in raster space is clearly inside the province.
  Evidence: Local diagnostic script output for province `45` showed `roundedInside: true`, so engine center is not simply area-centroid.
- Observation: Existing generator prepended a synthetic one-pixel `#pb` run per province from longest horizontal run midpoint.
  Evidence: `MapGeometryGenerator.runGeneration` used `buildAnchorPreludeRuns` and prepended those runs ahead of all ownership runs.
- Observation: Dominions center latching can be driven by that first prepended `#pb` pixel rather than true centroid.
  Evidence: The deterministic border-cling behavior aligns with first-run anchor mechanics and persisted across terrain/noise changes.

## Decision Log

- Decision: Halt further exploratory warp/geometry tuning until diagnostics are available.
  Rationale: Repeated heuristic edits did not converge and introduced regressions.
  Date/Author: 2026-03-06 / Codex
- Decision: Add non-invasive diagnostics artifacts generated from existing `#pb` and map state.
  Rationale: We need reproducible evidence of anchor candidates versus game behavior before any new fix.
  Date/Author: 2026-03-06 / Codex
- Decision: Use `ProvinceAnchorLocator` output as the prepended anchor run instead of longest-run midpoint.
  Rationale: `ProvinceAnchorLocator` explicitly prefers interior pixels and nearest-to-centroid fallback.
  Date/Author: 2026-03-06 / Codex

## Outcomes & Retrospective

At this checkpoint, diagnostics are implemented and integrated, and one surgical fix was applied to the prepended anchor mechanism. Generation now writes sidecar anchor diagnostics for every map and no longer uses longest-run midpoint as first-run anchor.

Remaining follow-up is in-game verification with user screenshots on the new `anti_kaiju_3p_anchor_diag` output to confirm edge-cling is eliminated for the previously problematic tile(s).

## Context and Orientation

Map generation for this repo lives in `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor`. `MapGenerationService.scala` orchestrates generation, writing `.map`, writing `.tga`, and optional variants. Province ownership is represented by `#pb` directives (`model/src/main/scala/model/map/MapDirective.scala`), rasterized by `ProvincePixelRasterizer` (`model/src/main/scala/model/map/image/ProvincePixelRasterizer.scala`), and existing anchor candidates are computed by `ProvinceAnchorLocator` (`model/src/main/scala/model/map/image/ProvinceAnchorLocator.scala`).

The diagnostic writer introduced by this plan must not alter gameplay directives; it only writes additional files next to the generated map.

## Plan of Work

Create a new service in `apps/.../mapeditor` named `MapGenerationDiagnosticsWriter`. It will read pass-through directives from the generated surface layer, collect `#pb` runs, reconstruct ownership raster, and compute four anchor candidates per province: first `#pb` pixel encountered, longest-run midpoint, area centroid (rounded), and `ProvinceAnchorLocator` pixel. It will write these into a text report (`<mapName>_debug_anchors.txt`) and a visual overlay image (`<mapName>_debug_anchors.tga`) where each candidate type has a distinct color marker.

Then wire this writer into `MapGenerationServiceImpl` immediately after successful main image generation for the surface layer. If map image generation fails, diagnostics should not run. If diagnostics fail, map generation should fail with that error because diagnostics are now part of the debugging workflow.

No map directives (`#pb`, `#terrain`, `#neighbour`, starts) are changed by this milestone.

## Concrete Steps

From repo root `/home/bayesartre/dev/dumb-onion-hax`:

1. Add `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/MapGenerationDiagnosticsWriter.scala` with:
   - trait `MapGenerationDiagnosticsWriter[Sequencer[_]]`
   - class `MapGenerationDiagnosticsWriterImpl[Sequencer[_]: Async: Files]`
2. Update constructor of `MapGenerationServiceImpl` in `MapGenerationService.scala` to accept diagnostics writer with default implementation.
3. Call diagnostics writer in surface write flow after main image write and before terrain variants.
4. Run:
     sbt "project apps" compile
5. Generate one map via CLI config and verify diagnostics files are present in output bundle.

Expected transcript fragments:

    Writing map to .../<map>.map
    Writing map image to .../<map>.tga
    Writing map diagnostics report to .../<map>_debug_anchors.txt
    Writing map diagnostics overlay to .../<map>_debug_anchors.tga

## Validation and Acceptance

Acceptance is satisfied when a generated bundle contains all of:

- `<mapName>.map`
- `<mapName>.tga`
- `<mapName>_debug_anchors.txt`
- `<mapName>_debug_anchors.tga`

And the report contains entries for known problematic ids when present (for example `45` or `47`) including all four candidate anchor coordinates.

## Idempotence and Recovery

These changes are additive. Re-running map generation overwrites diagnostics artifacts in the same bundle safely. If diagnostics writing fails due malformed `#pb` or map size mismatch, fix the underlying generation inputs and re-run; no cleanup is required beyond deleting the failed output directory.

## Artifacts and Notes

Current reproducibility evidence before implementation:

    province 45 first #pb at x=4017 y=360 len=1
    centroid(rounded) is inside province according to raster check

This is exactly why we need sidecar diagnostics for every run.

## Interfaces and Dependencies

In `apps/.../MapGenerationDiagnosticsWriter.scala`, define:

    trait MapGenerationDiagnosticsWriter[Sequencer[_]] {
      def write[ErrorChannel[_]](
          layer: MapLayer[Sequencer],
          mapName: String,
          outputBundleDirectory: Path
      )(using files: Files[Sequencer], errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]): Sequencer[ErrorChannel[Unit]]
    }

Use existing dependencies only:

- `ProvincePixelRasterizer` for ownership reconstruction
- `ProvinceAnchorLocator` for one anchor candidate
- `TargaImageEncoder` for overlay output
- `MapImagePainter` base painting helpers (optional but preferred)

---

Revision note (2026-03-06 / Codex): Created initial diagnostics-first ExecPlan after repeated non-converging geometry iterations and user-requested reset.
