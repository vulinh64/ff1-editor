# AGENTS.md

Guidance for future Codex sessions working in this repository.

## Read First

Start with these docs before changing code:

1. `docs/CURRENT-STATUS.md`
2. `docs/HEADSTART.md`
3. `docs/HERO-CLASSES.md`
4. `docs/HERO-GROWTH.md`
5. `docs/HERO-MAGIC.md`
6. `docs/DECOMPILED_METHOD_LEDGER.md`

The neighboring `G:\REPOSITORY\vddoh-editor` project is the main convention
reference. Consult it for JavaFX layout, patch option dialogs, build-output
flows, Java ME library handling, and service/data/view separation.

## Project Goal

Build a JavaFX editor for the Namco Bandai Java ME port of `Final Fantasy`.
The editor builds patched JAR copies. It must never mutate the selected input
JAR in place.

Default paths:

- patched jars: `%USERPROFILE%\.ff1-editor\dist`
- temporary extracted files: `%USERPROFILE%\.ff1-editor\temp`

## Architecture Rules

- Keep JavaFX code under `com.ff1.editor.view`.
- Keep immutable snapshots, edit requests, enums, and simple data carriers under
  `com.ff1.editor.data`.
- Keep byte discovery, validation, and patching under `com.ff1.editor.service`.
- Prefer records and enums when the shape is stable.
- Keep confirmed offsets in service classes or docs, not scattered through UI.
- Add or expose writable fields only after the raw layout is confirmed.
- Data patches and bytecode patches must remain separate concepts in the UI and
  implementation.

## Current Features

- Startup launches JavaFX and opens a JAR chooser.
- Manifest validation expects the Namco Bandai `Final Fantasy` MIDlet.
- Heroes tab edits base-class starting HP/STR/AGL/INT/STA/LCK from `cp0`.
- Magic Matrix tab edits class permission masks for learnable spells.
- The command bar `Build Patched JAR` button opens a VDDOH-style modal for
  optional global patches.
- Implemented global patches:
  - force strong level-ups via `g.class`;
  - universal spell-charge growth via `cp0` chunk 4 plus `g.class`.

## Important Discoveries

- Starting HP is a signed byte in the current game path. HP `200` displayed as
  `-56/-56`, so normal UI caps starting HP at `0..127`.
- Body stats are capped in the editor at `0..99`.
- Upgraded classes inherit live stats; their starting-stat rows are read-only.
- Growth matrix: `cp0` chunk 4, `6 x 49 x 14`.
- Spell permission masks: `cp0` chunk 1, big-endian 16-bit masks at
  `chunk1 + 2 + spellId * 13 + 11`.
- Class permission mask bits:
  - base classes: bits `0..5`
  - upgraded classes: bits `8..13`
  - all classes: `0x3f3f`
- Spell permissions and spell charges are separate. A class can be permitted to
  learn a spell but still be unable to use it without charges for that level.

## Build And Verification

Use:

```cmd
build-with-jdk.cmd
```

For manual app testing:

```cmd
build-and-run-ff1-editor-fx.cmd
```

For catalog regeneration:

```cmd
build-and-catalog.cmd
```

If Maven dependency resolution is needed and sandbox blocks network access,
request escalation normally.

For decompiler work, previous sessions use CFR at `tools\cfr.jar`. The jar is
ignored; on fresh clones download CFR 0.152 with the command documented in
`tools\README.md`.

## Editing Rules

- Use `apply_patch` for manual file edits.
- Do not rewrite unrelated files or clean generated folders unless asked.
- This repository may appear mostly untracked; do not assume untracked files are
  disposable.
- Keep docs updated when a byte layout, runtime behavior, or in-game test result
  becomes confirmed.
- Prefer small, testable patches over broad refactors.

## Near-Term Work

- Decode spell names and spell observable stats for a future `Magic` tab.
- Decode item, weapon, armor, and monster data.
- Investigate an optional unsigned/wider starting-HP engine patch.
- Add focused tests around patch-state detection and JAR entry replacement.
