# Directory Configuration Service

This document describes the services that allow a user to select the map editor
export directory and the Dominions map input directory.

## Components
- **DirectoryChooser** – launches a `JFileChooser` restricted to directories. It
  exposes a `chooseDirectory` method returning the selected path or an error in
  the effect channel.
- **DirectoryConfigService** – orchestrates two directory selections (export and
  input) and persists the results to `map-editor-wrap.conf`, creating the file
  when necessary.

## Design Notes
- UI concerns are isolated in `DirectoryChooserImpl`; other services remain free
  of Swing dependencies so the rendering layer can be replaced later.
- All effects follow the `Sequencer`/`ErrorChannel` pattern outlined in the
  architecture guides.

## Testing
- `sbt compile`
