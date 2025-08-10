# Map Editor Processing Pipeline

This document captures the initial plan for processing map-editor directories and `.map` files in a modular, effect-safe manner.

## Plan
1. **Identify the most recent map-editor directory**
   - Implement a `LatestEditorFinder` capability that lists subfolders and returns the one with the newest modification timestamp.
   - Contract sketch:
     ```scala
     trait LatestEditorFinder[Sequencer[_]]:
       def mostRecentFolder[ErrorChannel[_]](
         root: fs2.io.file.Path
       )(using errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel],
             traceId: model.trace.Id
       ): Sequencer[ErrorChannel[fs2.io.file.Path]]
     ```
2. **Copy the directory while extracting the `.map` file**
   - Create a `MapEditorCopier` capability that:
     - Streams files from the source directory to the destination.
     - Skips the `.map` file and returns its contents as an effectful value.
   - Contract sketch:
     ```scala
     trait MapEditorCopier[Sequencer[_]]:
       def copyWithoutMap[ErrorChannel[_]](
         source: fs2.io.file.Path,
         dest: fs2.io.file.Path
       )(using Files[Sequencer],
             errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel],
             traceId: model.trace.Id
       ): Sequencer[ErrorChannel[Stream[Sequencer, Byte]]]
     ```
3. **Apply map-directive transformation**
   - Introduce `MapDirectiveTransformer` that accepts a directive stream and an FS2 `Pipe` to modify directives.
   - Contract sketch:
     ```scala
     trait MapDirectiveTransformer[Sequencer[_]]:
       def transform[ErrorChannel[_]](
         directives: Stream[Sequencer, MapDirective],
         pipe: Pipe[Sequencer, MapDirective, MapDirective]
       )(using errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel],
             traceId: model.trace.Id
       ): Sequencer[ErrorChannel[Vector[MapDirective]]]
     ```
4. **Render and persist the updated `.map` file**
   - Build a `MapWriter` capability that renders directives back to text and writes the file to the output directory.
   - Contract sketch:
     ```scala
     trait MapWriter[Sequencer[_]]:
       def write[ErrorChannel[_]](
         directives: Vector[MapDirective],
         output: fs2.io.file.Path
       )(using Files[Sequencer],
             errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel],
             traceId: model.trace.Id
       ): Sequencer[ErrorChannel[Unit]]
     ```
5. **Compose a higher-level service**
   - Assemble the above capabilities into an orchestrating `MapProcessingService` that:
     1. Finds the latest editor folder.
     2. Copies contents, extracting the map.
     3. Applies the directive filter.
     4. Writes the modified `.map` file alongside the copied assets.

## Milestones
1. **Scaffolding**
   - Introduce capability traits and stubs for `LatestEditorFinder`, `MapEditorCopier`, `MapDirectiveTransformer`, and `MapWriter`.
2. **Filesystem operations**
   - Implement directory discovery and copying; add unit tests that operate on temporary directories.
3. **Directive transformation**
   - Implement parsing and transformation using FS2 pipes; include tests with sample `.map` files.
4. **Output generation**
   - Implement rendering/writing logic and integration tests covering the full pipeline.
5. **Documentation & examples**
   - Update architecture docs with links to new services and provide an example module demonstrating end-to-end usage.
   - Example: [MapEditorWrapApp](../../../apps/src/main/scala/com/crib/bills/dom6maps/apps/MapEditorWrapApp.scala) selects the
     most recent map-editor output directory, copies it to the game's map folder under a matching name, and severs the map to
     horizontal wrap.

## Service Contract Boundaries
- Each service is a small, single-responsibility trait following the capability pattern and accepting abstract `Sequencer` and `ErrorChannel` type parameters.
- Implementations should provide concrete filesystem and parsing behavior while respecting the project's error-handling guidelines.
- A composite `MapProcessingService` orchestrates the overall workflow by delegating to these capabilities, preserving modularity and SOLID design.

## Configuration
- [Directory Configuration Service](../directory_configuration_service.md) captures user-selected directories and persists them to `map-editor-wrap.conf`.

## Testing
- `sbt compile`
