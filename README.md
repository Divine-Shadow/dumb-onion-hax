# dumb-onion-hax
High end hax for dominions 6 maps.

Built on the Typelevel stack with Cats, FS2, Log4cats, and property-based tests via ScalaCheck.

See [documentation/engineering/architecture/project_structure.md](documentation/engineering/architecture/project_structure.md) for the module layout and consult [AGENTS.md](AGENTS.md) before contributing.

Refer to the [Dominions 6 Map Making Manual](documentation/domain/dominions/manual/update/README.md) for domain specifics.

The short term goal of this project is to be able to parse map files and then edit them with things like:
- Sever the adjacency rules between rows 0 and 7
- Place thrones in specific locations
- Make all tiles have 'high magic sites'

## Headless CLI

- Config: edit `map-editor-wrap.conf` to point `source` to `./data/live-games` and `dest` to `./data/generated-maps` (created if missing).
  - Lookup order: `-Ddom6.configPath=/path/to/file` if set; otherwise `./map-editor-wrap.conf`; otherwise `../map-editor-wrap.conf`.
  - If no file is found, a sample is written to `./map-editor-wrap.conf` and the app exits.
- Optional: add throne placements to `throne-override.conf` (see `throne-override.conf` for format).
  - You can also point to a custom overrides file with `-Ddom6.overridesPath=/absolute/or/relative/path/to/overrides.conf`. Set `-Ddom6.ignoreOverrides=true` to skip applying overrides.
 - Run (script): `bash scripts/run-app` (see options below)
 - Run (direct): `sbt "project apps" "runMain com.crib.bills.dom6maps.apps.MapEditorWrapCliApp"`
 - JVM memory (optional):
   - Preferred: set `APP_JAVA_OPTS` for the app's forked JVM, e.g. `APP_JAVA_OPTS="-Xms4G -Xmx4G" sbt "project apps" "runMain com.crib.bills.dom6maps.apps.MapEditorWrapCliApp"`
   - Or pass to sbt/JVM: `sbt -J-Xms4G -J-Xmx4G "project apps" "runMain com.crib.bills.dom6maps.apps.MapEditorWrapCliApp"`
   - Avoid placing raw flags after tasks (e.g., `... run -Xms4G`)—sbt will treat them as commands and error.
 - Wrap options (optional): pass JVM props `-Ddom6.wrap.main=hwrap|vwrap|full|none|duel` and `-Ddom6.wrap.cave=hwrap|vwrap|full|none`.
  - WSL support: if you run under WSL, Windows-style paths like `C:\Users\...` in `map-editor-wrap.conf` and `-Ddom6.overridesPath=...` are automatically converted to `/mnt/c/...`.

Script options
- `--main cli|app|<FQCN>`: choose entrypoint (default `cli`).
- `--jvm-opts "-Xms4G -Xmx4G"`: JVM opts for the forked app JVM.
- `--` then args: pass program args to the main class.

## Dev Kit (Docker)

Use the shared Dev Kit to run this project in an isolated container with proxy/DNS defaults.

- Start: `scripts/devkit -p dumb-onion-hax up`
- Shell: `scripts/devkit -p dumb-onion-hax exec 1 bash`
- Warm caches: `scripts/devkit -p dumb-onion-hax warm`
- Compile: `scripts/devkit -p dumb-onion-hax maintain`
- tmux (2 agents): `scripts/devkit -p dumb-onion-hax tmux-shells 2`

Notes
- Run these from the monorepo root (where `scripts/devkit` lives).
- DNS allowlist is enabled by default; add domains with `scripts/devkit allow <domain>` then `scripts/devkit restart`.

## License

![License: Polyform Noncommercial](https://img.shields.io/badge/license-Polyform%20Noncommercial-blue)

This project is licensed under the [Polyform Noncommercial License 1.0.0](https://polyformproject.org/licenses/noncommercial/1.0.0/).

You may use this project freely for personal, noncommercial purposes.  
Commercial use or distribution requires attribution and explicit permission.
