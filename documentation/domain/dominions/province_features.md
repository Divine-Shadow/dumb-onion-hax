# Province Feature Commands

This page records how province level features are specified in a Dominions map file and outlines the planned support for them in the codebase.

## Syntax

Province features are assigned by pairing a province selection with a feature identifier:

```text
#setland <province identifier>
#feature <feature identifier>
```

`#setland` selects the province to modify without removing its existing units. `#feature` then adds a feature such as a throne or unique site to that province. For example, `#setland 120` followed by `#feature 1358` adds the *Throne of Winter* to province 120.

## Throne Defenders

Placing a throne with `#feature` only adds the site. Standard throne defenders are not created. The manual notes that changing province properties does not alter independent defenders and that any desired defenders must be assigned manually using [Commander Commands](manual/update/sections/6_map_file_commands/6.8_commander_commands.md) after selecting the province with `#land` or `#setland` ([Province Commands](manual/archive/sections/6_map_file_commands/6.7_province_commands.md)).


## Context

The current map model captures terrain, adjacency, and special links but lacks a way to store feature overrides. Without this, user supplied features cannot be represented or written back to a map file. Feature directives also do not round-trip through the parser and renderer, leaving `#setland` and `#feature` lines as pass-through text.

## Implementation Plan

- Introduce `FeatureId` and `ProvinceFeature` models and store them in `MapState`.
- Extend `MapDirective` with `SetLand` and `Feature` cases; teach the parser and renderer to read and write these directives.
- Add encoding logic that emits a `#setland` followed by a `#feature` for each `ProvinceFeature` in `MapState`.
- Provide a `ProvinceFeatureService` that replaces the `features` collection in `MapState` with supplied overrides.
- Test round-trip parsing and service behaviour to ensure feature directives are preserved.

## Related Pages

- [Domain Model Progress](domain_model.md)
- [Map Making Manual](manual/update/README.md)
