# Skill And Effect Records

This file tracks the current spell/effect investigation for the local Namco
Bandai `Final Fantasy` J2ME jar.

## Runtime Loader

`b.class`, private static method `C()`, loads `cp0` chunk `1` into
`j.a:[[C]`. The chunk has 94 records, each 13 bytes:

```text
2-byte count + 94 spell/effect records * 13 bytes
```

The loader maps each source record to runtime fields as follows:

| Source Bytes | Runtime Field | Current Label |
|-------------:|---------------|---------------|
|          0-1 | `j.a[id][10]` | price/cost |
|            2 | `j.a[id][0]`  | raw field 0 |
|            3 | `j.a[id][1]`  | effect id / targeting router |
|            4 | `j.a[id][4]`  | effect kind |
|            5 | `j.a[id][2]`  | power or status bits |
|            6 | `j.a[id][8]`  | hit/accuracy modifier |
|            7 | `j.a[id][5]`  | raw field 5 |
|            8 | `j.a[id][6]`  | animation/resource id |
|            9 | `j.a[id][7]`  | animation flags |
|           10 | `j.a[id][3]`  | element/status mask |
|        11-12 | `j.a[id][9]`  | class permission mask |

The editor exposes these records in the `Skills` tab. Price, `power/status`,
and `accuracy` are editable; partly confirmed fields keep conservative labels
until the battle behavior is fully decoded.

## Editable Fields

The editable pass writes the shop price/cost field plus two one-byte effect
fields:

| Source Bytes | Runtime Field | Editor Column | Range |
|-------------:|---------------|---------------|------:|
|          0-1 | `j.a[id][10]` | Price         | 0..65535 |
|            5 | `j.a[id][2]`  | Power/Status  | 0..255 |
|            6 | `j.a[id][8]`  | Accuracy      | 0..255 |

These fields are context-sensitive. For `Damage` and `Healing` kinds,
`Power/Status` is a base amount. For status/recovery/protection kinds, it is
usually a bit mask. `Accuracy` is used by hit checks and by a few non-damage
kinds as a secondary value.

## Confirmed Invoker Routing

Battle command selection in `g.class` confirms that both learned spells and
equipment-cast spells route through `j.a[spellId][1]`:

```text
learned spell: g.j = j.a[learnedSpellId][1]
weapon use:    g.j = j.a[j.c[itemId - 7][5]][1]
armor use:     g.j = j.a[j.d[itemId - 48][3]][1]
```

The battle execution path then resolves item/equipment casts through the same
effect helper as learned spells. For hero actions, command type `1` is learned
magic and command type `2` is item/equipment use; both call
`g.class`, private static `a(byte targetSide, int targetIndex, int spellId)`,
while `g.C[g.Y]` still identifies the acting hero.

Consumables in the same command path point at internal records:

| Item ID | Item        | Effect Record |
|--------:|-------------|--------------:|
|       1 | Potion      |            91 |
|       2 | Antidote    |            92 |
|       3 | Gold Needle |            93 |

## Battle Effect Behavior

`g.class`, private static `a(byte targetSide, int targetIndex, int spellId)`,
switches on `j.a[spellId][4]` to compute result values such as damage, healing,
status success/failure, and sentinel values. Follow-up mutation is handled in
the neighboring `a(byte,int,int)` helper and related battle animation states.

Stock bytecode inspection shows this helper reads spell metadata, target defense
and resistance fields, target status, HP, and RNG, but does not read the acting
hero's Intelligence. That means stock learned spells and stock item/equipment
casts do not scale with INT.

The optional INT-scaled spell damage patch wraps this helper's integer returns.
Because item/equipment casts use the same helper while `g.C[g.Y]` is still the
active hero slot, positive enemy-target damage from player item/equipment casts
is scaled by the acting hero's INT in the same way as learned spell damage.
Party-target healing and non-damage/sentinel results remain unscaled.

Known field uses from that helper:

- `j.a[id][4]`: effect kind switch.
- `j.a[id][2]`: damage/healing amount or status mask, depending on kind.
- `j.a[id][3]`: element/status mask used against target weakness/resistance
  style fields.
- `j.a[id][8]`: hit/accuracy modifier.

## Effect Kind Names

Effect kind names are decoded from `g.class`, private static
`a(byte,int,int)`, which computes success/result values, and the neighboring
mutation helper with the same parameters. Names stay broad when one kind covers
several status bits.

| Kind | Editor Label | Evidence |
|-----:|--------------|----------|
|    0 | None | returns zero; used by field/menu spells like Exit/Teleport. |
|    1 | Damage | uses power, element mask, target defense/resistance, and random/double damage. |
|    2 | Undead damage | same damage shape but gated by target field `g[target][18] & 0x08`; used by Dia line. |
|    3 | Status inflict | applies `power/status` as a status bit on success. |
|    4 | Sleep stage down | decrements target sleep/stun stage field. |
|    5 | Mind blast | reduces monster field `8`; used by Fear-like behavior. |
|    6 | Unused/no-op | returns zero and has no follow-up mutation. |
|    7 | Healing | returns negative HP restoration using the power byte as the base. |
|    8 | Status recovery | checks/applies status recovery bits; used by Blindna, Poisona, Life, Stona, Vox, Full-Life. |
|    9 | Defense up | adds `power/status` to defense-like battle field. |
|   10 | Resistance up | ORs `power/status` into resistance/status-protection field; Invis also sets a display/status bit. |
|   11 | Attack/accuracy up | adds `power/status` to attack and `accuracy` to hit rate; used by Saber. |
|   12 | Haste | raises a speed/hit-count stage field. |
|   13 | Attack up | adds `power/status` to attack; used by Temper. |
|   14 | Defense down | subtracts `power/status` from defense-like battle field; used by Focus/Focara. |
|   15 | Full heal/death | returns full restoration or death-scale sentinel behavior; used by Curaja. |
|   16 | Evasion up | adds `power/status` to evasion-like battle field; used by Blink, Silence, Invisira. |
|   17 | Resistance clear | clears resistance/status-protection field; used by Dispel. |
|   18 | Conditional status | status inflict with additional HP/status gates; used by Stun, Blind, Kill. |

White LV8 order in this Java ME port is `Full-Life`, `Holy`, `NulAll`,
`Dispel`. This differs from the previous hardcoded editor labels and is
confirmed by both `cp0` skill behavior and `PACK0_4` text order.

## Excalibur Note

Physical attack resolution reads weapon special bytes from `j.c[weapon][6]` and
`j.c[weapon][7]`. If those bytes intersect enemy fields `g[target][20]` or
`g[target][18]`, the attack gains extra damage and hit chance. Excalibur has
special bytes `255,255`, so it matches every bit in those two target fields.
Keep the UI label as `Special` until those enemy fields are named with more
confidence.
