# Final Fantasy 1 J2ME Editor

JavaFX editor and patch builder for the Namco Bandai Java ME release of `Final Fantasy`. The editor opens an original game JAR, discovers known data tables and bytecode patch states, lets selected fields be edited, and builds a new patched JAR copy.

This project is inspired by [vulinh64/vddoh-editor](https://github.com/vulinh64/vddoh-editor).

The selected input JAR is never modified in place. Patched JARs are written to `%USERPROFILE%\.ff1-editor\dist`, and temporary extracted files are written to `%USERPROFILE%\.ff1-editor\temp`.

## Technologies

* Java 25

* JavaFX Controls

* Maven and Maven Wrapper

* Java 25 Class-File API for bytecode patching

* Lombok

* Apache Commons Lang

* SLF4J and Logback

* Maven Shade plugin for the packaged application JAR

* Bundled Java ME API jars under `me-lib` for compiling against CLDC/MIDP types

## How To Run

Build the project:

```cmd
build-with-jdk.cmd
```

Launch the JavaFX editor:

```cmd
build-and-run-ff1-editor-fx.cmd
```

Build and regenerate the JAR catalog:

```cmd
build-and-catalog.cmd
```

After building, the packaged application can also run directly:

```cmd
java -jar target\ff1-data-editor-0.1.0.jar
```

Developer discovery commands:

```cmd
java -jar target\ff1-data-editor-0.1.0.jar ff1.jar --catalog target\ff1-catalog.md
java -jar target\ff1-data-editor-0.1.0.jar heroes ff1-jar
java -jar target\ff1-data-editor-0.1.0.jar items ff1-jar
java -jar target\ff1-data-editor-0.1.0.jar skills ff1-jar
java -jar target\ff1-data-editor-0.1.0.jar dump-int-arrays ff1.jar g
java -jar target\ff1-data-editor-0.1.0.jar dump-text ff1-jar PACK0_4 <boundariesCsv> <startInclusive> [endExclusive]
java -jar target\ff1-data-editor-0.1.0.jar search-text ff1-jar <text> [moreText...]
```

## Functionality

The JavaFX app opens with a JAR chooser and validates that the selected file is the expected Namco Bandai `Final Fantasy` MIDlet. Once loaded, the editor provides these tabs:

* `Heroes`: edits base-class starting HP, STR, AGL, INT, STA, and LCK from `cp0`. Upgraded classes are shown as read-only mirror rows because class change inherits live character stats.

* `Magic Matrix`: edits class permission masks for learnable White and Black Magic. Spell names are decoded from `PACK0_4`.

* `Equipment Matrix`: edits weapon and armor equip permission masks.

* `Skills`: shows all 94 spell/effect records and edits price, `power/status`, and `accuracy`.

* `Items`: shows decoded item/equipment names, descriptions, prices, equipment stats, equip classes, and cast spell ids. Shared item prices, weapon damage/accuracy/cast spell ids, and armor absorb/evasion lower are editable. Key/quest items are hidden.

The `Build Patched JAR` command opens a global patch dialog. These options change how the patched JAR behaves compared with the base game:

* Strong level-ups: every level-up takes the strong HP/stat growth path. HP always receives the strong-growth bonus plus the normal stamina contribution, and STR, AGL, INT, STA, and LCK each gain at least `+1` when leveling. This makes long-term growth much less swingy; by level 20, each body stat has received at least 19 guaranteed level-up points after level 1.

* Universal spell-charge growth: Warrior, Thief, Monk, and their upgraded classes can gain spell charges from the growth table while leveling, instead of spell-charge growth being gated to mage-style classes only. Spell permissions are still separate, so classes still need Magic Matrix access before they can learn and cast spells.

* 15 max spell charges: raises the per-level spell charge cap from `9` to `15`, rewrites charge growth so every class can eventually reach 15 charges per spell level, and updates field recovery so inns/cottages can refill the larger pool.

* Damage-causing spells scale with INT: player damage spells against enemies scale with the acting hero's Intelligence; at `99` INT, affected direct damage is roughly 50% higher. Healing and party-target buffs such as Temper, Saber, and Haste are not intentionally scaled, so this patch does not make high-INT Haste turn double attacks into triple attacks. Missed or resisted spell effects, monster spells, and the `9999` damage cap stay unchanged.

* Healing spells scale with INT: Cure-like and Heal-like spells scale their restored HP with the acting hero's Intelligence; at `99` INT, affected healing is roughly 50% stronger. Curaja's full-heal effect and Life/Full-Life revival effects stay unchanged.

* INT+STA reduce enemy spell effects (experimental): enemy-cast direct spell damage and normal spell/effect success chances against heroes are reduced by the target hero's Intelligence plus Stamina. At the capped resistance value, affected damage is reduced by 30% and affected positive success chances are reduced by 20%. Player-cast spells, healing/buffs, Dia-style undead damage, conditional status effects, physical attacks, and physical status-on-hit stay unchanged.

* Cornelia sells Masamune: replaces Cornelia weapon shop's Knife slot with Masamune.

* Cornelia sells Excalibur: replaces Cornelia weapon shop's Nunchaku slot with Excalibur.

* Cornelia armor shop sells Ribbon and Protect Ring: fills Cornelia armor shop's two empty inventory slots with Ribbon and Protect Ring.

* Always successful Run: normal escape attempts always succeed, while boss/no-run encounters still block running.

* Party action order: changes normal battle turn order so party actions resolve before enemies. Party commands are grouped as `Magic -> Item -> Attack -> Run/other`, preserving party slot order inside each group; enemies act afterward in randomized enemy-only order. Ambush first turns keep the original enemy-control behavior.

* Enemy crits respect party defense: enemy critical hits against party members double the already defense-reduced damage instead of adding the raw pre-defense attack roll. Party critical hits against enemies remain unchanged.

* Cottage revives KO: lets Cottage revive knocked-out party members before applying its normal full HP and spell-charge recovery. Sleeping Bag and Tent still skip KO members.

* Airship lands on safe terrain: expands airship landing from the stock terrain set to additional safe land terrain while still rejecting water-like and blocked/special terrain.

## Project Layout

* `src/main/java/com/ff1/editor/data`: immutable snapshots, edit records, enums, and patch-state data.

* `src/main/java/com/ff1/editor/service`: JAR loading, cataloging, discovery, validation, data patching, and bytecode patching.

* `src/main/java/com/ff1/editor/view`: JavaFX application, tabs, tables, and command bar.

* `src/main/java/com/ff1/editor/utils`: shared byte/JAR helpers.

* `docs`: reverse-engineering notes, confirmed byte layouts, behavior notes, and patch documentation.

## Notes

This project is evidence-driven. Generic FF1 mechanics are useful vocabulary, but confirmed local JAR data, decompiled behavior, bytecode inspection, and in-game tests take precedence.

Some fields remain read-only or conservatively labeled until their raw layout and runtime behavior are confirmed. Starting HP is intentionally capped to `0..127` in the normal UI because this game path reads the starting HP byte as signed.
