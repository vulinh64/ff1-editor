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
