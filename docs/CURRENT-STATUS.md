# Current Status

This is the quick landing page for the FF1 J2ME editor project.

## Project Shape

- The editor targets the Namco Bandai Java ME `Final Fantasy` jar.
- The app is JavaFX-first and follows the neighboring `vddoh-editor` conventions:
  `data` records/enums for snapshots and edits, `service` classes for byte
  discovery/patching, and `view` classes for JavaFX only.
- Selected jars are validated by manifest and extracted through
  `EditorLoadService`.
- Patched jars are written to `%USERPROFILE%\.ff1-editor\dist`.
- Temporary extracted files live under `%USERPROFILE%\.ff1-editor\temp`.
- The input jar is never mutated.

## Implemented Editor Features

- Startup opens a JAR chooser, then loads the selected jar into an
  `EditorWorkspace`.
- Heroes tab:
  - shows the 12 known classes;
  - edits base-class starting HP/STR/AGL/INT/STA/LCK;
  - keeps upgraded classes read-only and mirrors their base-class row values.
- Magic Matrix tab:
  - splits White Magic and Black Magic into sub-tabs;
  - reads spell names from `PACK0_4`;
  - exposes each spell permission mask as class checkboxes;
  - writes masks back to `cp0`.
- Equipment Matrix tab:
  - splits Weapons and Armor into sub-tabs;
  - exposes each equip permission mask as class checkboxes;
  - writes weapon masks back to `cp0` chunk 3 and armor masks back to
    `cp0` chunk 2.
- Items tab:
  - discovery/edit view split into Weapons, Armor, and Items;
  - shows item names/descriptions/prices from `PACK0_3` and `cp0` chunk 0;
  - edits shared item prices and writes them back to `cp0` chunk 0;
  - shows weapon damage/accuracy/cast spell/equip classes from `cp0` chunk 3;
  - edits weapon cast spell ids with a skill dropdown and writes them back to
    `cp0` chunk 3;
  - shows armor subtype/absorb/evasion lower/cast spell/resistance/equip classes from
    `cp0` chunk 2.
- Skills tab:
  - discovery/edit view for all 94 `cp0` chunk 1 spell/effect records;
  - shows spell/effect names from game text where available, raw runtime fields, effect ids,
    permission masks, prices, and known spell/equipment/item invokers;
  - edits price, `power/status`, and `accuracy` fields.
- Command bar:
  - `Build Patched JAR` opens a VDDOH-style global patch modal;
  - the modal contains optional global patches, not tab-local data controls.

## Implemented Global Patches

- Strong level-ups:
  - JDK 25 Class-File API patch in `g.class`;
  - changes six level-up random-roll modulus checks from `% 8` to `% 1`;
  - makes every HP/stat growth roll succeed.
- Universal spell-charge growth:
  - hybrid patch;
  - copies strong mage charge-growth data into all six class growth groups in
    `cp0` chunk 4;
  - changes the `g.class` charge-growth gate from `classId >= 3` to
    `classId >= 0` with the JDK 25 Class-File API.
- 15 max spell charges:
  - hybrid patch;
  - includes the universal `g.class` charge-growth gate from `classId >= 3` to
    `classId >= 0`;
  - changes the `g.class` spell-charge cap from `9` to `15`;
  - rewrites `cp0` chunk 4 charge-growth slots for all class groups so every
    spell level gains one max charge at character levels `3, 6, 9, ..., 45`;
  - changes the `i.class` field recovery helper from `+10` to `+15` charges so
    inns/tents/cottages can refill the larger pool.
- INT-scaled spell damage:
  - bytecode patch in `g.class`;
  - routes spell-effect returns through a Class-File API transform;
  - scales positive enemy-target spell results from player actors by
    `damage + damage * intelligence / 200`;
  - leaves failed effects, sentinel values, party-target healing, monster
    spells, and the `9999` cap unchanged.
- Cornelia sells Masamune:
  - data patch in `cp0`;
  - changes the Cornelia weapon shop Knife slot from item id `9` to item id
    `47`;
  - leaves the input jar untouched and writes the change only to patched output.
- Cornelia sells Excalibur:
  - data patch in `cp0`;
  - changes the Cornelia weapon shop Nunchaku slot from item id `8` to item id
    `46`;
  - compatible with the separate Knife-to-Masamune shop patch.
- Always successful run:
  - bytecode patch in `g.class`;
  - rewrites the Run success helper so normal escapes always succeed;
  - preserves the encounter no-run/boss gate `j.b[e * 15 + 1] == 1`.
- Party action order:
  - bytecode patch in `g.class`;
  - normal battles and first-turn preemptive battles act as party command groups first:
    `Magic -> Item -> Attack -> Run/other`, preserving party slot order inside
    each group;
  - enemy slots act after the party in randomized enemy-only order;
  - first-turn ambush keeps the original queue while enemy control is active; the
    turn after control returns uses the same patched normal order.
- Cottage revives KO:
  - bytecode patch in `i.class`;
  - changes the field recovery helper so Cottage (`recoveryKind == 3`) clears the
    death/KO status bit before applying its normal full HP and spell-charge
    recovery;
  - Sleeping Bag and Tent still skip KO members.
- Airship lands on safe terrain:
  - bytecode patch in `i.class`;
  - changes the world-map airship landing check from stock `0` and `10..14` to
    `0` and `10..33`;
  - rejects water-like low ids and higher blocked/special terrain bytes.

## Confirmed Data Locations

- Class names: `PACK0_1`, offsets documented in `HERO-CLASSES.md`.
- Base starting class templates: `cp0` offset `0x00003f05`, six records,
  10 bytes each. The Heroes tab reads these bytes directly when loading a jar
  and writes the same table when building patched output.
- Growth matrix: `cp0` chunk 4, shape `6 x 49 x 14`.
- Spell metadata: `cp0` chunk 1, 94 records, 13 bytes each.
- Spell names/descriptions: `PACK0_4`, a length-prefixed text table whose first
  id is read from the table header. Spell name text is `firstId + spellId * 2`;
  description text is the following row.
- Learnable spell masks: `chunk1 + 2 + spellId * 13 + 11`, big-endian 16-bit.
- Spell/effect router: `j.a[spellId][1]`; battle code assigns this field to
  `g.j` for learned spells, weapon-cast spells, armor-cast spells, and several
  consumable effects.
- Spell/effect editable fields: source record bytes `0..1`, `5`, and `6` in
  each 13-byte record, loaded as `j.a[id][10]` (`price/cost`), `j.a[id][2]`
  (`power/status`), and `j.a[id][8]` (`accuracy`).
- Shop inventory rows: `cp0` chunks `6..11` via the shop chunk source array
  documented in `SHOP-INVENTORY.md`.
- Weapon records: `cp0` chunk 3, 41 records, 9 bytes each. Runtime weapon item
  ids are offset by 7, so `Knife` is item id `9` and `Masamune` is item id `47`.
  Record bytes `2..3` are editable as the equip class mask. Record byte `6` is
  editable as the battle cast skill id.
- Armor records: `cp0` chunk 2, 41 records, 6 bytes each. Runtime armor item
  ids are offset by 48 and split into body armor, shields, helms, and gloves.
  Record bytes `0..1` are editable as the equip class mask.
- Shared item metadata: `cp0` chunk 0, 106 records, 4 bytes each. The first
  field is the shop price.
- Runtime level-up and spell-charge logic: `g.class`, method `F()`.
- Spell metadata loader: `b.class`, private static method `C()`.
- Spell learn check: `i.class`, private method `l(int spellId, int characterIndex)`.
- Run/escape check: `g.class`, private static method `c(int actor)`. See
  `BATTLE-RUN.md`.
- Encounter table: `cp0` chunk 12, 245 records of 15 bytes. Encounter byte `1`
  is the no-run/boss-style flag used by Run and battle-start advantage logic.
- Random encounter rate: `i.class`, private method `V()`. It rolls
  `random(100) < aI`, where `aI` rises per eligible step, resets to
  `-2 - random(4)` after an encounter, and caps at `15`. Airship skips random
  encounters. Code evidence shows the direct vehicle rate clamp is on
  `j.a.e == 2` (canoe/river-looking state), not `j.a.e == 1` (ship/sea-looking
  state). See `FIELD-MOVEMENT.md`.
- Battle turn queue: `g.class`, private static method `G()`. See
  `BATTLE-ORDER.md`.
- Field recovery for inns/shelters: `i.class`, private static method `l(int)`.
  Inn is `0`, Sleeping Bag is `1`, Tent is `2`, Cottage is `3`. Inn and Cottage
  share the spell-charge recovery amount.
- Airship landing check: `i.class`, private method `L()`. Stock accepts terrain
  `0` and `10..14`; the optional patch accepts terrain `0` and `10..33`.

## Confirmed Game Behavior

- Starting HP is read as a signed byte. Setting HP to `200` displayed `-56/-56`.
- Starting HP is therefore capped to `0..127` in the normal Heroes UI.
- STR/AGL/INT/STA/LCK display cleanly at `99`; the editor caps them at `0..99`.
- Class upgrade does not load separate starting stats. Upgraded classes inherit
  live character stats. In the Heroes tab, upgraded rows mirror the editable
  base row to make that inheritance visible.
- Red Mage, White Mage, and Black Mage begin with `LV1 2/2` charges.
- Warrior, Thief, and Monk begin with no charges.
- White Wizard has all White Magic permissions and no Black Magic permissions.
- Black Wizard has all Black Magic permissions and no White Magic permissions.
- Red Wizard has broader access than Red Mage, but Red Mage and Red Wizard are
  distinct permission bits.

## Useful Build Commands

```cmd
build-with-jdk.cmd
build-and-run-ff1-editor-fx.cmd
build-and-catalog.cmd
java -jar target\ff1-data-editor-0.1.0.jar items ff1-jar
```

Use `build-with-jdk.cmd` for quick compile verification after normal code edits.

## Next Good Targets

- Decode spell names and observable spell stats for a future `Magic` tab.
- In-game check the implemented Cornelia weapon-shop replacement patch
  `cp0[0x1a57] = 0x2f` for `Knife -> Masamune`.
- In-game check the implemented Cornelia weapon-shop replacement patch
  `cp0[0x1a56] = 0x2e` for `Nunchaku -> Excalibur`.
- Decode the remaining unknown item, weapon, and armor fields.
- Decode monster records.
- Investigate an optional unsigned/wider starting-HP engine patch.
- Add focused tests around patch detection and byte replacement once layouts
  settle.
