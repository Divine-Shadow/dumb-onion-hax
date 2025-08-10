# Wrap Selection Service

This document describes the services that allow a user to choose how map wrap
settings are converted.

## Components
- **WrapChoiceService** – displays a Swing dialog with radio buttons for
  `hwrap`, `vwrap`, or `no-wrap`. It returns the user's selection in the effect
  channel. The service contains only UI code so rendering can be replaced later.
- **WrapConversionService** – applies the selected wrap to map directives by
  severing the appropriate neighbour connections. It delegates to
  `WrapSever` for the transformation logic.

## Integration
`MapEditorWrapApp` instantiates `WrapChoiceServiceImpl` to obtain the desired
wrap and delegates to `WrapConversionServiceImpl` to rewrite the directives
before writing the map file.

## Testing
- `sbt "project apps" test`
