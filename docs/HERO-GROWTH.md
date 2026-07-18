# Hero Growth

This file records the first confirmed level-up mechanism notes for the local
Namco Bandai `Final Fantasy` J2ME jar.

## Runtime Fields

The battle reward and level-up routine is in `g.class`, method `F()`.

Confirmed character fields:

| Field   | Meaning         |
|---------|-----------------|
| `a.a:I` | Current EXP     |
| `a.b:B` | Character level |
| `a.c:S` | Max HP          |
| `a.c:B` | Strength        |
| `a.d:B` | Agility         |
| `a.e:B` | Intelligence    |
| `a.f:B` | Stamina         |
| `a.g:B` | Luck            |

## Level-Up Algorithm

The routine checks `j.p:[I` for the next EXP threshold. Characters stop leveling
after level 50. EXP is capped at `999999`.

The growth matrix is loaded into `g.a:[[[B]` from packed `cp` resource chunk 4.
It is shaped as:

```text
6 base class groups x 49 level-up rows x 14 bytes
```

The runtime indexes it as:

```text
growth[classId % 6][currentLevel - 1][slot]
```

The first six slots govern hero stat growth:

| Slot | Meaning          |
|-----:|------------------|
|    0 | Strong HP growth |
|    1 | Strength         |
|    2 | Agility          |
|    3 | Intelligence     |
|    4 | Stamina          |
|    5 | Luck             |

If a slot is nonzero, that stat grows for the level. If a slot is zero, the game
rolls a 1-in-8 chance. HP growth adds `20 + random(0..4)` when the strong HP
check succeeds, then always adds `(stamina + staminaGain) / 4 + 1`. Body stats
gain one point when their slot succeeds.

## Strong Level-Up Class Patch

`HeroLevelGrowthClassPatcher` patches `g.class` with the JDK 25 Class-File API.
It finds the six level-up random roll sequences:

```text
Random.nextInt() >>> 1 % 8
```

and changes the modulus operand from `8` to `1`. The result of `% 1` is always
zero, so every non-guaranteed HP/stat check succeeds without changing method
length or stack behavior.

Effect:

- every level-up has strong HP growth
- STR, AGL, INT, STA, and LCK each gain at least `+1`
- original EXP thresholds, level cap, HP cap, and spell-charge growth remain
  unchanged

The patch is intentionally separate from the normal starting-stat data patch.

This was tested in-game from level 1 to level 2. The status screens showed every
body stat gaining at least `+1`, and HP received the strong-growth path. Current
HP can be lower than max HP after the test if the character took damage before
opening the status screen; compare max HP for the growth result.

## Spell-Charge Growth

Spell-charge growth uses the same `cp0` chunk 4 growth matrix as stat growth.
Slots `6..13` are the charge increments for spell levels `8..1`:

```text
charge[levelIndex] += growth[classId % 6][currentLevel - 1][13 - levelIndex]
```

The game caps each spell charge at `9` after applying the increment.

The cap is a runtime bytecode clamp, not a nibble-sized storage limit. Character
current and max spell charges are stored as byte arrays:

```text
a.b[8] = current spell charges for levels 1..8
a.c[8] = max spell charges for levels 1..8
```

Save/load writes and reads one byte per current/max charge. Battle casting checks
the current charge for zero, then decrements it:

```text
chargeLevel = (spellId - 1) % 32 / 4
a.b[chargeLevel]--
```

Field recovery currently adds `10` current charges per level, then clips current
to max. That is enough to refill the stock cap of `9`, but a higher max-charge
patch may also need to raise the recovery amount if full restoration is desired.

Implemented 15-charge patch:

- `g.class` keeps the universal charge-growth gate relaxed from `classId >= 3`
  to `classId >= 0`;
- `g.class` changes the max-charge clamp from `9` to `15`;
- `cp0` chunk 4 rewrites charge slots `6..13` for all six growth groups;
- every spell level gains `+1` max charge at character levels
  `3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 39, 42, 45`;
- `i.class`, private static `l(int)`, changes field recovery from `+10` to
  `+15` current charges before clipping current charges to max.

With that schedule, every class can reach `15 15 15 15 15 15 15 15` max charges
by level 45, assuming the character has spell permissions and learns spells for
those levels. The patch still leaves save/load storage at one byte per current
and max charge.

Future higher-cap prospect: `127` is the likely hard practical ceiling without
changing storage width, but it needs broader testing because values above `127`
become negative if treated as signed bytes.

The class upgrade does not point at a separate growth row. It uses modulo `6`:

| Runtime class | Growth group |
|---------------|--------------|
| Knight        | Warrior      |
| Ninja         | Thief        |
| Master        | Monk         |
| Red Wizard    | Red Mage     |
| White Wizard  | White Mage   |
| Black Wizard  | Black Mage   |

Confirmed natural final charges by level 50, assuming the usual level-1 mage
starting charges:

| Growth group      | Final charges L1..L8 |
|-------------------|----------------------|
| Red Mage/Wizard   | `9 9 9 9 9 8 8 7`   |
| White Mage/Wizard | `9 9 9 9 9 9 9 9`   |
| Black Mage/Wizard | `9 9 9 9 9 9 9 9`   |

The level-up routine has a hardcoded gate before applying spell-charge growth:

```java
if (classId >= 3) {
    // add charges from growth slots 6..13
}
```

That means base Warrior, Thief, and Monk do not gain charges even if their
growth data is edited. Knight, Ninja, and Master do enter the charge loop after
upgrade because their runtime ids are `6..8`.

## Battle Stat Effects

Physical attack resolution is in `g.class`, private static
`a(boolean attackerIsHero, boolean targetIsHero, int attackerIndex, int targetIndex)`.

For hero attackers:

- attack is `battleAttackBonus + j.f(hero)`;
- `j.f(hero)` is `STR / 2 + weaponPower`, except unarmed Monk/Master uses
  `level * 2`;
- hit rate is `battleHitBonus + j.e(hero)`;
- `j.e(hero)` is `AGI + weaponAccuracy`;
- hit count is `hitRate / 32 + 1`, doubled for unarmed Monk/Master;
- the critical-hit threshold is the equipped weapon record index
  `j.a.a[hero].h`, not a separate weapon crit field or class field;
- visible weapon item ids are `weaponIndex + 7`, so Masamune item id `47`
  uses crit threshold `40`.

That last point means this port appears to preserve the classic FF1 weapon-index
critical chance behavior, but with the local `0..40` weapon record index rather
than the visible `7..47` item id.

Each hit attempt rolls one value in `0..200`. The swing misses when the roll is
above the hit threshold. If it hits, the same roll is compared to the crit
threshold. Agility therefore affects hit chance and hit count, but it does not
add to the crit threshold. If a weapon crit threshold is higher than the current
hit threshold, the crit threshold is clipped to the hit threshold.

## Universal Spell-Charge Hybrid Patch

To make every class gain charges while leveling, we need both sides:

- a `cp0` chunk 4 data patch that copies the stronger White Mage / Black Mage
  charge-growth template into every growth group while preserving stat growth
- a `g.class` Class-File API patch that changes the charge gate from
  `classId >= 3` to `classId >= 0`

This only gives characters charges. They still need spell permissions from the
Magic Matrix before they can learn and cast spells.

The editor implements this as one global option. Load detection combines both
required pieces: `cp0` chunk 4 must match the universal charge-growth data and
`g.class` must contain the relaxed class gate. If only one side matches, the
state is treated as unknown/unsupported rather than silently applying a partial
patch.

## Output Flow

Global growth patches are selected from the command-bar `Build Patched JAR`
modal, following the VDDOH convention. The modal is separate from the Heroes and
Magic Matrix tabs because these patches affect runtime behavior globally rather
than one visible data table.
