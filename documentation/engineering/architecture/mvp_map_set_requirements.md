# MVP Map Set Requirements

[Back to Architecture Index](README.md)

This page defines the minimum files and directives for an MVP generated map set.

## Required Files
- `<mapname>.map`
- `<mapname>.tga`

## Optional Files (Configured)
- `<mapname>_winter.tga`
- Terrain look variants (normal and optional winter suffix `w`):
  - `<mapname>_forest.tga`
  - `<mapname>_waste.tga`
  - `<mapname>_farm.tga`
  - `<mapname>_swamp.tga`
  - `<mapname>_highland.tga`
  - `<mapname>_plain.tga`
  - `<mapname>_kelp.tga`
  - `<mapname>_water.tga`

## Required `.map` Directives (MVP)
- `#dom2title <text>`
- `#imagefile <filename>.tga`
- `#mapsize <width> <height>`

## Directives Required for Generated Province Geometry
- `#pb <x> <y> <len> <province>`
- `#terrain <province> <mask>`
- `#neighbour <a> <b>`

## Recommended MVP Directives
- `#description "..."`
- One wrap directive (`#nowraparound`, `#hwraparound`, `#vwraparound`, or `#wraparound`)
- `#winterimagefile <filename>` when winter image generation is enabled

## Recommended Non-MVP Files
- `banner.png` and workshop metadata if publishing to Steam Workshop.
- `_plane2` map/image assets if supporting multi-plane maps.

## Current Generator Coverage
- Generates required map and image files.
- Generates `#pb`, `#terrain`, and `#neighbour` from deterministic geometry.
- Supports optional winter and terrain-variant image sets via policy configuration.
