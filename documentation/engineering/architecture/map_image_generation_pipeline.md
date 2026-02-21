# Map Image Generation Pipeline

[Back to Architecture Index](README.md)

This page describes how map image bytes are generated from map directives.

## Purpose
- Produce a deterministic, playable `.tga` map image directly from map data.
- Keep image generation separate from map text rendering so each concern can evolve independently.

## Data Sources
- `MapLayer.state` for terrain masks and map-level state.
- `MapLayer.passThrough` for pixel ownership runs (`#pb`) and optional `#imagefile` directive.

## Pipeline
1. Resolve output image path.
   - Use `#imagefile` if present.
   - Otherwise default to `<map-file-name>.tga` in the same directory.
2. Resolve image dimensions.
   - Prefer explicit `#mapsize` in pass-through directives.
   - Fallback to `state.size` (`width = size * 256`, `height = size * 160`).
   - Last fallback: infer from `#pb` runs.
3. Rasterize province ownership.
   - Convert `#pb x y len province` runs into a per-pixel province identifier array.
   - Interpret `#pb y` as bottom-origin row coordinates.
4. Paint deterministic terrain image.
   - Fill sea background.
   - Paint province pixels with sea/land color from terrain masks.
   - Draw province borders where neighboring province identifiers differ.
   - Place one white anchor pixel per province.
5. Encode to Targa.
   - Uncompressed, true-color (24-bit) TGA.
   - Bottom-left origin.
6. Write bytes to disk.

## Components
- `model.map.image.MapTerrainPainter`
  - Service contract for mapping ownership + terrain masks to RGB image bytes.
- `model.map.image.PrimaryTerrainColorMapTerrainPainter`
  - Default implementation that assigns deterministic colors by primary terrain type.
- `model.map.image.ConstantColorMapTerrainPainter`
  - Alternate implementation that keeps classic land/sea constant coloring.
- `model.map.image.ProvincePixelRasterizer`
  - Converts `#pb` runs to province ownership pixels.
- `model.map.image.MapImagePainter`
  - Shared low-level paint utility used by terrain painters.
- `model.map.image.TargaImageEncoder`
  - Encodes RGB bytes into `.tga` format.
- `apps.services.mapeditor.MapImageWriter`
  - Orchestrates extraction, painting, encoding, and file writing.

## Integration
- `MapProcessingServiceImpl` accepts an optional `MapImageWriter`.
- Existing map processing remains unchanged when no image writer is supplied.

## Testing Focus
- TGA header/payload correctness.
- `#pb` run rasterization and clipping.
- Painter behavior for borders, sea/land coloring, and anchor pixels.
- End-to-end map processing with optional image output enabled.
- Province identity stability between click-hit geometry and rendered borders.
