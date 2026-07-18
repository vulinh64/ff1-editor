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
nameTextId = 346 + 2 * itemId
descriptionTextId = nameTextId + 1
```

The JavaFX Items tab reads these layouts through `ItemEquipmentDiscoveryService`
and displays the decoded records in three read-only tables: Weapons, Armor, and
Items. The same discovery path can be smoke-tested with:

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
|     7 | special/element byte, not fully named |
|     8 | special/effect byte, not fully named  |

Damage and accuracy are confirmed by `j.f(hero)` and `j.e(hero)`:

```text
attack = STR / 2 + weaponDamage
hitRate = AGI + weaponAccuracy
```

Unarmed Monk/Master are special-cased elsewhere and use level-based attack.
Weapon critical chance is not a separate record field; battle code uses the
equipped weapon id/index as the critical threshold.

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
assigns `g.j = j.a[j.c[itemId - 7][5]][1]` before target selection. The field
remains read-only until the surrounding use restrictions and target behavior are
fully mapped.

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
|     3 | evasion penalty contribution          |
|     4 | spell id cast when used, `0` for none |
|     5 | resistance/special mask               |

Defense and evasion are confirmed by `j.b(hero, slot, armorId)` and
`j.a(hero, slot, armorId)`:

```text
defense = sum equippedArmor[1]
evasion = baseEvasion - sum equippedArmor[2]
```

### Armor Permissions

| ID | Type | Armor | Absorb | Evasion | Casts | Resistance | Allowed Classes |
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

- Trace the runtime path that casts item/equipment spells when used from a menu
  or battle. The battle command path is confirmed; field/menu restrictions still
  need a fuller pass before editing.
- Name the unknown weapon bytes `0`, `1`, `7`, and `8`.
- Name the shared item metadata bytes `2` and `3`.
- Decode resistance/special mask bits for armor and elemental/effective-family
  bits for weapons.
