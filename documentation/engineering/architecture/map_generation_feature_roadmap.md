# Map Generation Feature Roadmap

[Back to Architecture Index](README.md)

This roadmap coordinates the next generator features and keeps decisions explicit.

## Scope
- Nation-aware generation input (MapNuke nation config parsing).
- Start/throne override capabilities.
- Terrain-aware image painting quality improvements.
- Throne set-piece defenders tied to tier/metadata.

## Guiding Constraints
- Keep service-contract boundaries stable so implementations can be swapped.
- Preserve deterministic generation for fixed seed + config.
- Do not block near-term delivery on format migration decisions.

## Phase 1: Nation Config Ingestion (One-Time Parse)
## Goal
- Parse existing MapNuke nation config once per generation request and project into internal domain types.

## Deliverables
- `NationGenerationConfigLoader` capability (read + parse external config).
- `NationGenerationConfig` domain model (capital preference, coastal/water bias near capital, other parsed hints).
- `MapGenerationRequest` extension to accept optional nation config input path(s).
- Validation/reporting output for unsupported/ignored fields.

## Decisions
- Near-term: parse MapNuke format directly.
- Deferred: convert to native config format after field stability is known.

## Acceptance Criteria
- Generation runs with and without nation config.
- Parsed capital/water-ratio constraints influence generated map.
- Unknown fields are reported without hard failing (unless marked required).

## Phase 2: Start/Throne Override Expansion
## Goal
- Expose explicit overrides for start locations and throne locations in the generation pipeline.

## Deliverables
- `GenerationOverrideConfig` model covering:
  - start province override per nation/faction
  - throne province override (tier and optional throne id)
- Integration in `MapGenerationService` pipeline, reusing `SpawnPlacementService` and `ThronePlacementService` contracts where possible.
- Conflict-resolution rules:
  - invalid province reference
  - duplicate throne assignment
  - start and throne collisions

## Acceptance Criteria
- Overrides are applied deterministically.
- Invalid overrides return actionable diagnostics.
- Existing no-override behavior remains unchanged.

## Phase 3: Terrain-Aware Painting Contract
## Goal
- Improve generated image readability by painting unique colors by terrain type behind a stable service contract.

## Deliverables
- `MapTerrainPainter` capability contract (input: ownership + terrain masks; output: RGB map image).
- Default implementation: one deterministic color per major terrain kind (plains, forest, farm, swamp, waste, highland, sea/deep sea).
- Generator config option selecting painter implementation/palette preset.

## Acceptance Criteria
- Generated `.tga` visibly differentiates terrain types.
- Current border/anchor behavior remains correct.
- Alternate painter can be swapped without touching generation orchestration.

## Phase 4: Throne Set-Piece Defenders
## Goal
- Add throne defender set pieces keyed by throne tier and optional throne metadata.

## Deliverables
- `ThroneSetPieceService` capability:
  - input: resolved throne placements + tier/id metadata
  - output: defender script directives/events for assigned provinces
- Set-piece catalog model:
  - default set per tier
  - optional per-throne-id override
  - optional randomness pool with deterministic seed
- Integration point after throne placement and before map write.

## Acceptance Criteria
- Thrones can receive deterministic defender compositions by tier.
- Existing behavior preserved when set-piece config is absent.
- Defender assignment is testable and auditable from generated directives.

## Dependency Order
1. Phase 1 (nation parsing) unlocks nation-aware generation constraints.
2. Phase 2 (overrides) ensures controlled placement for starts/thrones.
3. Phase 3 (painting contract) is independent of 1/2 and can run in parallel.
4. Phase 4 depends on reliable throne placement output from Phase 2.

## Testing Plan by Phase
- Phase 1: parser golden tests + mapping tests + malformed input diagnostics.
- Phase 2: override precedence/validation tests + end-to-end map generation tests.
- Phase 3: painter snapshot tests + ownership/palette contract tests.
- Phase 4: set-piece selection tests + directive emission integration tests.

## Risks and Mitigations
- External format drift (MapNuke):
  - Mitigation: tolerant parser + explicit unsupported-field reporting.
- Override conflicts producing invalid states:
  - Mitigation: centralized validation and conflict policy.
- Visual changes masking geometry regressions:
  - Mitigation: keep geometry correctness tests independent from painter output.
- Throne-defender coupling complexity:
  - Mitigation: isolate in dedicated capability and keep clear input/output model.

## Suggested Execution Slices
1. Slice A: Phase 1 parser + domain model + no-op integration.
2. Slice B: Phase 2 start/throne override application + validation.
3. Slice C: Phase 3 terrain painter contract + default palette impl.
4. Slice D: Phase 4 throne set-piece service + initial tier templates.
