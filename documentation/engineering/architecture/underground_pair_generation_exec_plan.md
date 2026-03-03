# Underground Pair Generation From Scratch

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This plan is maintained in accordance with `.agent/PLANS.md`.

## Purpose / Big Picture

The generator can now produce a fully linked two-plane map set from scratch: a surface map and a `_plane2` underworld map. This enables immediate in-game validation of underground behavior without hand-authoring a cave layer.

In this iteration, every province can be connected to its counterpart underground via `#gate <id> <id>`, underworld terrain is forced to cave-only, and cave terrain flags are removed from the surface layer.

## Progress

- [x] (2026-03-03 01:55Z) Reviewed known-good multi-plane maps and confirmed that surface and `_plane2` share identical `#pb` ownership runs.
- [x] (2026-03-03 01:58Z) Added optional underground generation mode to `MapGenerationRequest` and CLI/config parsing.
- [x] (2026-03-03 02:03Z) Implemented generation of companion files `<map>_plane2.map` and `<map>_plane2.tga`.
- [x] (2026-03-03 02:05Z) Implemented all-province tunnel gates (`#gate id id`) on both layers when enabled.
- [x] (2026-03-03 02:07Z) Forced cave-only terrain underground and stripped cave/cavewall flags from surface terrain.
- [x] (2026-03-03 02:10Z) Added and passed targeted tests for CLI parsing and pair generation behavior.
- [ ] (2026-03-03 02:14Z) Commit code and docs for this underground slice.

## Surprises & Discoveries

- Observation: Known-good maps keep `#pb` exactly the same between surface and `_plane2`.
  Evidence: `Abyssal_elves.map` vs `Abyssal_elves_plane2.map` and `TCP41.map` vs `TCP41_plane2.map` have equal `#pb` line counts and content.

- Observation: Existing real maps commonly duplicate matching `#gate id id` entries in both map files.
  Evidence: Sample map pairs under `data/sample-game-map-reader-input/*` include the same gate lines in both files.

## Decision Log

- Decision: Reuse surface generated `#pb` runs for underground.
  Rationale: Matches known-good format and avoids coordinate drift between layers.
  Date/Author: 2026-03-03 / Codex

- Decision: Use cave-only mask (`TerrainFlag.Cave`) for all underground provinces in this iteration.
  Rationale: Delivers a simple, deterministic, verifiable first underground slice.
  Date/Author: 2026-03-03 / Codex

- Decision: Strip cave and cavewall flags from surface terrains as a guardrail.
  Rationale: Enforces user requirement that cave terrains not appear above ground.
  Date/Author: 2026-03-03 / Codex

## Outcomes & Retrospective

The generator now produces a working surface/underworld pair suitable for in-game verification, with deterministic tunnels and terrain separation between layers.

Follow-up iteration focus:

- Add underworld-specific terrain variation (for example cave forests, cave walls, freshwater cave lakes) instead of all-cave.
- Define underworld adjacency/border style policy independently from surface if needed.
- Add automated end-to-end test that loads generated pair via map inspector workflow.

## Context and Orientation

Main implementation points:

- Pair generation orchestration: `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/MapGenerationService.scala`
- CLI request parsing: `apps/src/main/scala/com/crib/bills/dom6maps/apps/MapGeneratorCliApp.scala`
- Config model: `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/MapGeneratorConfig.scala`
- Tests: `apps/src/test/scala/com/crib/bills/dom6maps/services/mapeditor/MapGenerationServiceSpec.scala`, `apps/src/test/scala/com/crib/bills/dom6maps/apps/MapGeneratorCliAppSpec.scala`

## Plan of Work

Introduce an optional underground mode in the request/config surface. When enabled, write two map/image outputs from one generated geometry result:

- Surface: existing `<map>.map` + `<map>.tga`
- Underworld: `<map>_plane2.map` + `<map>_plane2.tga`

Use mirrored `#pb` and mirrored `#gate id id` sets for tunnel behavior. Keep plane metadata explicit via `#planename`.

## Concrete Steps

From repository root `/home/bayesartre/dev/dumb-onion-hax`:

    sbt compile
    sbt "project apps" "testOnly com.crib.bills.dom6maps.apps.MapGeneratorCliAppSpec com.crib.bills.dom6maps.apps.services.mapeditor.MapGenerationServiceSpec"
    sbt -Ddom6.generator.configPath=/home/bayesartre/dev/dumb-onion-hax/map-generator.conf "project apps" "set Compile / run / fork := false" "runMain com.crib.bills.dom6maps.apps.MapGeneratorCliApp"

Expected results:

- Both map files and both image files are generated.
- Surface and underworld contain matching gate sets when tunnel mode is on.
- Underworld terrains are cave-only; surface has no cave/cavewall flags.

## Validation and Acceptance

Acceptance is met when:

1. `sample_mvp.map` and `sample_mvp_plane2.map` both exist, with corresponding `.tga` files.
2. Surface and underworld each include the same `#gate id id` set for all provinces.
3. Underworld has cave-only terrain masks.
4. Surface contains no cave/cavewall terrain flags.
5. In-game map opens with selectable underworld plane and no parse/load errors.

## Idempotence and Recovery

Generation is idempotent for fixed seed/config and safely overwrites prior outputs. If a config parse fails, disable underground block or set valid values and rerun the same command.

## Artifacts and Notes

Generated pair target for manual game verification:

- `/mnt/c/Users/Shadow's Throne/AppData/Roaming/Dominions6/maps/sample-map-bundle/sample_mvp.map`
- `/mnt/c/Users/Shadow's Throne/AppData/Roaming/Dominions6/maps/sample-map-bundle/sample_mvp_plane2.map`
- `/mnt/c/Users/Shadow's Throne/AppData/Roaming/Dominions6/maps/sample-map-bundle/sample_mvp.tga`
- `/mnt/c/Users/Shadow's Throne/AppData/Roaming/Dominions6/maps/sample-map-bundle/sample_mvp_plane2.tga`

## Interfaces and Dependencies

New interface additions:

- `UndergroundGenerationMode` in `MapGenerationService.scala`:
  - `Disabled`
  - `MirroredPlane(planeName, connectEveryProvinceWithTunnel)`

Config additions:

- `MapGeneratorUndergroundConfig` in `MapGeneratorConfig.scala`
- parsed by `MapGeneratorCliApp.parseUndergroundGenerationModeForTest`

---

Revision Note (2026-03-03 02:14Z): Initial ExecPlan for first underground-from-scratch generation slice. Implementation complete and validated, commit pending.
