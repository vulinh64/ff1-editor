# Hero Magic

This file records the current reverse-engineering notes for class spell
permissions in the local Namco Bandai `Final Fantasy` J2ME jar.

## Spell Metadata Loader

Spell metadata is loaded in `b.class`, private static method `C()`.

That method opens the `cp` resource family:

```text
f.a("cp", 30, j.k)
```

and then reads `f.b(1)` into:

```text
j.a:[[C
```

`j.a` is the global spell metadata table. The loader creates each spell record
with 11 `char` fields. Field `9` is the spell's class permission mask. Field
`10` is used as the spell price/cost in the magic shop UI. Field `1` is the
invoked effect/targeting router: learned spells and equipment-cast spells assign
`g.j = j.a[spellId][1]` before battle target selection.

The relevant bytecode shape is:

```text
chunk = f.b(1)
count = f.a(chunk, 0)
j.a = new char[count][11]
...
j.a[spell][9] = f.a(chunk, recordOffset)
```

## Class Permission Mask

The class mask is also used by character method `a.a()`:

```text
class 0..5   -> 1 << classId
class 6..11  -> 256 << (classId - 6)
```

Confirmed class bits:

| Bit |      Hex | Class        |
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

For "any class can learn/cast this spell", the mask should become:

```text
0x3f3f
```

That includes all six base-class bits and all six upgraded-class bits. Red Mage
and Red Wizard are separate bits, so upgraded permissions are not automatically
inherited by this matrix.

## Learn Check

The magic learn routine is in `i.class`, private method:

```text
private boolean l(int spellId, int characterIndex)
```

The first gate is:

```text
if ((j.a[spellId][9] & party[characterIndex].classMask()) == 0) reject
```

After that it checks whether the character already knows the spell, then stores
the spell id into that character's learned-spell slots:

```text
a.a[levelGroup][slot] = spellId
```

The level group is derived from the spell id:

```text
levelGroup = ((spellId - 1) % 32) / 4
```

Each level group has three learned-spell slots.

## UI Display Check

The magic shop/detail UI also renders class icons from `j.a[spell][9]`. It
checks base classes with:

```text
mask & (1 << classId)
```

and upgraded classes with:

```text
mask & (256 << classId)
```

where that display loop's `classId` is `0..5`.

## Patch Plan

To make a Red Mage or Red Wizard learn higher-level spells such as level-8
black magic, patch `j.a[spell][9]` for those spell records. For a broad editor
option, expose the 12 class bits per spell as checkboxes and write the resulting
16-bit mask back to the `cp` spell metadata source.

This is now implemented as the `Magic Matrix` tab. The table uses spells as rows
and hero classes as columns, with White Magic and Black Magic split into sub-tabs.
There is no broad "allow all classes" button by design; Warrior, Thief, Monk, and
other non-casters can be enabled manually for experiments, but they still need
spell charges from separate data/runtime behavior before those permissions matter.

Keep this separate from spell charges:

- spell charges are stored on the character in `a.b:[B` and `a.c:[B`
- spell charge growth is handled by the level-up matrix in `g.class`
- having permission does not help if the character has no charge for that spell
  level

Confirmed matrix expectations:

- White Wizard normally has every White Magic permission and no Black Magic
  permission.
- Black Wizard normally has every Black Magic permission and no White Magic
  permission.
- Red Wizard has permissions beyond Red Mage and can naturally learn more spells,
  but Red Mage/Red Wizard permissions are separate class bits.

## Open Item

The raw `cp0` chunk parser is now reconciled with `f.b(1)`:

- `cp0` has a 30-entry little-endian length table.
- chunk `0` is `426` bytes, matching `2 + 106 * 4`.
- chunk `1` is `1224` bytes, matching `2 + 94 * 13`.
- learnable spell IDs `1..64` use chunk `1`; spell ID `0` is the dummy record.
- each permission mask is stored big-endian at `chunk1 + 2 + spellId * 13 + 11`.

## Spell Text Source

Spell names and descriptions are decoded from `PACK0_4`, not hardcoded in the
editor. The table is length-prefixed:

```text
first text id + count + count text blobs
spell name text id = firstTextId + spellId * 2
spell description text id = spellNameTextId + 1
```

For the local jar, this makes White LV8 order `Full-Life`, `Holy`, `NulAll`,
`Dispel`; ID `30` is the Holy damage record.

## Future Spell Editing

Renaming spells should be feasible, but it is not yet mapped. The likely first
target is the same spell metadata/text loading path used by `b.class` and
`Ff1TextService`. Keep names separate from the Magic Matrix: the matrix controls
who can learn/cast a spell, while a later Magic tab can expose visible spell
names, prices, elements, targeting, or other observable spell fields after their
record bytes are confirmed.

## INT Damage Scaling Patch

The stock battle spell-effect helper in `g.class`, private static
`a(byte targetSide, int targetIndex, int spellId)`, computes spell output from
spell metadata, target defenses/resistances/status, and RNG. Current bytecode
inspection shows no caster Intelligence read in that formula.

The optional editor patch uses the JDK 25 Class-File API to transform that
helper's integer returns. Positive enemy-target results from player actors use:

```text
damage = damage + damage * intelligence / 200
```

At INT `99`, this yields roughly `149%` of the stock result. Failed effects,
`10000` sentinel values, party-target healing, monster spells, and values capped
at `9999` are left unchanged.
