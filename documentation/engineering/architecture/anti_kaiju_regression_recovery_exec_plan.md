# Anti-Kaiju Regression Recovery ExecPlan

[Back to Architecture Index](README.md)

This ExecPlan is a living document. Keep `Progress`, `Discoveries`, `Decision Log`, and `Outcomes` updated as work proceeds.

## Purpose
- Recover parity with legacy Anti-Kaiju behavior while preserving the anchor snap fix.
- Stop ad-hoc tuning by comparing legacy vs current output against explicit acceptance checks.

## Current Comparison (Legacy vs Current)

### Baseline Intent (Legacy)
- Layout: `18 x 5`, horizontal wrap.
- Scenario placements from MapNuke layout:
  - Players at `(0,2)`, `(6,2)`, `(12,2)`
  - Thrones at `(3,4)`, `(9,4)`, `(15,4)`, `(3,0)`, `(9,0)`, `(15,0)`
- Visual style:
  - Curved/squiggled province boundaries
  - Clear border readability
  - River overlays visible
- Mechanical behavior:
  - Oceania starts in water and has water-heavy local zone
  - Effective map vertical structure matches intended 5-row layout

### Observed Current Issues
- Visual:
  - Some borders hard to read (especially same-luminance terrain neighbors).
  - River overlays not consistently visible.
  - Horizontal seam artifact at map edge.
  - Province shapes are polygonal (not sufficiently curved).
- Mechanical:
  - Vertical structure appears collapsed to ~3 rows in many areas.
  - Scenario throne spacing/placement appears off in gameplay view.
  - Scenario starts not matching intent (Oceania often non-water).

## Key Discovery (Confirmed)
- Nation/allocation layer is currently effectively bypassed for Anti-Kaiju test runs:
  - `data/map-generation/allocation-profiles.conf` currently contains `profiles=[]`.
  - Recent verification configs were generated with `allocation.enabled=false`.
- Consequence: nation-specific environment shaping (for example Oceania water bias/cap-zone behavior) cannot take effect.

## What We Must Preserve
- Anchor snap fix:
  - stronger interior anchor selection
  - no seam-split province components
- This fix removed border-latched centers and must remain intact while we recover parity.

## Scope
- In scope:
  - Scenario->placement correctness (starts/thrones)
  - Allocation profile wiring and nation-specific zone generation
  - Visual readability (borders/rivers/seam)
  - Curvature quality level
- Out of scope (for this pass):
  - Underground anti-kaiju variant generation beyond preserving existing support

## Progress
- [x] Captured regression matrix from user screenshots and notes.
- [x] Confirmed allocation profile bypass as root contributor for Oceania mismatch.
- [ ] Add reproducible legacy-vs-current generation presets in repo (config files, not ad-hoc `/tmp` only).
- [ ] Re-enable allocation policy for Anti-Kaiju scenario with non-empty nation profiles.
- [ ] Validate scenario starts/thrones resolve to intended cells/provinces in emitted `.map`.
- [ ] Tune geometry curvature/readability without reintroducing anchor snap.
- [ ] Validate in game with screenshot checklist.

## Execution Plan

1. Repro Presets
- Add checked-in generator config presets for:
  - `anti_kaiju_current_repro`
  - `anti_kaiju_parity_candidate`
- Ensure each preset uses unique map title/name to avoid Dominions cache confusion.

2. Restore Nation Allocation Path
- Populate `data/map-generation/allocation-profiles.conf` with explicit profiles for:
  - EA Pangaea (`7`) surface
  - EA Oceania (`41`) surface (water-heavy cap circle)
  - EA Marverni (`12`) surface
- Run with `allocation.enabled=true` for parity candidate preset.

3. Placement Correctness Gate
- Add diagnostic assertions/tools that check:
  - scenario start cells map to expected province ids
  - throne targets map near intended `(x,y)` placements
  - starts are not silently remapped by fallback unless exact target is missing

4. Visual Quality Pass
- Improve border contrast for low-contrast terrain neighbor pairs.
- Keep rivers visible over bright terrain.
- Keep seam smoothing, but avoid introducing visible vertical blend bars.
- Increase boundary squiggle while retaining connected-component invariants.

5. Acceptance Validation
- Mechanical acceptance:
  - Oceania start in water province.
  - Local Oceania region shows elevated water share vs other starts.
  - Thrones visually/mechanically aligned with intended spacing.
  - Effective play area feels 5 rows high, not collapsed corridors.
- Visual acceptance:
  - Rivers visible in marked problem zones.
  - No obvious seam artifact at wrap edge.
  - Province boundaries are clearly readable and non-rectilinear.

## Concrete Files to Touch
- Scenario/allocation:
  - `data/map-generation/scenarios.conf`
  - `data/map-generation/allocation-profiles.conf`
- Generation flow:
  - `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/MapGenerationService.scala`
  - `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/MapGeometryGenerator.scala`
- Rendering/overlays:
  - `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/MapImageWriter.scala`
  - `model/src/main/scala/model/map/image/MapConnectionOverlayPainter.scala`

## Decision Log
- Decision: Treat this as parity recovery with explicit acceptance checks, not incremental “looks better” edits.
  - Rationale: repeated iterations regressed unrelated behavior.
- Decision: Preserve anchor fix as non-negotiable baseline.
  - Rationale: center latching bug is resolved and must not return.

## Outcomes
- Pending implementation cycle.
