# Final Fantasy 1 J2ME Editor Head Start

This repository targets the Namco Bandai Java ME release of `Final Fantasy`.
The current local fixture is `ff1.jar`, whose manifest identifies:

```text
MIDlet-Name: Final Fantasy
MIDlet-Version: 1.0.0
MIDlet-Vendor: Namco Bandai
MIDlet-1: Final Fantasy,/i.png,FinalFantasy
MicroEdition-Profile: MIDP-2.0
MicroEdition-Configuration: CLDC-1.1
```

## Borrowed Direction From `vddoh-editor`

The useful pattern from the neighboring editor is:

- use a tiny launcher and keep the JavaFX application in `view`;
- keep one shared `EditorWorkspace` object for the selected jar, extracted/cataloged data,
  and next output jar;
- keep game data offsets in small service classes, not buried in UI code;
- document every confirmed byte range before exposing it as writable;
- make the editor build a patched copy of the jar rather than mutating the input;
- separate data-resource patching from bytecode patching;
- treat obfuscated class behavior as evidence, but prefer raw resource offsets for round-trip editing.

For FF1, the first milestone is resource discovery rather than editing. This jar has
short obfuscated class names plus many extensionless binary resources (`a0`, `m0`,
`mg0`, `PACK0_0`, etc.), so the initial tool catalogs the archive and groups pack
resources before any writes are attempted.

## Mechanics Baseline

General Final Fantasy I mechanics are well documented: classes/jobs drive stat
growth, level ups increase HP and class-dependent stats, equipment controls attack
and defense, and magic is organized by levels. Use those facts as labels and
hypotheses only.

This Java ME port may store, compress, or tune data differently. An editor field is
considered safe only after one of these is true:

- a raw byte range is changed and confirmed to round-trip after reloading the jar;
- decompiled or bytecode behavior confirms how the bytes are parsed;
- an in-game test confirms the changed value affects the expected mechanic.

## Current Tooling

Build and catalog:

```cmd
build-and-catalog.cmd
```

or directly:

```cmd
mvn -q -DskipTests package
java -jar target\ff1-data-editor-0.1.0.jar ff1.jar --catalog target\ff1-catalog.md
```

The generated catalog is intentionally boring but useful: manifest fields, entry
counts, class/resource lists, `PACKn_m` groups, likely binary data resources, and
size fingerprints. This gives the next reverse-engineering pass stable names to
refer to.

Useful discovery dumps:

```cmd
java -jar target\ff1-data-editor-0.1.0.jar heroes ff1-jar
java -jar target\ff1-data-editor-0.1.0.jar items ff1-jar
```

Launch the first JavaFX shell:

```cmd
build-and-run-ff1-editor-fx.cmd
```

The packaged main method now opens the JavaFX editor by default when no
arguments are supplied. Reverse-engineering CLI commands remain available as
explicit developer commands.

On startup, the JavaFX app follows the VDDOH editor flow: it opens a JAR chooser
first, exits if no file is selected, and then loads the chosen file through
`EditorLoadService`. That keeps manifest validation and workspace extraction in
one place.

The JavaFX app currently follows the VDDOH split between `view`, `data`, and
`service`. Global engine/data patch options are deliberately kept out of the
data tabs: the command bar has a single `Build Patched JAR` action that opens a
VDDOH-style modal for optional global patches, then builds the patched output.

The Heroes tab allows HP/STR/AGL/INT/STA/LCK values to be edited for base
classes and builds a patched JAR by replacing `cp0`. The confirmed table is
byte-backed and reads HP as a signed byte, so the safe editor range is `0..127`.
STR/AGL/INT/STA/LCK use FF1's visible stat cap of `0..99`. Upgraded class rows
are read-only because class change inherits live character stats.

Future optional engine patch: change the game's starting-HP read path to treat
the `cp0` HP byte as unsigned. Keep this separate from normal data editing.

The Magic Permissions tab exposes spell permission masks. It is split into
White Magic and Black Magic sub-tabs. Each row is a spell with a compact class
mask editor plus the decoded in-game description, and the patched jar writes the
resulting 16-bit mask back into `cp0`. Spell labels and descriptions are decoded
from `PACK0_4`; do not hardcode spell-name arrays in the editor.

The Equipment / Items tab is a discovery/edit surface for item/equipment data.
It is split into Weapons, Armor, and Items tables and currently exposes names,
descriptions, prices, equipment class masks, damage/accuracy, absorb/evasion
lower, cast-on-use spell ids, affinity/family masks, resistance masks, and
source offsets.
Shared item prices are editable as unsigned 16-bit values. Weapon damage,
accuracy, cast-on-use skill ids, affinity masks, family masks, and equip class
masks are editable; armor absorb, evasion lower, resistance masks, and equip
class masks are editable.
Weapon cast spell labels come from decoded skill/spell data. Key/quest items are
hidden from the Items sub-tab because they are not normal balance data. Keep
other item/equipment bytes read-only until the remaining unknown bytes are named
or bounded.

The Skills tab exposes all 94 spell/effect records from `cp0` chunk `1`.
Spell/effect labels come from decoded game text where available, with consumable
effect labels derived from item names. Price, `power/status`, and `accuracy` are
editable; the remaining raw fields stay read-only until their behavior is fully
named.

The Monsters tab is a discovery/edit surface split into Normal and Bosses /
Fixed. Monster names come from `PACK0_14`; records come from `cp0` chunk `15`.
The Bosses / Fixed split is derived from encounter data, not a confirmed
monster-stat flag: a monster appears there when any `cp0` chunk `12` encounter
row containing that monster has no-run/boss-style byte `1`. Confirmed editable
fields are EXP, Gil, HP, attack, hit count, defense, evasion, magic defense,
Archetypes, Weaknesses, and Resists. Archetypes edit record byte `20` and are
limited to three selected families per monster. Weaknesses edit byte `22` and
Resists edit byte `23`; neither has a selection cap, but the same bit cannot be
selected in both masks. Source offsets and raw leading bytes remain read-only.
Confirmed monster reward fields: record bytes `4..5` are total EXP award and
bytes `6..7` are Gil. EXP is divided among living party members in-game.

The current global patch modal supports:

- forced strong level-ups through a `g.class` bytecode patch;
- universal spell-charge growth through a hybrid `cp0` growth-data patch plus a
  `g.class` class-gate patch.
- 15 max spell charges through a hybrid `cp0` growth schedule, `g.class` cap/gate
  patch, and `i.class` recovery patch.
- an always-successful Run patch in `g.class` that preserves the encounter
  no-run/boss gate.
- a party action-order patch in `g.class` that changes only normal battle queue
  creation so party commands resolve before randomized enemy actions.
- an enemy critical-defense patch in `g.class` that changes only enemy critical
  hits against party members to double post-defense damage instead of adding the
  raw pre-defense attack roll.
- a weapon affinity damage patch in `g.class` that changes matching weapon
  special bonuses from stock flat attack/hit bonuses to half weapon damage added
  to attack plus clamped hit chance.
- a Cottage revive patch in `i.class` that lets Cottage revive KO members while
  preserving Sleeping Bag/Tent behavior.
- an airship landing patch in `i.class` that expands landing to safe world-map
  land terrain while rejecting water-like and higher blocked/special terrain
  bytes.

Global patch options are checked for already-patched/unsupported states
when a jar is loaded.

## JavaFX Direction

This project should follow the same migration shape as `vddoh-editor`:

```text
com.ff1.editor.FinalFantasyDataEditor      JavaFX-first launcher plus CLI tools
com.ff1.editor.view.FxEditorApplication    stage, tabs, command bar, status
com.ff1.editor.view.*                      table/detail views
com.ff1.editor.data.*                      immutable snapshots and edit records
com.ff1.editor.service.EditorLoadService   validate/load/catalog selected jar
com.ff1.editor.service.EditorPatchService  build patched output jar
com.ff1.editor.service.*Offsets            confirmed byte maps only
```

Do not build a one-off Swing or CLI-only editor. The current CLI catalog command
exists to accelerate reverse engineering and can later become a hidden developer
tool behind the JavaFX app.

## Near-Term Editor Milestones

1. Finish decoding unknown item, weapon, armor, spell/effect, and monster
   fields.
2. Add or widen editable fields only after a confirmed byte layout exists.
3. Investigate spell/name text editing beyond the current decoded read-only
   labels.
4. Optional engine patch: make starting HP read unsigned/wider so values above
   `127` do not display as negative.
5. Keep patched output jars in `%USERPROFILE%\.ff1-editor\dist` and temporary
   extraction/workspace files in `%USERPROFILE%\.ff1-editor\temp`.

## Notes From Initial `javap`

`FinalFantasy.class` is the MIDlet entry point and creates the display/game canvas.
Class `i` is very large and likely contains major game-state/menu/battle behavior.
Class `b` has many static arrays and rendering/update methods and is a good early
candidate for menu or shared data behavior. These names are hypotheses until a
method ledger confirms roles.
