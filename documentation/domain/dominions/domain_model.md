# Domain Model Progress

This document tracks data types extracted from the reference tables in the map making manual.

## Enumerations

- `Nation` – all playable nations indexed by the table in the advanced map commands section.
- `TerrainFlag` – bitmask values for terrain types.
- `MagicType` – rare terrain masks representing magic paths.
- `BorderFlag` – bitmask values for province borders.

## Value Classes

- `ProvinceId` – 1-based identifier for provinces in the map file.

## Map Directives

`model.map.MapDirective` and its subclasses represent typed commands found in a `.map` file. They replace primitive arguments with small value classes to reduce errors.

### Province Adjacency

- `Neighbour` – standard connection between two provinces.
- `NeighbourSpec` – connection with a `BorderFlag` describing mountain passes, rivers, impassable borders, or roads.
