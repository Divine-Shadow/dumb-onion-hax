# Province Location ASCII Grid Service Plan

This plan introduces a service that renders province geometry as an ASCII grid.

## Goals
- Accept an fs2 file and a printer.
- Parse map directives from the file.
- Derive province locations from the geometry.
- Print an ASCII grid with each province identifier placed at its cell.

## Plan
1. **Printer Abstraction**
   - Add `Printer[F[_]]` with a `println` method.
2. **Grid Printing Service**
   - Implement `ProvinceLocationGridPrinter.print` which:
     - Parses directives with `MapFileParser.parseFile`.
     - Builds a `MapState` via `MapState.fromDirectives`.
     - Renders the grid using province locations and map size.
3. **Testing**
   - Define `Arbitrary` generators for map geometry.
   - Property test the renderer to ensure every province appears at the expected coordinates.

## Out of Scope
- Non-square maps or alternative render formats.
- Persisting rendered output.

[Back to Architecture Guide Index](README.md)
