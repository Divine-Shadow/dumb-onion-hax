# Terrain Distribution Weighting For Generated Maps

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This plan is maintained in accordance with `.agent/PLANS.md`.

## Purpose / Big Picture

Map generator output currently hard-codes land terrain assignment thresholds, which causes some runs to cluster into a narrow set of terrain types and makes tuning impossible from configuration. After this change, users can define explicit terrain weights in generator config (swamp, waste, highland, forest, farm, extra lake) and get generated maps that approximate those target distributions while still preserving deterministic generation for a fixed seed.

The user-visible result is that `map-generator.conf` gains a `terrain-distribution` block, and changing those percentages changes generated terrain composition in the resulting `.map` and `.tga`.

## Progress

- [x] (2026-03-02 23:46Z) Investigated existing terrain generation path and confirmed thresholds were hard-coded in `MapGeometryGenerator.buildTerrainMask`.
- [x] (2026-03-02 23:50Z) Added `TerrainDistributionPolicy` model with validation in `model/src/main/scala/model/map/generation/TerrainDistributionPolicy.scala`.
- [x] (2026-03-02 23:53Z) Threaded terrain distribution policy through `GeometryGenerationInput` and generator terrain assignment logic.
- [x] (2026-03-02 23:56Z) Added optional `terrainDistribution` config model and CLI parsing path in apps module.
- [x] (2026-03-02 23:58Z) Added tests for policy validation and generator behavior under forced terrain distribution.
- [x] (2026-03-02 23:59Z) Ran full targeted compile/test validation for model and apps changes.
- [x] (2026-03-03 00:53Z) Regenerated `sample_mvp` and validated generated terrain mask diversity against configured profile.
- [x] (2026-03-03 00:54Z) Commit implementation and updated docs.
- [x] (2026-03-03 00:36Z) Switched terrain assignment to deterministic random per-province sampling from configured distribution.
- [x] (2026-03-03 00:38Z) Revalidated apps test suite and regenerated `sample_mvp` with updated sampler.
- [ ] (2026-03-03 00:39Z) Commit post-ExecPlan sampler refinement and doc updates.

## Surprises & Discoveries

- Observation: Existing generator defaults effectively encoded a fixed non-sea land split (swamp/farm/forest/waste/highland/plain) by hard-coded threshold values rather than policy data.
  Evidence: `buildTerrainMask` in `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/MapGeometryGenerator.scala` used constant thresholds `0.16, 0.32, 0.50, 0.68, 0.84`.

- Observation: `map-generator.conf` and `apps/map-generator.conf` are ignored from git, so user runtime behavior can differ from repository defaults unless both local files are updated.
  Evidence: prior sessions showed local CLI runs loading `apps/map-generator.conf`.

- Observation: Initial post-ExecPlan sampler variants (bit-truncating hash and coordinate sine noise) still produced undesirable bias on small map sizes.
  Evidence: generated terrain mask counts repeatedly clustered into a subset of configured classes during test runs.

## Decision Log

- Decision: Keep `seaRatio` separate and apply terrain weights only to non-sea provinces.
  Rationale: Existing behavior and config already use `seaRatio`, and user request focused on land-terrain composition similar to Dominions advanced generator settings.
  Date/Author: 2026-03-02 / Codex

- Decision: Model “Extra Lakes” as `TerrainFlag.FreshWater` in map output.
  Rationale: This maps directly to an existing terrain flag and provides clear correspondence with requested distribution controls.
  Date/Author: 2026-03-02 / Codex

- Decision: Make the new config block optional and default to the previous implicit distribution.
  Rationale: Backward compatibility for existing configs and behavior while enabling explicit tuning.
  Date/Author: 2026-03-02 / Codex

- Decision: Replace integer-truncating hash sampling with continuous deterministic noise (`sin`-based fractional mapping) to reduce distribution skew.
  Rationale: Truncation-based hashing created clustering that prevented configured percentages from appearing reliably on small to medium generated maps.
  Date/Author: 2026-03-03 / Codex

- Decision: Use deterministic random per-province sampling (SplitMix64-based draw) rather than coordinate-based noise for terrain assignment.
  Rationale: User requested random sampling from configured distribution, and per-province deterministic draws provide unbiased frequency matching while preserving seed determinism.
  Date/Author: 2026-03-03 / Codex

## Outcomes & Retrospective

Implementation and validation are complete except for final commit. The change introduces explicit, validated control over terrain composition and keeps deterministic output characteristics intact, with terrain type assigned by deterministic random sampling from configured percentages.

Observed result in generated `sample_mvp` (48 provinces, sea ratio 0.30) includes all requested terrain families:

- plains (`0`)
- sea (`4`)
- freshwater / extra-lake (`8`)
- highland (`16`)
- swamp (`32`)
- waste (`64`)
- forest (`128`)
- farm (`256`)

## Context and Orientation

Terrain generation for map bundles is orchestrated in `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/MapGenerationService.scala` and implemented in `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/MapGeometryGenerator.scala`.

Generator input parameters are defined by `model/src/main/scala/model/map/generation/GeometryGenerationInput.scala`, and CLI config parsing is performed by:

- `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/MapGeneratorConfig.scala`
- `apps/src/main/scala/com/crib/bills/dom6maps/apps/MapGeneratorCliApp.scala`

The new policy type is implemented in `model/src/main/scala/model/map/generation/TerrainDistributionPolicy.scala` and mirrors the style used by `BorderSpecGenerationPolicy`.

## Plan of Work

Define a new domain policy that captures non-sea terrain percentages and validates that each percentage is in `[0.0, 1.0]` and that the sum is at most `1.0`. Compute plains as the remainder.

Extend geometry input to carry this policy so generation logic is purely data-driven. Replace hard-coded terrain thresholds with cumulative thresholds derived from policy values and map “extra lake” to `TerrainFlag.FreshWater`.

Add optional config support so users can specify terrain percentages in `map-generator.conf` without breaking existing configs. Convert config values into `TerrainDistributionPolicy` during request construction.

Add tests at model and app layers for policy validation, parser acceptance/rejection, and generator conformance to an extreme policy (100% waste on land).

## Concrete Steps

From repository root `/home/bayesartre/dev/dumb-onion-hax`, run:

    sbt compile
    sbt "project model" "testOnly com.crib.bills.dom6maps.model.map.generation.*"
    sbt "project apps" "testOnly com.crib.bills.dom6maps.apps.MapGeneratorCliAppSpec com.crib.bills.dom6maps.apps.services.mapeditor.MapGeometryGeneratorSpec"
    sbt "project apps" "runMain com.crib.bills.dom6maps.apps.MapGeneratorCliApp"

Expected outcomes:

- Compile succeeds.
- New terrain policy specs pass.
- CLI parsing accepts valid terrain-distribution config and rejects invalid totals.
- Generator conformance test passes for forced all-waste land policy.
- Generated map bundle appears under configured output directory and shows varied land terrain types according to configured percentages.

## Validation and Acceptance

Acceptance criteria:

1. With default config (no `terrain-distribution` block), generator still succeeds and remains deterministic.
2. With explicit `terrain-distribution`, generated map contains terrain masks reflecting desired distribution family (for example, more forest/farm and less waste/highland).
3. Invalid percentage sums above 1.0 fail early with actionable error.
4. Existing map generation and image writing tests remain green.

## Idempotence and Recovery

All edits are additive and safe to re-run. Running generation repeatedly overwrites output files in the same configured folder with deterministic output for fixed seed.

If a config parse fails due to invalid percentages, reduce total non-sea terrain percentages to at most `1.0` and rerun the same command.

## Artifacts and Notes

Key edited files:

- `model/src/main/scala/model/map/generation/TerrainDistributionPolicy.scala`
- `model/src/main/scala/model/map/generation/GeometryGenerationInput.scala`
- `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/MapGeometryGenerator.scala`
- `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/MapGeneratorConfig.scala`
- `apps/src/main/scala/com/crib/bills/dom6maps/apps/MapGeneratorCliApp.scala`
- tests under `model/src/test/scala/model/map/generation` and `apps/src/test/scala/com/crib/bills/dom6maps`

## Interfaces and Dependencies

New interface contract:

- `TerrainDistributionPolicy.fromRaw[ErrorChannel[_]](...)` validates config percentages and returns typed policy values.
- `GeometryGenerationInput` now includes:
  - `terrainDistributionPolicy: TerrainDistributionPolicy = TerrainDistributionPolicy.default`

Generator dependency:

- `MapGeometryGenerator` uses `input.terrainDistributionPolicy` when selecting non-sea terrain masks.

---

Revision Note (2026-03-03 00:39Z): Reopened living plan to reflect post-commit sampler refinement requested by user ("randomly sample from distribution"), with commit pending for this follow-up.
