# Hero Classes

This file records the first confirmed hero/class labels found in the local
Namco Bandai `Final Fantasy` J2ME jar.

The class names are encoded with the game font table from `fn0` and are stored
in `PACK0_1`. The current discovery command is:

```cmd
java -jar target\ff1-data-editor-0.1.0.jar heroes ff1
```

## Confirmed Class Names

| ID | Class        | Tier     | Upgrade From | Source                        |
|---:|--------------|----------|--------------|-------------------------------|
|  0 | Warrior      | Base     | -            | `PACK0_1` offset `0x000000ba` |
|  1 | Thief        | Base     | -            | `PACK0_1` offset `0x000000c3` |
|  2 | Monk         | Base     | -            | `PACK0_1` offset `0x000000ca` |
|  3 | Red Mage     | Base     | -            | `PACK0_1` offset `0x000000d0` |
|  4 | White Mage   | Base     | -            | `PACK0_1` offset `0x000000da` |
|  5 | Black Mage   | Base     | -            | `PACK0_1` offset `0x000000e6` |
|  6 | Knight       | Upgraded | Warrior      | `PACK0_1` offset `0x000000f2` |
|  7 | Ninja        | Upgraded | Thief        | `PACK0_1` offset `0x000000fa` |
|  8 | Master       | Upgraded | Monk         | `PACK0_1` offset `0x00000101` |
|  9 | Red Wizard   | Upgraded | Red Mage     | `PACK0_1` offset `0x00000109` |
| 10 | White Wizard | Upgraded | White Mage   | `PACK0_1` offset `0x00000115` |
| 11 | Black Wizard | Upgraded | Black Mage   | `PACK0_1` offset `0x00000123` |

## Confirmed Starting Stats

These values are confirmed from in-game level-1 status screenshots. Their exact
packed byte offsets are still being traced. The source labels call the fifth
body stat `Vitality`; this editor uses `Stamina` because that is the label found
in the Java mobile resources.

| ID | Class        | HP | STR | AGL | INT | STA | LCK | Confidence                                   |
|---:|--------------|---:|----:|----:|----:|----:|----:|----------------------------------------------|
|  0 | Warrior      | 35 |  20 |   5 |   1 |  10 |   5 | confirmed by status screenshot               |
|  1 | Thief        | 30 |   5 |  10 |   5 |   5 |  15 | confirmed by status screenshot               |
|  2 | Monk         | 33 |   5 |   5 |   5 |  20 |   5 | confirmed by status screenshot               |
|  3 | Red Mage     | 30 |  10 |  10 |  10 |   5 |   5 | confirmed by status screenshot               |
|  4 | White Mage   | 28 |   5 |   5 |  15 |  10 |   5 | confirmed by status screenshot               |
|  5 | Black Mage   | 25 |   1 |  10 |  20 |   1 |  10 | confirmed by status screenshot               |
|  6 | Knight       | 35 |  20 |   5 |   1 |  10 |   5 | inherited from Warrior after class change    |
|  7 | Ninja        | 30 |   5 |  10 |   5 |   5 |  15 | inherited from Thief after class change      |
|  8 | Master       | 33 |   5 |   5 |   5 |  20 |   5 | inherited from Monk after class change       |
|  9 | Red Wizard   | 30 |  10 |  10 |  10 |   5 |   5 | inherited from Red Mage after class change   |
| 10 | White Wizard | 28 |   5 |   5 |  15 |  10 |   5 | inherited from White Mage after class change |
| 11 | Black Wizard | 25 |   1 |  10 |  20 |   1 |  10 | inherited from Black Mage after class change |

## Confirmed Level-1 Spell Charges

The status screenshots also confirm that Red Mage, White Mage, and Black Mage
begin with `LV1 2/2` spell charges. Warrior, Thief, and Monk begin with `0/0`.

## Confirmed Patch Table

The starting class template table is in the `cp0` resource entry at offset
`0x00003f05`. It contains six base-class records, 10 bytes each. Class upgrades
inherit live character stats, so upgraded classes do not have separate editable
starting records in this table.

Record layout confirmed from `j.a(int)`:

| Byte | Field                                 |
|-----:|---------------------------------------|
|    0 | Starting/current max HP byte          |
|    1 | Strength                              |
|    2 | Agility                               |
|    3 | Intelligence                          |
|    4 | Stamina                               |
|    5 | Luck                                  |
|    6 | Unknown short-ish template byte       |
|    7 | Unknown short-ish template byte       |
|    8 | Unknown short-ish template byte       |
|    9 | Initial spell charge byte for level 1 |

Because byte 0 is read as a signed Java byte and then converted to `short`, the
confirmed table cannot directly represent `999` HP. Setting HP to `200` produced
`-56/-56` in-game, confirming signed-byte behavior. The editor therefore keeps
starting HP at `0..127` and body stats at the FF1 visible cap of `0..99`.

Body stats were also tested at `99`; the game displays `99` for STR/AGL/INT/STA/LCK.
These fields are one byte in the source table, but the editor intentionally caps
them at the familiar FF1 visible maximum instead of allowing signed-byte overflow
experiments through the normal UI.

Future patch idea: add an optional in-game/bytecode patch that treats this
starting HP byte as unsigned. That should remain separate from the normal
data-only hero stat patch flow, similar to how VDDOH keeps engine fixes separate
from data edits.

## Notes

- The local jar uses `Warrior`, not `Fighter`.
- The local jar uses `Monk`, not `Black Belt`.
- The stored order is `Warrior`, `Thief`, `Monk`, `Red Mage`, `White Mage`,
  `Black Mage`, then the six upgraded classes.
- The confirmed local stat label is `Stamina`, not `Vitality`.
- This mobile version appears to use spell charges, so do not treat `MP` as a
  starting character stat until we decode the class/spell-charge records.
- Generic FF1 class ordering in guides may list Red Mage / Black Mage / White
  Mage / Monk differently. Use the local order above for editor IDs unless a
  later parser proves a separate runtime ID order.

## Current Editor Behavior

The Heroes tab edits the six base-class template records only. Upgraded class
rows are displayed for context but are not directly editable because class
upgrade keeps the live character's current stats instead of loading a second
starting-stat template. To avoid presenting them as independent frozen values,
upgraded rows mirror their base-class row in the UI; for example, editing
Warrior STR also updates Knight STR on screen.

Patched jars are built by replacing `cp0`; the input jar is never mutated.

## Next Target

Identify bytes `6..9` well enough to decide whether initial spell charges should
be exposed in the Heroes tab, a later Magic tab, or left as internal template
data.
