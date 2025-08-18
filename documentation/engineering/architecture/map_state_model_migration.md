# Map State Model Migration Plan

Migration to a directive stream `MapState` ensures that only documented `MapDirective` lines drive state while all other lines round‑trip verbatim. See [Migration Progress](map_state_model_migration_progress.md) for current status.

## Directive Inventory & Authority Map
| Directive | Authority | Emission rule | Status (evidence) |
| --- | --- | --- | --- |
| `#mapsize` | State-owned | state-rendered | `model/src/main/scala/model/map/MapDirective.scala`, `model/src/main/scala/model/map/MapState.scala` |
| `#dom2title`, `#description` | State-owned | state-rendered | `model/src/main/scala/model/map/MapDirective.scala` |
| `#wraparound`, `#hwraparound`, `#vwraparound`, `#nowraparound` | State-owned | state-rendered | `model/src/main/scala/model/map/MapDirective.scala` |
| `#allowedplayer`, `#specstart` | State-owned | state-rendered | `model/src/main/scala/model/map/MapDirective.scala` |
| `#terrain`, `#gate`, `#neighbour`, `#neighbourspec` | State-owned | state-rendered | `model/src/main/scala/model/map/MapDirective.scala` |
| `#province` | State-owned | state-rendered | `model/src/main/scala/model/map/MapDirective.scala` |
| `#pb` | Pass-through + Derived-input | verbatim; source for derived province locations | `MapDirective.Pb` in `model/src/main/scala/model/map/MapDirective.scala` |
| `#imagefile`, `#winterimagefile`, `#domversion`, `#nodeepcaves`, `#nodeepchoice`, `#mapnohide`, `#maptextcol`, `#mapdomcol`, `#landname`, `#planename` | Pass-through | verbatim | `model/src/main/scala/model/map/MapDirective.scala` |
| comment lines (`--`) | Pass-through | verbatim | `MapDirective.Comment` in `model/src/main/scala/model/map/MapDirective.scala` |
| Derived province locations (from `#pb` runs via `ProvinceLocationService`) | Derived-input | state-rendered | `model/src/main/scala/model/map/ProvinceLocationService.scala` |

Documentation review surfaced additional directives like `#scenario` that remain unparsed.

## Two-Pass Behavior
Pass 1 consumes the full `MapDirective` stream to build `MapState`, retaining pass-through directives. Pass 2 re-emits pass-through directives verbatim and renders state-owned directives from `MapState` in canonical order; state-rendered output wins on conflicts. Verification relies on pass-through round-trips, ordered state sections, and end-to-end runs.

### Ordering & Merge Rules
- **State-rendered families:** map metadata (`#mapsize`, `#dom2title`, `#description`), wrap settings (`#wraparound`, `#hwraparound`, `#vwraparound`, `#nowraparound`), player constraints (`#allowedplayer`, `#specstart`), and province blocks (`#province`, `#terrain`, `#gate`, `#neighbour`, `#neighbourspec`, `#landname`).
- **Merge policy:** Pass 2 emits state-rendered families in the above order; preserved pass-through lines are interleaved at their original relative positions. When state-rendered directives conflict with existing lines, the state-rendered output wins.

## MapLayer Abstraction
`MapLayer` pairs the incremental `MapState` with the residual directive stream so services can mutate state while preserving unrelated lines. A `MapLayerLoader` parses an input file into this structure, enabling transformations in the [Map Editor Processing Pipeline](map_editor_pipeline.md) and [Map Modification Services](map_modification_services.md) to operate without sacrificing pass-through fidelity.

## Executable Step Cards
1. **Scaffolding (Complete)**
   *Purpose:* Establish minimal state model and province location derivation.
   *Preconditions (evidence):* `model/src/main/scala/model/map/MapState.scala`, `model/src/main/scala/model/map/ProvinceLocationService.scala`.
   *Actions:* none.
   *Deliverables:* existing files.
   *Verification:* `model/src/test/scala/model/map/MapStateSpec.scala`, `model/src/test/scala/model/map/ProvinceLocationServiceSpec.scala`.
   *Status:* Complete.

2. **Complete `MapDirective` coverage (Complete)**
   *Purpose:* Represent all documented directives and comments.
   *Preconditions (evidence):* `model/src/main/scala/model/map/MapDirective.scala` defines `Pb` and `Comment`.
   *Actions:* none.
   *Deliverables:* `MapDirective.scala`, inventory.
   *Verification:* parser unit tests.
   *Status:* Complete.

3. **Parser emits all `MapDirective` variants and fails on unmapped lines (Complete)**
   *Purpose:* Ensure Pass 1 sees every directive and surfaces defects.
   *Preconditions (evidence):* `model/src/main/scala/model/map/MapFileParser.scala` emits comments, covers `#pb`, and raises on unknown directives.
   *Actions:* emit all `MapDirective` variants, capture comments, raise on unmapped lines.
   *Deliverables:* `MapFileParser.scala`, parser tests.
   *Verification:* round-trip tests with malformed input.
   *Status:* Complete.

4. **Pass 1 `MapLayer` builder retains pass-through directives (In Progress)**
   *Purpose:* Stream derivation of a `MapLayer` that combines `MapState` with the preserved directive stream.
   *Preconditions (evidence):* `model/src/main/scala/model/map/MapState.scala` folds directives incrementally but lacks memory‑bound verification; `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/MapLayerLoader.scala` sketches loader logic.
   *Actions:* fold over the `MapDirective` stream producing a `MapLayer` without buffering the full input; finalize `MapLayerLoader` implementation.
   *Deliverables:* updated `MapState.scala`, `MapLayer.scala`, `MapLayerLoader.scala`, memory tests.
   *Verification:* unit tests comparing to existing `fromDirectives`, `model/src/test/scala/model/map/MapStateMemorySpec.scala`.
   *Status:* In Progress.

5. **Pass 2 writer merges outputs and pass-through lines (Complete)**
   *Purpose:* Re-emit pass-through directives verbatim and render state-owned directives in order.
   *Preconditions (evidence):* `model/src/main/scala/model/map/MapDirectiveCodecs.scala` and `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/MapWriter.scala` only render `MapState`.
   *Actions:* render state-owned directives in canonical order, merge with preserved pass-through stream, append verbatim lines with ordering checks.
   *Deliverables:* `MapDirectiveCodecs.scala`, `MapWriter.scala`.
   *Verification:* golden file round-trip with ordering assertions.
   *Status:* Complete.

6. **Refactor map modification services (Complete)**
   *Purpose:* Operate on `MapState` plus directive stream.
   *Preconditions (evidence):* services such as `apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/GateDirectiveService.scala` accept only `MapState`.
   *Actions:* update service signatures and adapters.
   *Deliverables:* service files and tests.
   *Verification:* service-level tests.
   *Status:* Complete.

7. **Retire province-id location logic (Complete)**
   *Purpose:* Remove reliance on `ProvincePixels` after new pipeline is proven.
   *Preconditions (evidence):* `ProvincePixels` directive removed from `model/src/main/scala/model/map/MapDirective.scala`.
   *Actions:* drop `ProvincePixels` handling and old lookup paths.
   *Deliverables:* directive models and docs.
   *Verification:* compilation and end-to-end tests.
   *Status:* Complete.

## Two-pass Proof Checklist
- Diff original map with Pass 2 output to confirm ordering and verbatim pass-through.
- Ensure `MapState` from Pass 1 matches `MapState.fromDirectives` for the same input.
- Run end-to-end flow to confirm dual-path behaviour.

## Perf/Memory Acceptance
- Centroid and grid derivation from `#pb` runs must stream the directive stream once and retain only `MapState` in memory.
- Pass 1 and `ProvinceLocationService` will be exercised under constrained heap (`-Xmx64m`) in tests to assert constant-space behaviour.
- Large-map golden tests confirm output equivalence without requiring full-file buffering.

