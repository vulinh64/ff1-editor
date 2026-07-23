# Battle Physical Damage

Physical attack resolution is in `g.class`, private static method
`a(boolean attackerIsParty, boolean targetIsParty, int attackerIndex, int targetIndex)`.

## Stock Critical Damage

For each successful hit, the game rolls a raw attack value and then subtracts
target defense:

```text
attackRoll = attack + random(attack)
normalDamage = max(1, attackRoll - targetDefense)
```

If the same hit roll is also inside the attacker's critical threshold, stock
damage adds the raw pre-defense attack roll:

```text
criticalDamage = normalDamage + attackRoll
```

This is why critical hits can hurt high-defense targets. Defense only reduces
the first term. The critical bonus itself bypasses defense.

## Weapon Special Bonus

For hero attacks, the routine reads two special bytes from the equipped weapon
record:

```text
weaponSpecialA = j.c[weaponIndex][6]
weaponSpecialB = j.c[weaponIndex][7]
```

Against monster targets, those bytes are checked against two monster masks:

```text
monsterFamilyOrTypeMask = g[target][18]
monsterElementWeaknessMask = g[target][20]

if ((weaponSpecialA & monsterElementWeaknessMask) != 0
    || (weaponSpecialB & monsterFamilyOrTypeMask) != 0) {
    attack += 4
    hitChance += 40
}
```

This is a single boolean effectiveness bonus. Matching several bits does not
stack several bonuses, and it does not change the physical attack into a spell
formula or separate elemental damage type.

Confirmed weapon examples:

| Weapon      | Special bytes | Meaning                                                                                            |
|-------------|---------------|----------------------------------------------------------------------------------------------------|
| Flame Sword | `0x10,0x88`   | Effective against fire-weak enemies, undead via `0x08`, and regenerative enemies via `0x80`.       |
| Ice Brand   | `0x20,0x00`   | Effective against ice-weak enemies.                                                                |
| Wyrmkiller  | `0x00,0x02`   | Effective against dragon/reptile-family enemies.                                                   |
| Great Sword | `0x00,0x04`   | Effective against the `0x04` family; local data includes gigas/ogres plus Goblin and Goblin Guard. |
| Sun Blade   | `0x00,0x08`   | Effective against undead; same monster family bit used by Dia-like damage gating.                  |
| Coral Sword | `0x00,0x20`   | Effective against aquatic enemies.                                                                 |
| Rune Blade  | `0x00,0x41`   | Effective against spellcaster/magical-style family bits, still partly fuzzy.                       |

Excalibur has `0xff,0xff` in these two weapon-special bytes, so it matches any
bit present in either monster mask. Practically, Excalibur gets the same one-time
`+4` attack and `+40` hit-chance bonus against monsters with any decoded
weakness/family bit, such as undead, dragons, or elemental weaknesses. It does
not apply separate Dia/fire/dragon formulas; those remain spell/effect logic.

## Weapon Affinity Damage Patch

`WeaponAffinityDamageClassPatcher` changes only the hero weapon-special match
bonus. Instead of stock `+4` attack and `+40` hit chance, a matching weapon uses:

```text
attack += weapon damage / 2
hitChance = 255
```

The added attack happens before defense, random damage rolls, and critical
handling. The game uses integer division here: Excalibur has `45` weapon damage,
so an affinity match adds `22` attack. The bonus is still a single yes/no
affinity bonus; matching multiple weakness or archetype bits does not stack
multiple half-weapon bonuses.

## Legendary Weapon Critical Toggle

`LegendaryWeaponCriticalClassPatcher` is a reversible command-bar toggle for
testing fast late-game routes and damage data collection. For hero attacks with
equipped weapon record `39` Excalibur or `40` Masamune, it forces:

```text
hitChance = 255
criticalThreshold = 200
```

The physical hit roll is `0..200`, so `255` makes those weapons no-miss and
`200` makes every resulting hit critical. This is independent of the weapon
affinity damage patch. If affinity already raised hit chance to `255`, the
no-miss assignment has no additional runtime effect for that swing, but the
legendary toggle still forces the critical threshold.

## Enemy Crits Respect Party Defense Patch

`EnemyCriticalDefenseClassPatcher` changes only enemy critical hits against party
members. The patched enemy-on-party critical bonus doubles the already
defense-reduced damage:

```text
enemyCriticalDamageToParty = normalDamage + normalDamage
```

Party critical hits against enemies remain stock:

```text
partyCriticalDamageToEnemy = normalDamage + attackRoll
```

This preserves useful party criticals against high-defense enemies while
preventing enemy criticals from bypassing party armor/defense.

Enemy physical attacks and their status-on-hit behavior are separate from the
spell/effect helper. The optional `INT+STA reduce enemy spell effects` patch
does not change this physical attack path.
