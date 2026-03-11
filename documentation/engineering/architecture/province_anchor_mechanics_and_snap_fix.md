# Province Anchor Mechanics and Snap Fix

[Back to Architecture Index](README.md)

This page explains how province anchor placement behaves in practice, why center snapping failed, and what we changed.

## Problem Summary
- Symptom in-game: province flags/units snapped to province borders (often on vertices), not inside province interiors.
- Symptom pattern: deterministic across runs for the same geometric locus, even when province ids changed.
- Most severe cases occurred near wrap seam behavior and on provinces that became visually disconnected components.

## Key Mechanics
- Dominions map ownership comes from `#pb <x> <y> <len> <province>`.
- The engine does not expose an explicit `#center` directive for province icon/unit placement.
- In observed behavior, icon/unit placement is strongly influenced by anchor-like pixels from ownership runs, not by simple area centroid.
- If ownership for one province is split into multiple disconnected pixel components, in-game placement can latch to undesirable border points.

## Root Causes We Identified
1. Synthetic prelude anchor choice was too weak:
- We prepend one `#pb` pixel per province before the full run list.
- Earlier, this prelude anchor could be near edges (`longest-run midpoint`), which increased border-latch risk.

2. Wrap-seam split provinces:
- Ownership generation in wrapped distance space could split a province across the horizontal seam into multiple disconnected components.
- This made anchor/placement behavior unstable and visually wrong even when topology was technically valid.

## Fixes Implemented
1. Stronger anchor selection:
- `ProvinceAnchorLocator` now prefers maximum boundary clearance first, then nearest-to-centroid tie-break.
- Prelude `#pb` anchor pixels now use `ProvinceAnchorLocator` output.

2. Remove seam-split ownership source:
- Ownership raster generation is performed in unwrapped (`NoWrap`) raster space.
- Gameplay wrap is still preserved via map wrap state (`#hwraparound`) and adjacency logic.
- Result: provinces remain single connected components in raster ownership while map still wraps for movement.

3. Diagnostics for verification:
- `MapGenerationDiagnosticsWriter` emits:
  - `<mapName>_debug_anchors.txt`
  - `<mapName>_debug_anchors.tga`
- This allows comparing:
  - first run point
  - longest-run midpoint
  - rounded centroid
  - anchor-locator point

## Why This Resolved Snap
- Border-latching was not just “noise/centroid drift”; it correlated with anchor placement and disconnected ownership components.
- By forcing robust interior anchors and eliminating seam-split components, placement stopped snapping to borders in validation maps (`v3+` series).

## Guardrails for Future Changes
- Do not reintroduce prelude anchors based solely on run-length midpoint.
- Do not generate wrapped-distance ownership if it can split provinces across seams.
- Keep diagnostics artifacts enabled in generation workflow for regression triage.
- When debugging placement, inspect connected components per province from emitted `#pb` before tuning geometry noise.
