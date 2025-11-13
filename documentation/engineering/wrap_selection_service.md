# Wrap Selection Service

This document describes the services that allow a user to choose how map wrap
settings are converted.

Prior to this fix, the cave layer was only processed when a specific cave wrap
was selected. [`MapWrapWorkflow`](../../apps/src/main/scala/com/crib/bills/dom6maps/services/mapeditor/MapWrapWorkflow.scala)
now copies the surface selection to the cave layer by default.

## Components
- **WrapChoiceService** – displays a Swing dialog with radio buttons for
  `hwrap`, `vwrap`, `full-wrap`, `no-wrap`, or `ground-surface duel`.
  Selecting the duel option disables wrap choices and cave-layer selection.
  Otherwise the dialog offers a checkbox to enable independent wrap selection
  for the cave layer. The panel also shows a summary of the current throne
  configuration with an "Override Thrones" checkbox that loads
  `throne-override.conf` when selected. The panel is wrapped in a scroll pane
  sized relative to the current display so the confirmation buttons stay within
  view even when system accessibility settings enlarge the fonts. It returns
  the chosen wraps and final throne configuration as a single
  `MapEditorSettings` value. The service
  contains only UI code so rendering can be replaced later.
- **MagicSiteFlagService** – rewrites every `Terrain` entry in the selected
  layers by adding `TerrainFlag.ManySites` (Dominions "high-magic sites" flag)
  when the corresponding checkbox is enabled. Disabled checkboxes leave the
  original mask untouched so existing high-magic provinces survive subsequent
  runs.
- **WrapConversionService** – applies the selected wrap to `MapState` by
  severing the appropriate neighbour connections. It delegates to
  `WrapSeverService` for the transformation logic. The duel option bypasses this
  service in favour of the [`GroundSurfaceDuelPipe`](ground_surface_duel_service.md).

## Integration
`MapEditorWrapApp` instantiates `WrapChoiceServiceImpl` to obtain the desired
wrap mode, magic-site toggles, and throne overrides. If a wrap is chosen it delegates to
`WrapConversionServiceImpl` to rewrite the `MapState` before writing the map
files. Selecting the duel mode runs the `GroundSurfaceDuelPipe` on both surface
and cave maps instead. The throne settings are always applied prior to wrap
conversion, and the magic-site flags are applied right after throne placement.

## Testing
- `sbt "project apps" test`
