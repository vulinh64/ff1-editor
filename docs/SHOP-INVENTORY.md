# Shop Inventory

This file records the first confirmed shop inventory layout for the local
Namco Bandai `Final Fantasy` J2ME jar.

For item categories, equipment record layouts, prices, and class equip masks,
see `ITEM-EQUIPMENT.md`.

## Runtime Loader

Shop inventories are loaded in `b.class`, private static method `C()`, through
the same `cp` resource family used by spells, equipment, and growth data:

```text
f.a("cp", 30, j.k)
```

The static shop chunk source array in `b` maps the six runtime shop categories
to `cp0` chunks:

| Shop Type | `cp0` Chunk | Current Meaning             |
|----------:|------------:|-----------------------------|
|         0 |           7 | weapon shop                 |
|         1 |           8 | armor shop                  |
|         2 |           6 | item shop                   |
|         3 |          10 | Black Magic shop            |
|         4 |           9 | White Magic shop            |
|         5 |          11 | special or small shop table |

Each shop chunk starts with a big-endian unsigned 16-bit row count, followed by
five one-byte item or spell ids per row. Runtime loader `b.C()` copies those
five bytes into `j.a[shopType][shopRow][0..4]` and stores the nonzero count in
slot `5`.

The shop screen does not expose more than those five row bytes. Blank visual
space under a short inventory list is just unused menu space. Zero bytes inside
a five-slot row can be filled with additional goods for that shop category.

The shop/menu state in `i.class` reads `aZ` and `ba` from event bytes when a
shop opens. It then renders and buys from:

```text
j.a[this.aZ][this.ba][slot]
```

The `i` menu code confirms `aZ == 0` is weapons: it indexes weapon records as
`j.c[itemId - 7]` and weapon names as text id `346 + 2 * itemId`.

Map event scripts use opcode `8` for inventory shops. The next two script bytes
are the runtime shop type (`aZ`) and row (`ba`). Service-style menus use opcode
`9`; the next two bytes are service column (`aZ`) and service price row (`ba`).
A full scan of the packed `m0` map-script chunks found no `08 02 06` opener for
ordinary item-shop row `6`. The only Bottled Faerie shop opener found was
`08 05 00`, the special-shop row used by the Caravan path.

## Editor Behavior

The Shops tab is location-first. Confirmed town/service mappings are shown under
their town names; unconfirmed rows are grouped under `<Unknown>` until in-game
inspection or event-script decoding confirms their location.

Inventory shops expose only placement: five editable good-id slots per shop row.
Decoded names, categories, and prices are shown as reference, but the price bytes
remain owned by the Equipment / Items and Skills tabs. For example, Cornelia's
item shop can replace Potion (`1`) with Cottage (`6`), or add Cottage to an empty
slot by changing a `0` slot to `6`.

Inn prices are separate from inventory goods. The editor exposes them as service
rows with item `Bed` and an editable price.

## Cornelia Anchor

The Cornelia shop rows give us a reliable first anchor:

| Shop Type | Chunk | Row | Status              | Inventory                                     |
|----------:|------:|----:|---------------------|-----------------------------------------------|
|         0 |     7 |   0 | confirmed in-game   | Nunchaku, Knife, Staff, Rapier, Hammer        |
|         1 |     8 |   0 | confirmed in-game   | Clothes, Leather Armor, Chain Mail, empty, empty |
|         2 |     6 |   0 | confirmed Cornelia | Potion, Antidote, Sleeping Bag, empty, empty |
|         4 |     9 |   0 | confirmed Cornelia | LV1 White Magic |
|         3 |    10 |   0 | confirmed Cornelia | LV1 Black Magic |

Rows are separate per shop type, so weapon row `0`, armor row `0`, and item
row `0` are different records. The weapon and armor mappings were confirmed by
in-game tests that changed those shop bytes; those changes are now handled as
ordinary Shops tab inventory edits. The item row `0` mapping is confirmed by
in-game shop inspection.

## Weapon Records

Weapon records are loaded from `cp0` chunk `3` into `j.c`. The chunk length is
`371` bytes:

```text
2-byte count + 41 weapon records * 9 bytes
```

The current confirmed record shape from `b.C()` is:

| Bytes | Runtime Field |
|------:|---------------|
|     0 | `j.c[id][0]`  |
|     1 | `j.c[id][1]`  |
|   2-3 | `j.c[id][2]`, big-endian unsigned short |
|     4 | `j.c[id][3]`  |
|     5 | `j.c[id][4]`  |
|     6 | `j.c[id][5]`  |
|     7 | `j.c[id][6]`  |
|     8 | `j.c[id][7]`  |

The runtime item id offset is confirmed:

```text
weaponIndex = itemId - 7
itemId = weaponIndex + 7
nameTextId = 346 + 2 * itemId
descriptionTextId = nameTextId + 1
```

This means item ids `7..47` are weapons. Text id `360` / item id `7` is blank,
so the first visible weapon is item id `8` (`Nunchaku`). `Knife` is item id `9`,
not item id `7`.

## Cornelia Weapon Shop

`cp0` chunk `7` is the weapon-shop table. It begins at absolute offset
`0x00001a54`; row `0` begins at `0x00001a56` and contains:

| Slot | Item ID | Name     |
|-----:|--------:|----------|
|    0 |       8 | Nunchaku |
|    1 |       9 | Knife    |
|    2 |      10 | Staff    |
|    3 |      11 | Rapier   |
|    4 |      12 | Hammer   |

This matches the expected early Cornelia weapon shop inventory. The Shops tab
can edit these slot ids directly.

Sell Excalibur instead of Nunchaku:

```text
cp0 offset 0x00001a56: 0x08 -> 0x2e
```

`0x2e` is item id `46`, whose weapon name text id is `438` (`Excalibur`) and
description text id is `439`.

Sell Masamune instead of Knife:

```text
cp0 offset 0x00001a57: 0x09 -> 0x2f
```

`0x2f` is item id `47`, whose weapon name text id is `440` (`Masamune`) and
description text id is `441`.

Both Cornelia weapon-shop replacement byte edits are confirmed in-game. The
`Nunchaku -> Excalibur` and `Knife -> Masamune` changes update the Cornelia town
weapon shop display and purchase result.

## Weapon Shop Rows

Known weapon-shop rows:

| Row | Row Offset   | Item IDs             | Decoded Items                                           | Location Status |
|----:|--------------|----------------------|---------------------------------------------------------|-----------------|
|   0 | `0x00001a56` | `8, 9, 10, 11, 12`   | Nunchaku, Knife, Staff, Rapier, Hammer                  | confirmed Cornelia weapon shop |
|   1 | `0x00001a5b` | `12, 13, 14, 15, 0`  | Hammer, Broadsword, Battle Axe, Scimitar                | wiki-backed Pravoka weapon shop |
|   2 | `0x00001a60` | `16, 17, 18, 19, 24` | Iron Nunchaku, Dagger, Crosier, Saber, Mythril Sword    | wiki-backed Elfheim weapon shop |
|   3 | `0x00001a65` | `18, 19, 20, 22, 0`  | Crosier, Saber, Longsword, Falchion                     | wiki-backed Melmond weapon shop |
|   4 | `0x00001a6a` | `42, 0, 0, 0, 0`     | Cat Claws                                               | confirmed Gaia weapon shop |
|   5 | `0x00001a6f` | `23, 24, 25, 26, 0`  | Mythril Knife, Mythril Sword, Mythril Hammer, Mythril Axe | confirmed Crescent Lake weapon shop |

## Cornelia Armor Shop

`cp0` chunk `8` is the armor-shop table. It begins at absolute offset
`0x00001a74`; row `0` begins at `0x00001a76` and contains:

| Slot | Item ID | Name          |
|-----:|--------:|---------------|
|    0 |      49 | Clothes       |
|    1 |      50 | Leather Armor |
|    2 |      51 | Chain Mail    |
|    3 |       0 | empty         |
|    4 |       0 | empty         |

The Shops tab can fill the two empty Cornelia armor-shop slots:

```text
cp0 offset 0x00001a79: 0x00 -> 0x50
cp0 offset 0x00001a7a: 0x00 -> 0x58
```

`0x50` is item id `80` (`Ribbon`) and `0x58` is item id `88` (`Protect Ring`).
Because this uses shop type `1`, the normal armor-shop detail and purchase path
reads armor records from `j.d[itemId - 48]`.

The Cornelia armor-shop fill byte edit is confirmed in-game. The two empty
slots become Ribbon and Protect Ring in the Cornelia town armor shop display and
purchase result.

## Armor Shop Rows

Known armor-shop rows:

| Row | Row Offset   | Item IDs             | Decoded Items                                                   | Location Status |
|----:|--------------|----------------------|-----------------------------------------------------------------|-----------------|
|   0 | `0x00001a76` | `49, 50, 51, 0, 0`   | Clothes, Leather Armor, Chain Mail                              | confirmed Cornelia armor shop |
|   1 | `0x00001a7b` | `50, 51, 52, 65, 81` | Leather Armor, Chain Mail, Iron armor, Leather Shield, Leather Gloves | wiki-backed Pravoka armor shop |
|   2 | `0x00001a80` | `52, 59, 66, 74, 75` | Iron armor, Copper Armlet, Iron Shield, Leather Cap, Helm        | wiki-backed Elfheim armor shop |
|   3 | `0x00001a85` | `53, 60, 76, 82, 83` | Knight's Armor, Silver Armlet, Great Helm, Bronze Gloves, Steel Gloves | wiki-backed Melmond armor shop |
|   4 | `0x00001a8a` | `61, 88, 0, 0, 0`    | Ruby Armlet, Protect Ring                                       | confirmed Gaia armor shop |
|   5 | `0x00001a8f` | `54, 67, 72, 77, 84` | Mythril Mail, Mythril Shield, Buckler, Mythril Helm, Mythril Gloves | confirmed Crescent Lake armor shop |

## Item Shop Rows

`cp0` chunk `6` is the item-shop table. It begins at absolute offset
`0x00001a2f`, has seven rows, and each row has five item-id slots:

| Row | Row Offset   | Item IDs         | Decoded Items                                      | Location Status |
|----:|--------------|------------------|----------------------------------------------------|-----------------|
|   0 | `0x00001a31` | `1, 2, 4, 0, 0`  | Potion, Antidote, Sleeping Bag                     | confirmed Cornelia item shop |
|   1 | `0x00001a36` | `1, 2, 4, 5, 0`  | Potion, Antidote, Sleeping Bag, Tent               | confirmed Pravoka item shop |
|   2 | `0x00001a3b` | `1, 2, 5, 6, 3`  | Potion, Antidote, Tent, Cottage, Gold Needle       | confirmed Elfheim item shop |
|   3 | `0x00001a40` | `1, 2, 5, 6, 0`  | Potion, Antidote, Tent, Cottage                    | unknown town/event |
|   4 | `0x00001a45` | `1, 2, 5, 6, 0`  | Potion, Antidote, Tent, Cottage                    | likely Crescent Lake item shop; inventory confirmed in-game, row shares contents with row `3` |
|   5 | `0x00001a4a` | `1, 2, 5, 6, 3`  | Potion, Antidote, Tent, Cottage, Gold Needle       | unknown town/event |
|   6 | `0x00001a4f` | `104, 0, 0, 0, 0` | Bottled Faerie                                    | duplicate Caravan-looking raw row; no `m0` event-script opener found |

The table has room for five goods per item-shop row. Rows with zeros can be
filled at the data level, but this only changes shops whose event script opens
shop type `2` with that row id. A Phoenix Down-style item should wait until
these row-to-town mappings are confirmed.

Crescent Lake's item shop is confirmed in-game to sell Potion, Antidote, Tent,
and Cottage. The raw item-shop table has two identical rows with those goods
(`3` and `4`), so the inventory is confirmed but the exact row should still be
validated through event-script decoding before applying a town-specific item
shop patch.

Melmond is confirmed to have no item shop. There is no blank or obvious
Melmond-specific row in `cp0` chunk `6`; this likely means Melmond simply has no
event that opens shop type `2`. In other words, the absence is expected to live
in the map/event script layer, not in the shared item-shop inventory table.

Row `6` mirrors the Caravan's initial Bottled Faerie inventory, but the actual
Caravan behavior uses special shop type `5`, not ordinary item-shop type `2`.
The map-script trace found no ordinary item-shop opener for type `2`, row `6`.

## Magic Shop Rows

Cornelia is confirmed to sell level-1 White Magic row `0` and level-1 Black
Magic row `0`. Pravoka is confirmed to sell level-2 White Magic row `1` and
level-2 Black Magic row `1`. Elfheim's two White Magic shops and two Black
Magic shops are confirmed in-game. The later town mappings are wiki-backed or
likely where noted and are surfaced in the Shops tab with that status.

| Location | Shop Type | Chunk | Row | Spell IDs          |
|----------|----------:|------:|----:|--------------------|
| Cornelia |         4 |     9 |   0 | `1, 2, 3, 4`       |
| Cornelia |         3 |    10 |   0 | `33, 34, 35, 36`   |
| Pravoka  |         4 |     9 |   1 | `5, 6, 7, 8`       |
| Pravoka  |         3 |    10 |   1 | `37, 38, 39, 40`   |
| Elfheim  |         4 |     9 |   2 | `9, 10, 11, 12`    |
| Elfheim  |         3 |    10 |   2 | `41, 42, 43, 44`   |
| Elfheim  |         4 |     9 |   3 | `13, 14, 15, 16`   |
| Elfheim  |         3 |    10 |   3 | `45, 46, 47, 48`   |
| Melmond  |         4 |     9 |   4 | `17, 18, 19, 20`   |
| Melmond  |         3 |    10 |   4 | `49, 50, 51, 52`   |
| Crescent Lake |    4 |     9 |   5 | `21, 22, 23, 24`   |
| Crescent Lake |    3 |    10 |   5 | `53, 54, 55, 56`   |
| Gaia     |         4 |     9 |   6 | `25, 26, 0, 0`     |
| Gaia     |         3 |    10 |   6 | `57, 58, 0, 0`     |
| Gaia     |         4 |     9 |   7 | `30, 31, 32, 0`    |
| Gaia     |         3 |    10 |   7 | `62, 63, 64, 0`    |
| Onrac    |         4 |     9 |   8 | `27, 28, 0, 0`     |
| Onrac    |         3 |    10 |   8 | `59, 60, 0, 0`     |
| Lufenia  |         4 |     9 |   9 | `29, 0, 0, 0`      |
| Lufenia  |         3 |    10 |   9 | `61, 0, 0, 0`      |

Lufenia's hidden shop rows contain only Full-Life (`29`) and Flare (`61`).

## Inn And Service Price Rows

`cp0` chunk `14` is loaded into runtime `j.e`, a seven-row table with two
big-endian unsigned 16-bit prices per row:

| Row | Row Offset   | Prices       | Location Status |
|----:|--------------|--------------|-----------------|
|   0 | `0x00002cd1` | `30, 40`     | confirmed Cornelia Inn Bed price is `30`; second service unconfirmed |
|   1 | `0x00002cd5` | `50, 80`     | unknown town/service |
|   2 | `0x00002cd9` | `100, 200`   | unknown town/service |
|   3 | `0x00002cdd` | `100, 0`     | unknown town/service |
|   4 | `0x00002ce1` | `200, 400`   | unknown town/service |
|   5 | `0x00002ce5` | `300, 750`   | unknown town/service |
|   6 | `0x00002ce9` | `500, 750`   | unknown town/service |

The shop/menu code subtracts `j.e[this.ba][this.aZ]` for these service-style
menus. Current evidence shows Inn uses service column `0`; other service mapping
still needs in-game confirmation before naming.

## Special Shop Rows

`cp0` chunk `11` is the special or small shop table. It has two rows:

| Row | Row Offset   | Item IDs          | Decoded Items                 | Location Status |
|----:|--------------|-------------------|-------------------------------|-----------------|
|   0 | `0x00001afe` | `104, 0, 0, 0, 0` | Bottled Faerie                | confirmed Caravan before Bottled Faerie purchase |
|   1 | `0x00001b03` | `1, 2, 3, 0, 0`   | Potion, Antidote, Gold Needle | confirmed Caravan-style evolved inventory after special purchase |

The shop code has a special branch for `aZ == 5`: if the relevant game flag is
already set, it forces `ba = 1`; after buying a key-item-style good from this
shop path, it also switches to row `1`. That matches the Caravan: it first sells
Bottled Faerie, then evolves into a small normal-item shop.

## Elfheim Magic-Shop Cross-Check

Elfheim is confirmed as item-shop row `2` by in-game inspection and by its magic
shop pattern. The town has four magic shops, corresponding to White Magic rows
`2` and `3` plus Black Magic rows `2` and `3`:

| Shop Type | Chunk | Row | Spell IDs         | Decoded Spells                         |
|----------:|------:|----:|-------------------|----------------------------------------|
|         4 |     9 |   2 | `9, 10, 11, 12`   | Cura, Diara, NulBlaze, Heal            |
|         4 |     9 |   3 | `13, 14, 15, 16`  | Poisona, Fear, NulFrost, Vox           |
|         3 |    10 |   2 | `41, 42, 43, 44`  | Fira, Hold, Thundara, Focara           |
|         3 |    10 |   3 | `45, 46, 47, 48`  | Sleepra, Haste, Confuse, Blizzara      |

This lines up with the third item-shop row progression: Cornelia row `0`,
Pravoka row `1`, and Elfheim row `2`.

## Item-Shop Armor Check

Armor ids share the same item-name and shared-price table as consumables, so an
armor id in an item-shop row is likely to display a name and price. It is not a
confirmed safe data-only patch target, though: the item shop uses shop type `2`
and skips the armor-shop preview/equipment path (`aZ == 1`) that indexes armor
records as `j.d[itemId - 48]`. Use the armor-shop row for buyable armor.

## Open Checks

- Map every town item-shop row in `cp0` chunk `6` before adding any new
  consumable globally. Phoenix Down is a candidate later enhancement, but it
  needs a bytecode-backed item id/effect route before shop rows alone are useful.
- Confirm whether item id `7` is intentionally an unused/blank weapon sentinel
  or has a hidden menu purpose.
- Decode the eight weapon record fields far enough to expose weapon stats in a
  future equipment tab.
