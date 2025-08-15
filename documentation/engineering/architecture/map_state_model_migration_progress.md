# Map State Model Migration Progress

This living document tracks progress on the [Map State Model Migration Plan](map_state_model_migration.md).

The plan now classifies directives as either **State-owned** or **Pass-through** and removes the previous "unknown" bucket. Migration proceeds in a two-pass pipeline where Pass 1 derives `MapState` from the full event stream and Pass 2 re-emits all pass-through directives verbatim.

## Migration Steps
1. **Define DirectiveEvent variants for MapSize, ProvinceAt, Adjacency** – [State-owned] – *pending*
2. **Capture ImageRow directives as events** – [Pass-through] – *pending*
3. **Capture Comment directives as events** – [Pass-through] – *pending*
4. **Upgrade MapFileParser to emit DirectiveEvents and fail on unmapped lines** – [State-owned] *(Two-pass: Pass 1 intake)* – *pending*
5. **Pass 1: fold event stream into MapState while preserving pass-through directives** – [State-owned] *(Two-pass)* – *pending*
6. **Pass 2: re-emit preserved pass-through directives verbatim** – [Pass-through] *(Two-pass)* – *pending*
7. **Refactor MapLayerLoader, MapProcessingService, GateDirectiveService, ThronePlacementService, SpawnPlacementService, WrapConversionService, WrapSeverService, MapSizeValidator to consume MapState and event streams under feature flags with tests** – [State-owned] – *pending*
8. **Remove legacy province-id coordinate logic after dual-path verification** – [State-owned] – *pending*

## Blockers
- **Missing feature flags** – fails *Feature flags* criterion.
- **Tests still consume `MapDirective` streams** – fails *Integration DoD* criterion.
- **No pass-through re-emission in writer** – fails *Two-pass enforcement* criterion.
- **Parser does not surface unmapped directives as defects** – fails *Fidelity & typing* criterion.

