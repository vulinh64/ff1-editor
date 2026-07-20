# Battle Run/Escape

## Confirmed Runtime Path

The battle command menu stores command id `3` for Run:

```java
case 3: {
    g.b(true);
    this.s = 2;
    g.e[g.l][0] = 3;
}
```

During action resolution, command id `3` animates the actor backward until
`g.a[actor] <= -12`, then calls `g.c(actor)` to decide whether the escape
succeeds.

Original `g.c(int actor)` behavior:

```java
private static boolean c(int n) {
    int n2 = 0;
    if (a) {
        return true;
    }
    if ((j.b[e * 15 + 1] & 0xFF) == 1) {
        return false;
    }
    int n3 = j.a.a[n].d + j.a.a[n].g + j.a.a[n].e;
    int[] nArray = g.b();
    for (int i2 = 0; i2 < nArray.length; ++i2) {
        n2 += g[nArray[i2]][8] - 100 >> 4;
        n2 += g[nArray[i2]][10];
    }
    return (j.a.nextInt() >>> 1) % n3 > (j.a.nextInt() >>> 1) % (n2 /= nArray.length);
}
```

Meaning:

- `g.a == true` always succeeds. This is the battle-start advantage flag set by
  `g.I()`.
- Encounter byte `j.b[e * 15 + 1] == 1` blocks running. This is the no-run/boss
  gate and must be preserved by convenience patches.
- Otherwise success is random: one roll modulo the actor's stat sum
  `AGL + LCK + INT` must beat one roll modulo the average enemy escape pressure
  derived from monster fields `8` and `10`.

## Luck Usage

The Run helper directly reads the acting hero's Luck stat as part of the escape
roll:

```text
escapeStat = AGL + LCK + INT
success = random(escapeStat) > random(averageEnemyEscapePressure)
```

Luck is also used when battle-start advantage is rolled in `g.I()`:

```text
advantageBase = (leader.AGL + leader.LCK) / 8
roll = advantageBase * 2 + random(100 - advantageBase) - encounterChanceByte
```

If `roll >= 90`, the party gets preemptive advantage. If `roll <= 10`, the party
is ambushed. No-run/boss encounters skip both advantage and ambush setup.

## Enemy Fleeing

Enemies use a separate flee check during enemy action planning in
`g.class`, private static method `g(int enemySlot)`. This is not the same
helper used by party Run.

The check reads runtime monster field `8`, which is loaded from monster record
byte `10`. This field acts like flee morale:

```text
morale = monster runtime field 8
leaderLevelPressure = party slot 0 level * 2
roll = random(50)

enemy flees if:
    encounter no-run flag == 0
    morale != 255
    morale - leaderLevelPressure + roll < 80
```

Because `random(50)` returns `0..49`, the practical flee chance is:

```text
chance = clamp(80 - morale + leaderLevel * 2, 0, 50) / 50
```

Meaning:

- higher party leader level makes normal enemies more likely to flee;
- lower monster morale makes an enemy more likely to flee;
- morale `255` disables this automatic enemy fleeing;
- encounter byte `j.b[e * 15 + 1] == 1` disables enemy fleeing, just as it
  blocks party Run and battle-start advantage.

When an enemy chooses command id `3`, the battle action path animates it backing
out, shows the escape message, and marks the enemy slot inactive.

## Fear-Style Effects

Effect kind `5` is the Fear-style morale reduction path. It only applies to
monster targets. The mutation helper subtracts the spell/effect power byte from
the same runtime morale field used by enemy fleeing:

```text
if target is a monster and morale > 0:
    morale -= spellPower
    if morale < 0:
        morale = 0
```

If morale was already `0` or lower before the effect, the helper returns the
normal no-effect sentinel instead of lowering it again.

Fear-style effects do not directly remove the monster in the same helper. They
make the next enemy action-planning check much more likely to choose flee. Once
morale reaches `0`, a run-enabled encounter will choose flee on that monster's
next action decision, because `0 - leaderLevel * 2 + random(50) < 80` is always
true for normal positive party levels.

## Implemented Patch

`AlwaysSuccessfulRunClassPatcher` rewrites only the tail of `g.c(int)`:

```java
if (a) {
    return true;
}
if ((j.b[e * 15 + 1] & 0xFF) == 1) {
    return false;
}
return true;
```

The patch therefore makes normal escapes always succeed, while no-run/boss
encounters still reject Run. It is exposed in the global patch modal as
`Always successful run`.

## Encounter No-Run Flag

Encounter rows are loaded from `cp0` chunk `12` into `j.b`. The layout is:

```text
2-byte count + 245 records * 15 bytes
```

Current confirmed encounter record shape:

| Byte | Meaning                                      |
|-----:|----------------------------------------------|
|    0 | encounter formation type                     |
|    1 | no-run/boss-style flag                       |
|    2 | preemptive/ambush chance pressure            |
| 3..5 | monster id, minimum count, maximum count     |
| 6..8 | monster id, minimum count, maximum count     |
| 9..11 | monster id, minimum count, maximum count    |
| 12..14 | monster id, minimum count, maximum count   |

Byte `1` has at least these effects:

- Run immediately fails in `g.c(int actor)` when it is `1`.
- Battle-start advantage setup in `g.I()` is skipped when it is `1`, so
  preemptive and ambush rolls do not run for these encounters.

Piscodemon confirms that this flag is encounter-level, not monster-level. Monster
name text `PACK0_14` local id `103` decodes as `Piscodemon`, so Piscodemon is
monster id `103`. Encounter rows `28` and `156` both contain monster id `103`,
and both rows have byte `1` set to `1`:

| Encounter | Type | No-run flag | Chance byte | Groups                  |
|----------:|-----:|------------:|------------:|-------------------------|
|        28 |    0 |           1 |          33 | `103 x2..4`             |
|       156 |    0 |           1 |          33 | `103 x3..7`             |

So Piscodemon encounters behave like no-run/fixed battles, but the enforcement
comes from the encounter rows that place Piscodemons, not from a Piscodemon
monster-stat boss bit.

The editor's Monsters tab uses this same encounter-level evidence for its
Bosses / Fixed sub-tab: any monster found in a no-run encounter row is shown
there, while the remaining monsters are shown under Normal.
