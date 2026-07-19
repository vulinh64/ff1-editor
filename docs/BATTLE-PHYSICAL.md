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
monsterFamilyOrStatusMask = g[target][18]
monsterElementWeaknessMask = g[target][20]

if ((weaponSpecialA & monsterElementWeaknessMask) != 0
    || (weaponSpecialB & monsterFamilyOrStatusMask) != 0) {
    attack += 4
    hitChance += 40
}
```

This is a single boolean effectiveness bonus. Matching several bits does not
stack several bonuses.

Excalibur has `0xff,0xff` in these two weapon-special bytes, so it matches any
bit present in either monster mask. Practically, Excalibur gets the same one-time
`+4` attack and `+40` hit-chance bonus against monsters with any decoded
weakness/family bit, such as undead, dragons, or elemental weaknesses. It does
not apply separate Dia/fire/dragon formulas; those remain spell/effect logic.

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
