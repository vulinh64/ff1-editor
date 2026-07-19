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
- Magic Matrix tab edits class permission masks for learnable spells and reads
  spell names from `PACK0_4`.
- Equipment Matrix tab edits weapon and armor equip permission masks.
- Items tab shows decoded item/equipment names, descriptions, prices,
  equipment stats, equip masks, and cast spell ids. Shared item prices, weapon
  damage/accuracy/cast spell ids, and armor absorb/evasion lower are editable.
  Key/quest items are hidden from the Items sub-tab.
- Skills tab shows all 94 spell/effect records and edits price,
  `power/status`, and `accuracy`.
- The command bar `Build Patched JAR` button opens a VDDOH-style modal for
  optional global patches.
- Implemented global patches:
  - force strong level-ups via `g.class`;
  - universal spell-charge growth via `cp0` chunk 4 plus `g.class`;
  - 15 max spell charges via `cp0`, `g.class`, and `i.class`;
  - damage-causing spells scale with INT via `g.class`;
  - healing spells scale with INT via `g.class`;
  - Cornelia sells Masamune and Cornelia sells Excalibur via `cp0`;
  - Cornelia armor shop sells Ribbon and Protect Ring via `cp0`;
  - always-successful Run, party action order, and enemy crit defense behavior
    via `g.class`;
  - Cottage revives KO and airship lands on safe terrain via `i.class`.

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
- Spell/effect names are decoded from `PACK0_4`; do not hardcode spell label
  arrays in the editor.
- Item/equipment names and descriptions are decoded from `PACK0_3`; the table's
  first text id is read from the data file header.
- Weapon records: `cp0` chunk 3, 41 records, 9 bytes each. Record bytes `2..3`
  are the equip class mask, bytes `4..5` are damage/accuracy, and byte `6` is
  the battle cast skill id.
- Armor records: `cp0` chunk 2, 41 records, 6 bytes each. Record bytes `0..1`
  are the equip class mask and bytes `2..3` are absorb/evasion lower.
- Cornelia armor shop row: `cp0` chunk 8 row 0 has two empty slots after
  Clothes, Leather Armor, and Chain Mail; those can be filled with Ribbon item
  id `80` and Protect Ring item id `88`.
- Shared item metadata: `cp0` chunk 0, 106 records, 4 bytes each. The first
  field is the shop price.

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

- Decode remaining unknown item, weapon, armor, and monster fields.
- Investigate spell/name text editing beyond the current read-only decoded
  labels.
- Investigate an optional unsigned/wider starting-HP engine patch.
- Add focused tests around patch-state detection and JAR entry replacement.
