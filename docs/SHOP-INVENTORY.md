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

This matches the expected early Cornelia weapon shop inventory. The editor now
implements data-only global patches for two optional replacements.

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

Both Cornelia weapon-shop replacement patches are confirmed in-game. The
`Nunchaku -> Excalibur` and `Knife -> Masamune` changes update the Cornelia town
weapon shop display and purchase result.

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

The editor implements a data-only global patch that fills the two empty
Cornelia armor-shop slots:

```text
cp0 offset 0x00001a79: 0x00 -> 0x50
cp0 offset 0x00001a7a: 0x00 -> 0x58
```

`0x50` is item id `80` (`Ribbon`) and `0x58` is item id `88` (`Protect Ring`).
Because this uses shop type `1`, the normal armor-shop detail and purchase path
reads armor records from `j.d[itemId - 48]`.

The Cornelia armor-shop fill patch is confirmed in-game. The two empty slots
become Ribbon and Protect Ring in the Cornelia town armor shop display and
purchase result.

## Item-Shop Armor Check

Armor ids share the same item-name and shared-price table as consumables, so an
armor id in an item-shop row is likely to display a name and price. It is not a
confirmed safe data-only patch target, though: the item shop uses shop type `2`
and skips the armor-shop preview/equipment path (`aZ == 1`) that indexes armor
records as `j.d[itemId - 48]`. Use the armor-shop row for buyable armor.

## Open Checks

- Confirm whether item id `7` is intentionally an unused/blank weapon sentinel
  or has a hidden menu purpose.
- Decode the eight weapon record fields far enough to expose weapon stats in a
  future equipment tab.
