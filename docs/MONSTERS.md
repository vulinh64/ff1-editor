# Monsters

This file tracks the monster record layout for the local Namco Bandai
`Final Fantasy` J2ME jar.

## Sources

- Names: `PACK0_14`, length-prefixed text table. Local text ids correspond to
  monster ids.
- Records: `cp0` chunk `15`, `128` records, `25` bytes each.
- Bosses / Fixed classification in the editor is encounter-derived: a monster is
  shown there when it appears in any `cp0` chunk `12` encounter row whose
  no-run/boss-style flag byte is `1`.

## Confirmed Reward Fields

Record bytes `4..5` are the total EXP award, big-endian unsigned 16-bit.
Record bytes `6..7` are Gil, big-endian unsigned 16-bit.

In-game EXP is divided among living party members, while Gil is awarded as the
full battle total. Confirmed examples:

| Monster | Record EXP | Record Gil | In-game evidence |
|---------|-----------:|-----------:|------------------|
| Goblin | 6 | 6 | Three Goblins give `18` Gil; `18 / 4` displays as `4` EXP per living hero. |
| Buccaneer | 60 | 120 | One Buccaneer gives `120` Gil; `60 / 4` displays as `15` EXP per living hero. |
| Crazy Horse | 63 | 15 | One Crazy Horse gives `15` Gil; `63 / 4` displays as `15` EXP per living hero. |

## Confirmed Editable Combat Fields

The Monsters tab edits these confirmed `cp0` chunk `15` record fields:

| Bytes | Field | Editor range |
|-------|-------|-------------:|
| `4..5` | EXP | `0..65535` |
| `6..7` | Gil | `0..65535` |
| `8..9` | HP | `0..999` |
| `12` | Defense | `0..255` |
| `13` | Evasion | `0..255` |
| `14` | Hit count | `0..255` |
| `16` | Attack | `0..255` |
| `20` | Archetype mask | up to 3 selected bits |
| `21` | Magic defense | `0..255` |
| `22` | Weakness mask | any selected bits, no overlap with resistance |
| `23` | Resistance mask | any selected bits, no overlap with weakness |

Known Archetype bits:

| Bit | Archetype |
|-----|-----------|
| `0x01` | Magical |
| `0x02` | Dragon |
| `0x04` | Giant/Ogre/Goblin |
| `0x08` | Undead |
| `0x10` | Werebeast |
| `0x20` | Aquatic |
| `0x40` | Mage |
| `0x80` | Regenerative |

Known Weakness/Resistance bits:

| Bit | Label |
|-----|-------|
| `0x01` | Poison/Stone |
| `0x02` | Death |
| `0x04` | Status |
| `0x08` | Time |
| `0x10` | Fire |
| `0x20` | Ice |
| `0x40` | Thunder |
| `0x80` | Earth |

## Current Editor Surface

The Monsters tab exposes editable EXP, Gil, HP, attack, hit count, defense,
evasion, magic defense, Archetypes, Weaknesses, and Resists columns. Archetypes
open a checkbox modal and are limited to three selected families per monster.
Weaknesses and Resists use the same checkbox modal without a selection limit.
The editor and patcher prevent selecting the same element as both a weakness and
a resistance.
For example, Chaos can be represented as no checked Weaknesses and all checked
Resists. Raw leading bytes and source offsets remain read-only.
