# Final Fantasy I Java ME - Game Mechanics

*A player-facing deep dive into how the Namco Bandai Java ME port resolves
growth, combat, magic, weapons, monsters, rewards, encounters, and optional
editor improvements.*

---

## 1. Introduction

This guide explains the mechanics of the Namco Bandai Java ME release of
*Final Fantasy*: how heroes grow, how attacks land, how magic is resolved, what
weapon "effective against" text really means, and how monsters are built.

FF1 looks simple until the odd questions start piling up: why can a critical hit
delete a heavily armored hero, why does INT feel strangely quiet in stock magic,
why does Excalibur feel good against almost everything, and why does a battle
with one Buccaneer award 120 Gil but only 15 EXP per living hero? This guide is
for those questions.

A few ground rules:

- **This is a mechanics guide, not a file-format guide.** It describes what the
  numbers mean in play, not where every byte lives in the game files.
- **Evidence first.** Notes here come from local bytecode inspection, data
  decoding, and in-game checks. When something is not fully confirmed, it is
  described as a design idea or open area.
- **Faithful port, modern eyes.** The Java ME game is broadly faithful to the
  NES release's feel: swingy level-ups, class restrictions, charge-based magic,
  weapon quirks, harsh encounters, and opaque monster traits. The editor can
  preserve that, or offer optional improvements for players who want a smoother
  remix.

That last point matters. The editor should not quietly turn FF1 into a different
game. The best improvements are opt-in, explainable, and shaped like answers to
specific rough edges rather than blanket rewrites.

---

## 2. Heroes And Core Stats

The six base classes are:

| Class      | Role                                           |
|------------|------------------------------------------------|
| Warrior    | Durable physical attacker                      |
| Thief      | Fast physical class, later upgrades into Ninja |
| Monk       | Unarmed physical class with unusual scaling    |
| Red Mage   | Hybrid caster and fighter                      |
| White Mage | Healing, protection, and undead damage         |
| Black Mage | Offensive and control magic                    |

Class change does not give a new "starting stat" table. Upgraded classes inherit
the live character's current stats:

| Base class | Upgraded class |
|------------|----------------|
| Warrior    | Knight         |
| Thief      | Ninja          |
| Monk       | Master         |
| Red Mage   | Red Wizard     |
| White Mage | White Wizard   |
| Black Mage | Black Wizard   |

The visible body stats are the familiar FF1 set:

- **STR** - physical power and part of weapon damage.
- **AGL** - hit rate and hit-count progression.
- **INT** - present as a stat, but stock spell output does not scale from it.
- **STA** - feeds HP growth.
- **LCK** - improves escape odds and battle-start advantage odds.

Starting HP has one important Java ME quirk: the normal game path reads the
starting HP byte as signed. Values above 127 can display as negative HP. The
editor therefore treats the ordinary starting HP field conservatively.

---

## 3. Level-Ups And Growth

Leveling is class-group based. Upgraded classes use the same growth group as
their base class, so Knight follows Warrior growth, Ninja follows Thief growth,
and so on.

At each level-up, the game checks class growth data for:

- strong HP growth
- STR
- AGL
- INT
- STA
- LCK
- spell-charge growth for magic levels 1 through 8

If a body-stat slot is marked for the level, the stat grows. If it is not marked,
the game still rolls a 1-in-8 chance. This is the classic FF1 swinginess: two
characters of the same class can land in slightly different places because
their "maybe" gains did or did not fire.

That is why level-ups can feel uneven in normal play. One Warrior might gain HP,
STR, and STA on a level, while another level-up might only show the guaranteed
HP trickle. The class plan is deterministic; the individual gains still have
some dice in them.

HP has two parts:

```text
strong HP bonus when the strong HP check succeeds
always add a stamina-based HP amount
```

When strong HP succeeds, the bonus is:

```text
20 + random(0..4)
```

The stamina contribution is always applied after that:

```text
(STA + STA gained this level) / 4 + 1
```

All arithmetic uses Java integer behavior, so division truncates.

### Optional improvement: strong level-ups

The editor can make every HP/stat growth check succeed. This keeps the same
growth table and level curve, but removes the unlucky "I gained almost nothing"
level-up rolls. It is a stronger and less swingy version of the same system, not
a new progression model.

---

## 4. Spell Permissions And Spell Charges

Learning a spell and having charges to cast it are separate systems.

A class must have permission for a spell before it can learn that spell. Red
Mage and Red Wizard are separate permission identities, so upgraded permissions
are not automatically inherited just because the names sound related.

Spell charges are stored by spell level, from level 1 to level 8. Stock charges
cap at 9. Mage-style classes begin with level-1 charges, while Warrior, Thief,
and Monk begin with none.

Stock charge growth has a class gate:

```text
base Warrior, Thief, Monk: no spell-charge growth
mage classes and upgraded classes: can gain charges from growth data
```

This means a non-caster can be given spell permissions but still be unable to
cast unless charges are also provided somehow.

Modder trap: giving Warrior access to Flare does not by itself make Warrior able
to cast Flare. The character still needs charges for that spell level.

### Optional improvements: universal charges and 15 max charges

The editor has two charge-oriented improvements:

- **Universal spell-charge growth** lets every class gain spell charges while
  leveling, while still requiring spell permissions before spells matter.
- **15 max spell charges** raises the per-level charge cap from 9 to 15 and
  adjusts recovery so inns and cottages can refill the larger pool.

These patches are intentionally separate from the Magic Matrix. One controls who
may learn spells; the other controls whether the character has fuel to cast
them.

---

## 5. Physical Attacks

A hero's physical attack is built from class/stat/equipment state:

Four levers matter separately: attack controls the damage roll, hit rate
controls whether swings connect, hit count controls how many swings are tried,
and the weapon index controls the critical threshold. Raising one lever does not
automatically raise all the others.

```text
attack = battle attack bonus + weapon attack contribution
hit rate = battle hit bonus + weapon hit contribution
```

For normal weapon users:

```text
weapon attack contribution = STR / 2 + weapon damage
weapon hit contribution = AGL + weapon hit rate
```

Unarmed Monk and Master are special:

```text
unarmed attack contribution = level * 2
unarmed hit count is doubled
```

Hit count is derived from hit rate:

```text
hit count = hit rate / 32 + 1
```

Each hit attempt rolls against a 0..200 range. If the roll is too high, the hit
misses. If it lands, the same roll is also compared against the critical
threshold.

### Weapon criticals

This port preserves the classic FF1 weapon-index critical behavior. A weapon's
critical threshold is based on its internal weapon index, not a separate visible
"crit" stat.

Practically:

- later weapons tend to have better critical thresholds because their weapon
  indices are higher;
- Masamune is extremely strong here, using the highest weapon index;
- if the critical threshold is higher than the current hit threshold, it is
  clipped by the hit threshold.

### Damage and critical damage

For each successful hit:

```text
attack roll = attack + random(attack)
normal damage = max(1, attack roll - target defense)
```

If the hit is critical, stock damage adds the raw pre-defense attack roll:

```text
critical damage = normal damage + attack roll
```

That is why critical hits can punch through high defense. Defense reduces the
normal damage term, but the critical bonus itself uses the raw attack roll.

### Optional improvement: enemy crits respect party defense

The editor can change only enemy critical hits against the party so the critical
bonus doubles the already defense-reduced damage instead:

```text
enemy critical damage to party = normal damage + normal damage
```

Party critical hits against enemies stay stock. This keeps player crits exciting
while making enemy crit spikes less armor-piercing.

---

## 6. Weapon Effectiveness

Some weapons are effective against certain monster traits or weaknesses.

Examples:

| Weapon      | Practical meaning                                                      |
|-------------|------------------------------------------------------------------------|
| Wyrmkiller  | Effective against Dragon-type monsters                                 |
| Sun Blade   | Effective against Undead monsters                                      |
| Coral Sword | Effective against Aquatic monsters                                     |
| Flame Sword | Effective against Fire-weak enemies, Undead, and Regenerative monsters |
| Ice Brand   | Effective against ice-weak enemies                                     |
| Excalibur   | Broadly effective because it matches every known bit                   |

The bonus is simple and physical:

```text
if weapon matches monster weakness or archetype:
    attack += 4
    hit chance += 40
```

Multiple matches do not stack. Excalibur can match many things, but it still
gets one effectiveness bonus, not eight.

This is also not a spell formula. A Flame Sword hit does not become a Fire spell;
it remains a physical attack with a flat effectiveness boost.

Worked example: if Flame Sword hits a Fire-weak monster, the game adds the flat
weapon effectiveness bonus. If the same monster is also Undead, the bonus does
not apply twice. It is a yes/no bonus, not a per-tag multiplier.

---

## 7. Magic And Skill Effects

The Java ME port uses a shared spell/effect helper for learned magic,
equipment-cast spells, and some item effects.

The important stock finding: **normal spell output does not read the caster's
INT.** Intelligence exists, but stock damage and healing formulas use spell data,
target defenses, target resistances, status, HP, and RNG.

### Damage spells

Damage spells are mostly "spell power plus randomness," then adjusted by the
target's weakness or resistance. They are not normally powered by the caster's
INT.

For normal damage spells, the confirmed broad shape is:

```text
simplified:
base = spell power
if target is weak to the spell element: base += base / 2
if target resists the spell element: base /= 2
base = max(1, base)
damage = base + random(base)
if hit check succeeds strongly: damage *= 2
damage caps at 9999
```

Because weakness and resistance are separate bitmasks, stock bytecode allows
both to be set at the same time. If that happens, both checks run. The editor
prevents this overlap because "weak and resistant to Fire" is mechanically
possible but conceptually ugly.

### Status and other chance effects

Many status-like effects use a chance shaped like:

```text
chance = 148 + spell success modifier - target magic defense
```

Then weaknesses and resistances adjust that chance:

```text
matching weakness: +40 chance
matching resistance: -148 chance
```

So resistance is extremely powerful for status prevention. When both weakness
and resistance are set in stock data, resistance usually dominates.

### Healing

Cure-like and Heal-like spells use a negative HP-restoration result internally:

```text
Cure/Heal-style randomized healing:
healing amount = spell power + random(spell power)
```

Full-heal and revival effects are separate categories.

### Optional improvements: INT scaling

The editor can make offensive spell damage scale with the acting hero's INT:

```text
damage = damage + damage * INT / 200
```

At 99 INT, affected damage is roughly 149% of stock.

There is also a separate healing patch for Cure-like and Heal-like restoration:

```text
healing = healing + healing * INT / 200
```

These are optional because the stock port is faithful to the older design where
INT is not a universal spell-power stat.

---

## 8. Monsters

Monsters have normal combat stats:

- HP
- attack
- hit count
- defense
- evasion
- magic defense
- EXP reward
- Gil reward

They also have three hidden tag systems:

- **Archetypes** - hidden monster tags for family or trait, such as Dragon,
  Undead, Werebeast, Aquatic, Mage, or Regenerative.
- **Weaknesses** - elements/status groups that improve incoming spell effects
  and can trigger some weapon effectiveness.
- **Resistances** - elements/status groups that reduce incoming spell effects.

The editor caps Archetypes to three selections per monster. This is a design
safety rule, not a stock engine rule. It keeps monsters readable: a Werewolf can
be Magical, Werebeast, and Regenerative without turning into every monster type
at once.

Weaknesses and Resistances have no count limit, but the editor prevents the same
element from appearing in both. Chaos is the clean extreme example:

```text
Weaknesses: none
Resistances: everything
```

### Regeneration

The Regenerative archetype has confirmed battle behavior. At end-of-round HP
tick time, regenerative monsters recover:

```text
max HP / 20
```

That is a 5% max-HP regeneration tick.

### Rewards

Monster EXP is the monster's total EXP award, then divided among living party
members. Gil is awarded as the full battle total.

Confirmed examples:

| Monster     | Stored EXP | Stored Gil | In-game result                                                        |
|-------------|-----------:|-----------:|-----------------------------------------------------------------------|
| Goblin      |          6 |          6 | Three Goblins give 18 Gil; EXP displays as 18 / 4 = 4 per living hero |
| Buccaneer   |         60 |        120 | One gives 120 Gil and 15 EXP per living hero                          |
| Crazy Horse |         63 |         15 | One gives 15 Gil and 15 EXP per living hero                           |

The Buccaneer row is the easiest way to remember the reward split: Gil is the
monster's full value, but EXP is shared across living party members.

---

## 9. Battle Flow And Turn Order

Stock turn order starts each turn by shuffling all party and enemy slots
together. Empty or dead slots can still exist in the queue, but action
dispatching decides whether they actually do anything.

Preemptive and ambush states do not build special queues. Instead:

- preemptive battles make enemies skip action selection on the first turn;
- ambushes make party slots skip their chosen commands on the first turn;
- boss/no-run encounters skip those advantage states.

### Optional improvement: party action order

The editor can change normal battle order to make party commands resolve first:

```text
Magic -> Item -> Attack -> Run/other -> enemies
```

Party slot order is preserved inside each command group. Enemy slots still act
afterward in randomized enemy order. Ambush first turns keep the stock enemy
advantage behavior.

Example: in stock combat, a Black Mage's Fire spell, a Warrior's attack, and an
enemy attack are all mixed into the same shuffled turn queue. With the party
action-order patch, party magic resolves before party items, then party attacks,
then enemies. It is still FF1 command combat, but less chaotic.

This is one of the most "modern feel" patches: it reduces chaotic turn-order
swings without rewriting combat into a different game.

---

## 10. Running, Recovery, And Field Movement

Run checks have a boss/no-run gate. If the encounter is marked no-run, escape is
blocked. Normal encounters use the stock escape helper unless patched.

For normal encounters, escape compares a party-side roll against enemy pressure:

```text
escape stat = runner AGL + runner LCK + runner INT
success = random(escape stat) > random(average enemy escape pressure)
```

Luck also contributes to the battle-start advantage roll. The leader's AGL and
LCK are combined before the game decides whether the party gets a preemptive
turn or is ambushed:

```text
advantage base = (leader AGL + leader LCK) / 8
```

Boss/no-run encounters skip this preemptive/ambush setup entirely.

Enemies can flee too, but they use a different system. Each monster has a hidden
morale value. When that enemy picks an action, the game compares morale against
the party leader's level:

```text
enemy flee chance = clamp(80 - morale + leader level * 2, 0, 50) / 50
```

Higher leader level makes ordinary enemies more likely to flee. Lower morale
does the same. A morale value of `255` means the enemy will not auto-flee from
this check, and boss/no-run encounters disable enemy fleeing entirely.

Fear-style effects work by lowering that morale value. They do not instantly
delete the monster; they make the next action decision much more likely to be
"run away." If morale reaches `0`, the enemy will flee on its next allowed
action decision in a normal run-enabled encounter.

### Optional improvement: always successful run

The editor can make normal escape attempts always succeed while preserving the
boss/no-run gate. This is a convenience patch, not a balance-neutral one: it
makes random encounters easier to disengage from.

### Inns, tents, cottages, and spell charges

Field recovery restores HP and spell charges. Stock recovery is built for the
normal 9-charge cap.

Stock shelter recovery skips KO members. Cottage normally gives stronger
recovery than lower shelters, but it still follows that KO skip in the unpatched
game.

### Optional improvement: Cottage revives KO

The editor can make Cottage revive KO party members before applying its normal
full recovery. Sleeping Bag and Tent still skip KO members.

### Random encounters

The world map uses a rising encounter counter rather than a flat classic roll.
The chance starts in a short grace period after an encounter, rises by movement
steps, and caps at 15%.

Airship movement does not roll random encounters.

The confirmed vehicle rate clamp applies to canoe/river travel. Ship travel
uses different encounter zones, but no equivalent direct rate clamp has been
found.

### Optional improvement: airship lands on safe terrain

Stock airship landing accepts a narrow set of land terrain. The editor can
expand this to additional safe land terrain while still rejecting water-like and
blocked/special terrain.

---

## 11. Faithful Port, Sensible Improvements

This Java ME release is remarkably faithful in spirit. It keeps several classic
FF1 traits:

- random-feeling level-ups
- class-specific spell permissions
- spell charges rather than MP
- weapon-index critical behavior
- physical weapon effectiveness as a flat bonus
- harsh enemy crit spikes
- random turn order
- no-run boss encounters
- Intelligence not powering stock spells

That faithfulness is valuable. The editor should not erase it by default.

But optional improvements can make the game friendlier while respecting its
shape:

| Improvement                                | Why it helps                            | Why it still feels like FF1                       |
|--------------------------------------------|-----------------------------------------|---------------------------------------------------|
| Strong level-ups                           | Removes weak level-up rolls             | Uses the same stat system and growth tables       |
| Universal spell-charge growth              | Enables creative class builds           | Still requires spell permissions and charges      |
| 15 max spell charges                       | Makes late-game magic less cramped      | Keeps charge-based magic instead of MP            |
| INT spell damage/healing                   | Gives INT visible value                 | Uses modest scaling and leaves many effects stock |
| INT+STA reduce enemy spells (experimental) | Makes defensive stats matter            | Does not touch physical attacks or player spells  |
| Enemy crit defense fix                     | Reduces armor-bypassing spikes          | Keeps player crits stock                          |
| Party action order                         | Makes commands feel more controllable   | Keeps command groups and enemy randomness         |
| Always successful run                      | Reduces random encounter friction       | Boss/no-run encounters remain locked              |
| Cottage revives KO                         | Makes the top shelter feel worth buying | Lower shelters stay weaker                        |
| Expanded airship landing                   | Reduces traversal frustration           | Still rejects unsafe terrain                      |

The guiding principle is simple: keep the old machine recognizable, but let
players choose which rough edges to sand down.

---

## 12. Common Misconceptions

| Misconception                                        | What the local Java ME logic shows                                                       |
|------------------------------------------------------|------------------------------------------------------------------------------------------|
| INT powers stock spell damage                        | Stock spell output does not read caster INT; INT scaling is an optional editor patch.    |
| Flame Sword deals Fire spell damage                  | Weapon effectiveness is still physical damage with a flat bonus.                         |
| EXP and Gil are awarded the same way                 | Gil is awarded as the full battle total; EXP is divided among living party members.      |
| Weakness and resistance cannot overlap in stock data | The stock logic allows overlap; the editor prevents it for clarity and balance.          |
| Archetype limits are an engine rule                  | The editor caps Archetypes to three selections as a readability and safety rule.         |
| Luck is purely decorative                            | Luck contributes to escape odds and battle-start advantage odds.                         |
| Fear instantly removes enemies                       | Fear-style effects lower enemy morale; the enemy flees on a later action decision.       |
| High starting HP is always safe                      | The stock starting-HP path can treat values above 127 as signed and display negative HP. |

---

## 13. Still Being Excavated

The current confirmed guide is not complete. Good future deep-dive targets:

- remaining monster record fields beyond the exposed combat stats and masks;
- armor resistance behavior in full detail;
- spell naming/text editing;
- whether support spells such as Haste, Temper, Saber, and Dia-like spells
  deserve their own optional INT scaling model;
- a safe unsigned or wider starting-HP patch for values above 127.

Until those are confirmed, the editor should keep the relevant fields read-only
or conservatively labeled.
