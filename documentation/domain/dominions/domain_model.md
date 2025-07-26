# Domain Model Progress

This document tracks data types extracted from the reference tables in the map making manual.

## Enumerations

- `Nation` – all playable nations indexed by the table in the advanced map commands section.
- `TerrainFlag` – bitmask values for terrain types.
- `MagicType` – rare terrain masks representing magic paths.

## Map Directives

`model.map.MapDirective` and its subclasses represent typed commands found in a `.map` file. They replace primitive arguments with small value classes to reduce errors.
