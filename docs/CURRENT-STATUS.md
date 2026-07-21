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
- Magic Permissions tab:
  - splits White Magic and Black Magic into sub-tabs;
  - reads spell names and descriptions from `PACK0_4`;
  - exposes each spell permission mask as an editable class mask;
  - writes masks back to `cp0`.
- Equipment / Items tab:
  - discovery/edit view split into Weapons, Armor, and Items;
  - shows item names/descriptions/prices from `PACK0_3` and `cp0` chunk 0;
  - edits shared item prices and writes them back to `cp0` chunk 0;
  - shows weapon damage/accuracy/cast spell/affinity/family/equip classes from `cp0` chunk 3;
  - edits weapon damage, accuracy, cast spell ids, affinity masks, family masks,
    and equip class masks,
    writing them back to `cp0` chunk 3;
  - shows read-only weapon effectiveness labels derived from skill names,
    weapon descriptions, and the raw special masks;
  - shows armor subtype/absorb/evasion lower/cast spell/resistance/equip classes from
    `cp0` chunk 2.
  - edits armor absorb, evasion lower, resistance masks, and equip class masks,
    writing them back to `cp0` chunk 2.
  - hides key/quest items from the Items sub-tab because those are not normal
    shop or inventory-balance data.
- Monsters tab:
  - discovery/edit view split into Normal and Bosses / Fixed;
  - reads monster names from `PACK0_14` and monster records from `cp0` chunk 15;
  - classifies Bosses / Fixed by whether the monster appears in any `cp0` chunk
    12 encounter row whose no-run/boss-style flag byte is `1`;
  - edits confirmed monster EXP, Gil, HP, attack, hit count, defense, evasion,
    magic defense, Archetypes, Weaknesses, and Resists fields, writing them back
    to `cp0` chunk 15;
  - limits Archetypes selection to three checked families per monster;
  - allows any number of Weakness and Resist selections, including no weaknesses
    or all resistances, while preventing the same element from being both a
    weakness and a resistance;
  - keeps source offsets and raw leading bytes read-only for ongoing decoding.
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
- Damage-causing spells scale with INT:
  - bytecode patch in `g.class`;
  - routes spell-effect returns through a Class-File API transform;
  - scales positive enemy-target spell damage results from player actors by
    `damage + damage * intelligence / 200`;
  - leaves failed effects, sentinel values, party-target healing, monster
    spells, and the `9999` cap unchanged.
- Healing spells scale with INT:
  - bytecode patch in `g.class`;
  - routes spell-effect returns through a Class-File API transform;
  - scales negative party-target effect kind `7` Cure/Heal-style restoration
    from player actors by `heal + heal * intelligence / 200`;
  - leaves Curaja/full-heal kind `15`, Life/Full-Life revival/status recovery
    kind `8`, failed effects, monster spells, and damage results unchanged.
- INT+STA reduce enemy spell effects:
  - bytecode patch in `g.class`;
  - reduces enemy-cast damage kind `1` against hero targets by up to `30%`;
  - reduces positive enemy-cast normal status/effect success chances against
    hero targets by up to `20%`;
  - uses `min(200, hero.INT + hero.STA + 2)` as the resistance stat;
  - affects kinds `1`, `3`, `4`, `5`, and `17`;
  - leaves player casts, healing/buffs, Dia-style kind `2`, conditional-status
    kind `18`, physical attacks, and physical status-on-hit unchanged.
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
- Cornelia armor shop sells Ribbon and Protect Ring:
  - data patch in `cp0`;
  - fills the two empty Cornelia armor-shop slots with item ids `80` and `88`;
  - uses the armor-shop path because item shops skip armor-specific preview and
    equipment handling.
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
- Enemy crits respect party defense:
  - bytecode patch in `g.class`;
  - changes only enemy critical hits against party members so the critical bonus
    doubles post-defense damage instead of adding the raw pre-defense attack roll;
  - leaves party critical hits against enemies unchanged.
- Weapon affinity damage bonus:
  - bytecode patch in `g.class`;
  - changes hero weapon-special matches from stock `+4` attack / `+40` hit
    chance to `weapon damage / 2` added attack and `255` hit chance;
  - applies the added attack before defense, random damage rolls, and critical
    handling, while keeping the one-time no-stacking affinity rule.
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
  Record bytes `2..3` are editable as the equip class mask. Record bytes `4`
  and `5` are editable as damage and accuracy. Record byte `6` is editable as
  the battle cast skill id. Record bytes `7` and `8` are editable as the weapon
  affinity and family/type masks.
- Armor records: `cp0` chunk 2, 41 records, 6 bytes each. Runtime armor item
  ids are offset by 48 and split into body armor, shields, helms, and gloves.
  Record bytes `0..1` are editable as the equip class mask. Record bytes `2`
  and `3` are editable as absorb and evasion lower. Record byte `5` is an armor
  resistance mask; battle setup ORs the four equipped armor masks into the hero's
  spell/status resistance field.
- Shared item metadata: `cp0` chunk 0, 106 records, 4 bytes each. The first
  field is the shop price.
- Runtime level-up and spell-charge logic: `g.class`, method `F()`.
- Spell metadata loader: `b.class`, private static method `C()`.
- Spell learn check: `i.class`, private method `l(int spellId, int characterIndex)`.
- Run/escape check: `g.class`, private static method `c(int actor)`. See
  `BATTLE-RUN.md`.
- Encounter table: `cp0` chunk 12, 245 records of 15 bytes. Encounter byte `1`
  is the no-run/boss-style flag used by Run and battle-start advantage logic.
- Monster table: `cp0` chunk 15, 128 records of 25 bytes. Names are decoded
  from `PACK0_14`, where local text ids correspond to monster ids.
- Monster record bytes `4..5` are total EXP award and bytes `6..7` are Gil,
  both big-endian unsigned 16-bit values. In-game EXP is divided among living
  party members, while Gil is awarded as the full battle total.
- Monster record bytes `8..9`, `12`, `13`, `14`, `16`, `20`, `21`, `22`, and
  `23` are editable HP, defense, evasion, hit count, attack, archetype mask,
  magic defense, weakness mask, and resistance mask respectively. The editor
  caps archetype masks to at most three set bits; weakness and resistance masks
  have no selection cap but may not overlap.
- Monster record byte `10` is loaded into runtime monster field `8` and used as
  enemy flee morale. Enemy AI chooses flee when
  `morale - leaderLevel * 2 + random(50) < 80`, unless the encounter no-run flag
  is set or morale is `255`. Fear-style effect kind `5` lowers this same runtime
  morale field by spell/effect power.
- Random encounter rate: `i.class`, private method `V()`. It rolls
  `random(100) < aI`, where `aI` rises per eligible step, resets to
  `-2 - random(4)` after an encounter, and caps at `15`. Airship skips random
  encounters. Code evidence shows the direct vehicle rate clamp is on
  `j.a.e == 2` (canoe/river-looking state), not `j.a.e == 1` (ship/sea-looking
  state). See `FIELD-MOVEMENT.md`.
- Battle turn queue: `g.class`, private static method `G()`. See
  `BATTLE-ORDER.md`.
- Luck is used in vanilla battle logic for Run escape odds and battle-start
  preemptive/ambush odds. See `BATTLE-RUN.md`.
- Physical damage and critical hits: `g.class`, private static method
  `a(boolean, boolean, int, int)`. See `BATTLE-PHYSICAL.md`.
- Weapon special effectiveness: the same physical attack helper checks weapon
  special byte `7` against monster elemental weakness mask `g[target][20]` and
  weapon special byte `8` against monster family/type mask `g[target][18]`.
  Matching either side grants one `+4` attack / `+40` hit-chance bonus, without
  stacking multiple elemental/family bonuses. Confirmed examples include Flame
  Sword `0x10,0x88`, Ice Brand `0x20,0x00`, Wyrmkiller `0x00,0x02`, Sun Blade
  `0x00,0x08`, Coral Sword `0x00,0x20`, and Excalibur `0xff,0xff`.
  The optional weapon affinity damage patch replaces that flat stock bonus with
  `weapon damage / 2` added attack plus `255` hit chance.
- Monster family/type bit `0x80` is regenerative. The local battle code checks
  `g[target][18] & 0x80` during end-of-round HP ticks and applies a `maxHp / 20`
  recovery result.
- Field recovery for inns/shelters: `i.class`, private static method `l(int)`.
  Inn is `0`, Sleeping Bag is `1`, Tent is `2`, Cottage is `3`. Inn and Cottage
  share the spell-charge recovery amount.
- Airship landing check: `i.class`, private method `L()`. Stock accepts terrain
  `0` and `10..14`; the optional patch accepts terrain `0` and `10..33`.
- Armor resistance aggregation: `j.class`, method `g(hero)`, ORs byte `5` from
  body, shield, helm, and gloves. `g.class` spell/effect logic halves matching
  damage, heavily lowers matching status chance, and hard-blocks matching
  conditional status. Physical on-hit statuses also consult this mask.

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
- Cornelia weapon-shop replacement patches are confirmed in-game:
  `cp0[0x1a56] = 0x2e` updates `Nunchaku -> Excalibur`, and
  `cp0[0x1a57] = 0x2f` updates `Knife -> Masamune`.
- Cornelia armor-shop fill patch is confirmed in-game:
  `cp0[0x1a79..0x1a7a] = 0x50,0x58` adds Ribbon and Protect Ring to the
  Cornelia town armor shop display and purchase result.

## Useful Build Commands

```cmd
build-with-jdk.cmd
build-and-run-ff1-editor-fx.cmd
build-and-catalog.cmd
java -jar target\ff1-data-editor-0.1.0.jar items ff1-jar
```

Use `build-with-jdk.cmd` for quick compile verification after normal code edits.

## Next Good Targets

- Decode the remaining unknown item, weapon, armor, spell/effect, and monster
  fields.
- Investigate spell/name text editing beyond the current read-only decoded
  labels.
- Design optional INT-scaling follow-up patches for Haste, Temper, Saber, and
  Dia-like undead-damage spells. Keep these separate from the current
  damage-only and healing-only patches unless the patch behavior is
  intentionally renamed.
- Continue decoding monster record fields beyond the currently exposed combat,
  type, weakness, resistance, and raw leading bytes.
- Investigate an optional unsigned/wider starting-HP engine patch.
- Add focused tests around patch detection and byte replacement once layouts
  settle.
