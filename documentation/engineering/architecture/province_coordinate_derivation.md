# Province Coordinate Derivation

This page outlines a streaming algorithm for computing an integral `(x, y)` location for every province.
It replaces assumptions that province identifiers imply map coordinates and instead uses the `#pb`
directive as the authoritative source of pixel ownership.

## Background

The map file lists `#pb` directives after the province data. Each entry encodes a horizontal run of
pixels belonging to a province:

```
#pb <x> <y> <len> <province id>
```

The directive is documented in the Dominions manual's [Basic Map Commands section]
(../../domain/dominions/manual/sections/6_map_file_commands/6.3_basic_map_commands.md#pb-x-y-len-province-nbr).
Maps commonly contain more than 16,000 of these lines, so any algorithm must process them in a
streaming fashion.

## Streaming Aggregation

1. Parse the map header to obtain pixel dimensions.
2. Stream through the `#pb` lines in file order. For each `#pb x y len province` run:
   - `count[p] += len`
   - `sumX[p] += len * x + (len - 1) * len / 2`
   - `sumY[p] += len * y`
   - Optionally track `minX/maxX` and `minY/maxY` for diagnostics.
   The map only maintains these small per‑province accumulators, allowing the scan to scale to
   large files.

## Wrap Handling

Horizontal or vertical wraps can leave stray pixels from severed provinces on both edges of the
image. To avoid misplacing such provinces, treat coordinates as circular values on both axes
regardless of the map's declared wrap mode:

- For each province maintain `sumCos` and `sumSin` for both axes using the run's centre point.
  After the scan compute angular means with `atan2(sumSin, sumCos)` and convert back to pixel
  coordinates. This yields centroids that are unaffected by fragments appearing on opposite edges.
- Runs shorter than a small threshold near the boundary may be ignored if necessary to further
  reduce noise.

## Coordinate Projection

After processing all runs, compute the centroid for each province using the angular means and
convert the angles back to pixel coordinates:

```
angleX = atan2(sumSinX[p], sumCosX[p])
angleY = atan2(sumSinY[p], sumCosY[p])
centroidX = width * normalize(angleX) / (2π)
centroidY = height * normalize(angleY) / (2π)
```

where `normalize` adjusts the angle into the range `[0, 2π)`.

Convert the centroid from pixels to the province mesh by dividing by the pixel dimensions of a
single province (256×160 in the default engine):

```
xCell = round(centroidX / 256)
yCell = round(centroidY / 160)
```

The result is an integral `(xCell, yCell)` identifying the province's location on the mesh.
`ProvinceLocationService` will expose this mapping to the rest of the system.

## Summary

- The `#pb` directive defines province pixels and supersedes any numeric ordering of province ids.
- A single pass over the directives with per‑province accumulators yields stable centroids.
- Circular averaging neutralises artifacts from horizontal or vertical wrapping.
- Converting centroids to cell indices provides integral coordinates suitable for map services.
