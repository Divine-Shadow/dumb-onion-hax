# Wrap Selection Service

This document describes the services that allow a user to choose how map wrap
settings are converted.

## Components
- **WrapChoiceService** – displays a Swing dialog with radio buttons for
  `hwrap`, `vwrap`, or `no-wrap`. The dialog also offers a checkbox to enable
  independent wrap selection for the cave layer. It returns the main map
  selection along with an optional cave selection. The service contains only UI
  code so rendering can be replaced later.
- **WrapConversionService** – applies the selected wrap to map directives by
  severing the appropriate neighbour connections. It delegates to
  `WrapSever` for the transformation logic.

## Integration
`MapEditorWrapApp` instantiates `WrapChoiceServiceImpl` to obtain the desired
wraps and delegates to `WrapConversionServiceImpl` to rewrite the directives
before writing the map files.

## Testing
- `sbt "project apps" test`
