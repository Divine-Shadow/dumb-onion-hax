# Map State

`MapState` captures high level map metadata derived from parsed directives while omitting heavy payloads such as province pixel data.

## Fields
- `size` – square dimension in provinces.
- `adjacency` – placeholder collection of province connections.
- `borders` – connections carrying a `BorderFlag` from `#neighbourspec`.
- `wrap` – horizontal and/or vertical wrapping behaviour.
- `title` and `description` – human readable metadata.
- `allowedPlayers` and `startingPositions` – player nation and starting province pairs.
- `terrains` – terrain masks for provinces.
- `gates` – special province links.

The companion object provides `fromDirectives` which folds a stream of `MapDirective` values to populate these fields.

`MapDirectiveCodecs` supplies the inverse operation by encoding `MapState` and its components back into canonical `MapDirective` values.
