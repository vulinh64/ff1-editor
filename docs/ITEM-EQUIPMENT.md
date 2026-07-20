# Item And Equipment

This file records the first confirmed item/equipment layout for the local Namco
Bandai `Final Fantasy` J2ME jar.

## Item Categories

The shop and equipment code treats visible item ids as shared ids across several
categories:

| Range    | Category           | Notes                                      |
|---------:|--------------------|--------------------------------------------|
|   `1..6` | consumables        | Potion, Antidote, Gold Needle, shelters    |
|      `7` | blank weapon slot  | unused or sentinel-looking                 |
|  `8..47` | weapons            | records in `cp0` chunk `3`                 |
|     `48` | blank armor slot   | unused or sentinel-looking                 |
| `49..64` | body armor         | records in `cp0` chunk `2`, index `id-48` |
| `65..73` | shields            | records in `cp0` chunk `2`, index `id-48` |
| `74..80` | helms              | records in `cp0` chunk `2`, index `id-48` |
| `81..88` | gloves/rings       | records in `cp0` chunk `2`, index `id-48` |
| `90..105` | key/special items | metadata names/prices, no equipment record |

Names and descriptions are in `PACK0_3`:

```text
firstTextId = minimum text id read from the PACK0_3 table header
nameTextId = firstTextId + 2 * itemId
descriptionTextId = nameTextId + 1
```

The JavaFX Items tab reads these layouts through `ItemEquipmentDiscoveryService`
and displays the decoded records in three tables: Weapons, Armor, and Items.
Shared item prices are editable as unsigned 16-bit values and write back to
`cp0` chunk `0`. Weapon damage, accuracy, and cast-on-use skill ids are editable
and write back to `cp0` chunk `3`. Weapon effectiveness is shown as a read-only
column derived from skill names, weapon descriptions, and the raw special masks;
unknown bits remain raw instead of guessed. Armor absorb and evasion lower are
editable and write back to `cp0` chunk `2`. Key/quest items are deliberately
hidden from the Items sub-tab. The Equipment Matrix tab edits weapon and armor
class equip masks through Weapons and Armor sub-tabs, then writes those 16-bit
masks back to `cp0` chunks `3` and `2`. The same discovery path can be
smoke-tested with:

```cmd
java -jar target\ff1-data-editor-0.1.0.jar items ff1-jar
```

## Shared Item Metadata

`b.class`, private static method `C()`, loads `cp0` chunk `0` into the shared
item metadata table:

```text
2-byte count + 106 records * 4 bytes
```

Current confirmed record shape:

| Bytes | Runtime Field                       |
|------:|-------------------------------------|
|   0-1 | shop price, big-endian unsigned     |
|     2 | unknown display/category byte       |
|     3 | unknown display/category byte       |

The shop buy/sell UI uses the first field as price. Selling uses half of this
price. Items with price `0` are not normal shop goods, except there are a few
late equipment records with placeholder-looking prices such as `2`.

Confirmed shop-stock consumables:

| ID  | Name           | Price |
|----:|----------------|------:|
|   1 | Potion         |    60 |
|   2 | Antidote       |    75 |
|   3 | Gold Needle    |   800 |
|   4 | Sleeping bag   |    75 |
|   5 | Tent           |   250 |
|   6 | Cottage        |  3000 |
| 104 | Bottled Faerie | 50000 |

## Inn And Shelter Field Recovery

Inn, Sleeping Bag, Tent, and Cottage behavior is hardcoded in `i.class`, private
static method `l(int recoveryKind)`, not in the shared item metadata table:

| `recoveryKind` | Source       | HP recovery | Spell-charge recovery |
|---------------:|--------------|------------:|----------------------:|
|              0 | Inn          |        1000 |                    10 |
|              1 | Sleeping Bag |          30 |                     0 |
|              2 | Tent         |          60 |                     0 |
|              3 | Cottage      |         999 |                    10 |

The stock helper skips heroes with status bit `0x01` (KO/death), so neither Inn
nor Cottage normally revives knocked-out members through this path. The optional
`Cottage revives KO` bytecode patch changes only the Cottage path: if a hero is
KO'd and the `recoveryKind` is `3`, the patch clears bit `0x01` and then lets the
full-recovery path run. Inn, Sleeping Bag, and Tent still skip KO members.

If the `15 max spell charges` patch is also selected, `Cottage revives KO`
preserves the patched field recovery amount of `15` charges instead of reverting
it to the stock `10`. Because Inn and Cottage share the same charge-recovery
local, the `15 max spell charges` recovery patch affects both Inn and Cottage.

## Weapon Records

Weapon records are loaded from `cp0` chunk `3` into `j.c`. The chunk length is
`371` bytes:

```text
2-byte count + 41 weapon records * 9 bytes
weaponItemId = weaponIndex + 7
```

Current confirmed record shape:

| Bytes | Runtime Field                         |
|------:|---------------------------------------|
|     0 | unknown display/type byte             |
|     1 | unknown display/type byte             |
|   2-3 | equip class mask, big-endian unsigned |
|     4 | weapon damage                         |
|     5 | weapon accuracy                       |
|     6 | spell id cast when used, `0` for none |
|     7 | element weakness effectiveness mask   |
|     8 | family/type effectiveness mask        |

Damage and accuracy are confirmed by `j.f(hero)` and `j.e(hero)`:

```text
attack = STR / 2 + weaponDamage
hitRate = AGI + weaponAccuracy
```

Unarmed Monk/Master are special-cased elsewhere and use level-based attack.
Weapon critical chance is not a separate record field; battle code uses the
equipped weapon record index as the critical threshold. Visible weapon item ids
are `weaponIndex + 7`, so Masamune item id `47` uses crit threshold `40`.
The attack roll is `0..200`, so with enough hit rate to cover that threshold,
Masamune crits on `41 / 201` hit attempts, about `20.4%`.

Weapon special bytes `7` and `8` are checked during physical attacks against two
monster masks: byte `7` is checked against the monster elemental weakness mask
`g[target][20]`, and byte `8` is checked against the monster family/type mask
`g[target][18]`. Any match gives a single physical effectiveness bonus:

```text
attack += 4
hitChance += 40
```

Excalibur has both special bytes set to `0xff`, so it matches any bit present in
either monster mask. That makes it broadly effective against monsters with any
elemental weakness or family/type bit, but the bonus does not stack for multiple
simultaneous matches.

Important behavior note: this is not a separate spell-style damage element. It
does not route Flame Sword through the Fire spell formula or Sun Blade through
Dia. The physical helper only grants one flat `+4` attack and `+40` hit-chance
bonus before normal hit/damage rolls.

Confirmed element weakness mask bits, shared with spell `element/status` masks:

| Bit    | Meaning   | Evidence |
|--------|-----------|----------|
| `0x10` | Fire      | Fire/Fira/Firaga spell records use `0x10`; Flame Sword uses this bit. |
| `0x20` | Ice       | Blizzard/Blizzara/Blizzaga spell records use `0x20`; Ice Brand uses this bit. |
| `0x40` | Lightning | Thunder/Thundara/Thundaga spell records use `0x40`; water-family enemies often carry this weakness. |
| `0x80` | Earth     | Quake/Earthquake spell records use `0x80`; no stock weapon except Excalibur targets it directly. |

Confirmed and tentative family/type mask bits:

| Bit    | Meaning                         | Evidence |
|--------|---------------------------------|----------|
| `0x02` | Dragon/reptile                  | Wyrmkiller uses this bit; matching monsters include dragons, wyrms, wyverns, hydras, lizards, snakes, and dinosaurs. |
| `0x04` | Giant/ogre/goblin family        | Great Sword uses this bit and its local description says "effective against giants"; matching monsters include Hill/Ice/Fire Gigas, ogres, Goblin, and Goblin Guard. |
| `0x08` | Undead                          | Sun Blade and Light Axe use this bit; Dia-like damage also gates on `g[target][18] & 0x08`. |
| `0x10` | Werebeast                       | Werebuster uses this bit; matching monsters are Werewolf and Weretiger. The label matches the FF1 wiki enemy-type wording. |
| `0x20` | Aquatic                         | Coral Sword uses this bit; matching monsters include Sahagin, sharks, sea monsters, Water Naga, and Kraken. |
| `0x40` | Spellcaster/magical, tentative  | Rune Blade uses this bit plus `0x01`; matching monsters include Ogre Mage, Evil Eye, Rakshasa, Dark Wizard, Dark Fighter, Lich, and several bosses. |
| `0x01` | Magical/boss-like, still fuzzy  | Rune Blade also uses this bit; matching monsters include spirits, elementals, golems, fiends, and some undead. |
| `0x80` | Regenerative                    | Flame Sword includes this bit in addition to Undead; matching monsters include Werewolf, trolls, vampires, Ogre Mage, Death Eye, and Death Machine. Local battle code also checks `g[target][18] & 0x80` during end-of-round HP ticks and applies a `maxHp / 20` recovery result. |

### Weapon Permissions

The equip mask uses the same class bits as spell permissions:

| Bit | Hex      | Class        |
|----:|---------:|--------------|
|   0 | `0x0001` | Warrior      |
|   1 | `0x0002` | Thief        |
|   2 | `0x0004` | Monk         |
|   3 | `0x0008` | Red Mage     |
|   4 | `0x0010` | White Mage   |
|   5 | `0x0020` | Black Mage   |
|   8 | `0x0100` | Knight       |
|   9 | `0x0200` | Ninja        |
|  10 | `0x0400` | Master       |
|  11 | `0x0800` | Red Wizard   |
|  12 | `0x1000` | White Wizard |
|  13 | `0x2000` | Black Wizard |

| ID | Weapon | Damage | Accuracy | Casts | Allowed Classes |
|---:|--------|-------:|---------:|-------|-----------------|
|  8 | Nunchaku | 12 | 0 | - | Monk, Ninja, Master |
|  9 | Knife | 5 | 10 | - | Warrior, Thief, Red Mage, Black Mage, Knight, Ninja, Red Wizard, Black Wizard |
| 10 | Staff | 6 | 0 | - | Warrior, Monk, Red Mage, White Mage, Black Mage, Knight, Ninja, Master, Red Wizard, White Wizard, Black Wizard |
| 11 | Rapier | 9 | 5 | - | Warrior, Thief, Red Mage, Knight, Ninja, Red Wizard |
| 12 | Hammer | 9 | 0 | - | Warrior, White Mage, Knight, Ninja, White Wizard |
| 13 | Broadsword | 15 | 10 | - | Warrior, Red Mage, Knight, Ninja, Red Wizard |
| 14 | Battle Axe | 16 | 5 | - | Warrior, Knight, Ninja |
| 15 | Scimitar | 10 | 10 | - | Warrior, Thief, Red Mage, Knight, Ninja, Red Wizard |
| 16 | Iron Nunchaku | 16 | 0 | - | Monk, Ninja, Master |
| 17 | Dagger | 7 | 10 | - | Warrior, Thief, Red Mage, Black Mage, Knight, Ninja, Red Wizard, Black Wizard |
| 18 | Crosier | 14 | 0 | - | Warrior, Monk, Knight, Ninja, Master |
| 19 | Saber | 13 | 5 | - | Warrior, Thief, Red Mage, Knight, Ninja, Red Wizard |
| 20 | Longsword | 20 | 10 | - | Warrior, Red Mage, Knight, Ninja, Red Wizard |
| 21 | Great Axe | 22 | 5 | - | Warrior, Knight, Ninja |
| 22 | Falchion | 15 | 10 | - | Warrior, Thief, Red Mage, Knight, Ninja, Red Wizard |
| 23 | Mythril Knife | 10 | 15 | - | Warrior, Thief, Red Mage, Black Mage, Knight, Ninja, Red Wizard, Black Wizard |
| 24 | Mythril Sword | 23 | 15 | - | Warrior, Red Mage, Knight, Ninja, Red Wizard |
| 25 | Mythril Hammer | 12 | 5 | - | Warrior, White Mage, Knight, Ninja, White Wizard |
| 26 | Mythril Axe | 25 | 10 | - | Warrior, Knight, Ninja |
| 27 | Flame Sword | 26 | 20 | - | Warrior, Red Mage, Knight, Ninja, Red Wizard |
| 28 | Ice Brand | 29 | 25 | - | Warrior, Red Mage, Knight, Ninja, Red Wizard |
| 29 | Wyrmkiller | 19 | 15 | - | Warrior, Thief, Red Mage, Knight, Ninja, Red Wizard |
| 30 | Great Sword | 21 | 20 | - | Warrior, Red Mage, Knight, Ninja, Red Wizard |
| 31 | Sun Blade | 32 | 30 | - | Warrior, Red Mage, Knight, Ninja, Red Wizard |
| 32 | Coral Sword | 19 | 15 | - | Warrior, Thief, Red Mage, Knight, Ninja, Red Wizard |
| 33 | Werebuster | 18 | 15 | - | Warrior, Red Mage, Knight, Ninja, Red Wizard |
| 34 | Rune Blade | 18 | 15 | - | Warrior, Thief, Red Mage, Knight, Ninja, Red Wizard |
| 35 | Power Staff | 12 | 0 | - | Warrior, Monk, White Mage, Black Mage, Knight, Ninja, Master, White Wizard, Black Wizard |
| 36 | Light Axe | 28 | 15 | Diara (`10`) | Warrior, Knight, Ninja |
| 37 | Healing Staff | 6 | 0 | Heal (`12`) | White Mage, Ninja, White Wizard |
| 38 | Mage's Staff | 12 | 10 | Fira (`41`) | Black Mage, Ninja, Black Wizard |
| 39 | Defender | 30 | 35 | Blink (`4`) | Knight, Ninja, Red Wizard |
| 40 | Wizard's Staff | 15 | 15 | Confuse (`47`) | Black Wizard |
| 41 | Vorpal Sword | 24 | 25 | - | Knight, Ninja, Red Wizard |
| 42 | Cat Claws | 22 | 35 | - | Knight, Ninja, Red Wizard, Black Wizard |
| 43 | Thor's Hammer | 18 | 15 | Thundara (`43`) | Knight, Ninja, White Wizard |
| 44 | Razer | 22 | 20 | Scourge (`50`) | Knight, Ninja, Red Wizard |
| 45 | Sasuke's Blade | 33 | 35 | - | Ninja |
| 46 | Excalibur | 45 | 35 | - | Knight |
| 47 | Masamune | 56 | 50 | - | All |

The nonzero cast spell ids are confirmed in the battle command path: weapon use
assigns `g.j = j.a[j.c[itemId - 7][5]][1]` before target selection. The editor
writes this one-byte field from the Weapons table `Casts` dropdown.
The editor also writes weapon damage and accuracy from the Weapons table.

Battle execution also confirms that weapon casts use the same spell/effect
helper as learned magic. In stock bytecode, that helper does not read the acting
hero's INT, so weapon-cast damage does not naturally scale with INT. If the
optional `Damage-causing spells scale with INT` patch is selected, positive enemy-target
weapon-cast damage from a player actor does scale with that actor's INT because
the patch keys off the active hero slot `g.C[g.Y]`.

## Armor Records

Armor records are loaded from `cp0` chunk `2` into `j.d`. The chunk length is
`248` bytes:

```text
2-byte count + 41 armor records * 6 bytes
armorItemId = armorIndex + 48
```

Current confirmed record shape:

| Bytes | Runtime Field                         |
|------:|---------------------------------------|
|   0-1 | equip class mask, big-endian unsigned |
|     2 | absorb / defense contribution         |
|     3 | evasion lower / penalty contribution  |
|     4 | spell id cast when used, `0` for none |
|     5 | resistance/special mask               |

Defense and evasion lowering are confirmed by `j.b(hero, slot, armorId)` and
`j.a(hero, slot, armorId)`:

```text
defense = sum equippedArmor[1]
evasion = baseEvasion - sum equippedArmor[2]
```

The table labels this byte as `Evasion Lower` because larger values reduce the
hero's evasion; heavy armor such as Knight's Armor carries a large penalty.
The editor writes absorb and evasion lower from the Armor table.

### Armor Permissions

| ID | Type | Armor | Absorb | Evasion Lower | Casts | Resistance | Allowed Classes |
|---:|------|-------|-------:|--------:|-------|------------|-----------------|
| 49 | Body | Clothes | 1 | 2 | - | - | All |
| 50 | Body | Leather Armor | 4 | 8 | - | - | Warrior, Thief, Monk, Red Mage, Knight, Ninja, Master, Red Wizard |
| 51 | Body | Chain Mail | 15 | 15 | - | - | Warrior, Red Mage, Knight, Ninja, Red Wizard |
| 52 | Body | Iron armor | 24 | 23 | - | - | Warrior, Knight, Ninja |
| 53 | Body | Knight's Armor | 34 | 33 | - | - | Warrior, Knight |
| 54 | Body | Mythril Mail | 18 | 8 | - | - | Warrior, Red Mage, Knight, Ninja, Red Wizard |
| 55 | Body | Flame Mail | 34 | 10 | - | `0x20` | Warrior, Knight, Ninja |
| 56 | Body | Ice Armor | 34 | 10 | - | `0x10` | Warrior, Knight, Ninja |
| 57 | Body | Diamond Armor | 42 | 10 | - | `0x40` | Knight |
| 58 | Body | Dragon Mail | 42 | 10 | - | `0x70` | Knight |
| 59 | Body | Copper Armlet | 4 | 1 | - | - | All |
| 60 | Body | Silver Armlet | 15 | 1 | - | - | All |
| 61 | Body | Ruby Armlet | 24 | 1 | - | - | All |
| 62 | Body | Diamond Armlet | 34 | 1 | - | - | All |
| 63 | Body | White Robe | 24 | 2 | Invisira (`24`) | `0x18` | White Wizard |
| 64 | Body | Black Robe | 24 | 2 | Blizzara (`48`) | `0x14` | Black Wizard |
| 65 | Shield | Leather Shield | 2 | 0 | - | - | Warrior, Knight, Ninja |
| 66 | Shield | Iron Shield | 4 | 0 | - | - | Warrior, Knight, Ninja |
| 67 | Shield | Mythril Shield | 8 | 0 | - | - | Warrior, Knight, Ninja |
| 68 | Shield | Flame Shield | 12 | 0 | - | `0x20` | Warrior, Knight, Ninja |
| 69 | Shield | Ice Shield | 12 | 0 | - | `0x10` | Warrior, Knight, Ninja |
| 70 | Shield | Diamond Shield | 16 | 0 | - | `0x40` | Knight |
| 71 | Shield | Aegis Shield | 16 | 0 | - | `0x02` | Knight |
| 72 | Shield | Buckler | 2 | 0 | - | - | Warrior, Thief, Red Mage, Knight, Ninja, Red Wizard |
| 73 | Shield | Protect Cloak | 8 | 2 | - | - | Warrior, Thief, Red Mage, White Mage, Black Mage, Knight, Ninja, Red Wizard, White Wizard, Black Wizard |
| 74 | Helm | Leather Cap | 1 | 1 | - | - | All |
| 75 | Helm | Helm | 3 | 3 | - | - | Warrior, Knight, Ninja |
| 76 | Helm | Great Helm | 5 | 5 | - | - | Warrior, Knight, Ninja |
| 77 | Helm | Mythril Helm | 6 | 3 | - | - | Warrior, Knight, Ninja |
| 78 | Helm | Diamond Helm | 8 | 3 | - | - | Knight |
| 79 | Helm | Healing Helm | 6 | 3 | Heal (`12`) | - | Knight, Ninja |
| 80 | Helm | Ribbon | 1 | 1 | - | `0xff` | All |
| 81 | Gloves | Leather Gloves | 1 | 1 | - | - | All |
| 82 | Gloves | Bronze Gloves | 2 | 3 | - | - | Warrior, Knight, Ninja |
| 83 | Gloves | Steel Gloves | 4 | 5 | - | - | Warrior, Knight, Ninja |
| 84 | Gloves | Mythril Gloves | 6 | 3 | - | - | Warrior, Knight, Ninja, Red Wizard |
| 85 | Gloves | Gauntlets | 6 | 3 | Thundara (`43`) | - | Knight, Ninja, Red Wizard |
| 86 | Gloves | Giant's Gloves | 6 | 3 | Saber (`59`) | - | Warrior, Knight, Ninja, Red Wizard |
| 87 | Gloves | Diamond Gloves | 8 | 3 | - | - | Knight |
| 88 | Gloves | Protect Ring | 8 | 1 | - | `0x08` | All |

## Shop Tables

See `SHOP-INVENTORY.md` for the shop table loader and Cornelia weapon-shop
patch notes. Current shop category mapping:

| Shop Type | `cp0` Chunk | Current Meaning  |
|----------:|------------:|------------------|
|         0 |           7 | weapon shop      |
|         1 |           8 | armor shop       |
|         2 |           6 | item shop        |
|         3 |          10 | Black Magic shop |
|         4 |           9 | White Magic shop |
|         5 |          11 | special shop     |

## Open Checks

- Trace the field/menu path for item/equipment spells. The battle command and
  battle execution path are confirmed; field/menu restrictions still need a
  fuller pass before editing.
- Name the unknown weapon bytes `0` and `1`.
- Name the shared item metadata bytes `2` and `3`.
- Decode resistance/special mask bits for armor.
- Refine the weapon family/type labels for bits `0x01` and `0x40`
  against more monster behavior and in-game expectations.
