# Battle Turn Order

## Confirmed Runtime Path

Battle turn order uses `g.C[]` as the action queue and `g.Y` as the current
queue index. Actors `0..3` are party slots; actors `4..12` are enemy slots.

At the start of a turn, state `7` calls `g.G()`:

```java
private static void G() {
    Y = 0;
    for (n = 0; n < C.length; ++n) {
        g.C[n] = n;
    }
    for (n = 0; n < 17; ++n) {
        int n2 = (j.a.nextInt() >>> 1) % C.length;
        int n3 = (j.a.nextInt() >>> 1) % C.length;
        int n4 = C[n2];
        g.C[n2] = C[n3];
        g.C[n3] = n4;
    }
}
```

So the original game starts every turn with all 13 actor slots and shuffles
them with 17 random swaps.

## Normal, Preemptive, Ambushed

`g.I()` sets two battle-start flags:

- `g.a == true`: party preemptive advantage.
- `g.b == true`: party ambushed.

Both flags are skipped entirely when encounter byte `j.b[e * 15 + 1] == 1`.
That is the same no-run/boss gate used by the Run helper.

The queue itself is still produced by `g.G()` in all three battle types. The
difference is in `H()`, the per-queue-slot dispatcher:

- Normal battle: neither flag is set, so healthy party slots and healthy enemy
  slots both act when their shuffled queue entry is reached.
- Preemptive battle: `g.a` makes healthy enemies skip their normal action
  selection, while party slots act normally.
- Ambushed battle: `g.b` makes healthy party slots skip their chosen command,
  while enemies act normally.

Status/death handling still runs in those skipped cases, so the flags are not
just a simple "delete all actors from the queue" rule.

## Implemented Patch

`PartyActionOrderClassPatcher` replaces `g.G()` with a branch:

- If `g.b` is set, use the original full random shuffle. The initial ambush turn
  therefore keeps the original enemy-control behavior.
- In normal or preemptive battles, build the party side first by command type and slot:
  `Magic -> Item -> Attack -> Run -> other`.
- Fill enemy slots `4..12` after the party and apply the original-style 17 random
  swaps only within that enemy tail.

This means normal battle order and the first preemptive turn become party actions
first, with spell casters first by slot, then item users by slot, then physical
attackers by slot, then run/other commands, followed by enemies in random enemy
order. Ambush clears `g.b` at turn end, so the turn after the party regains
control also follows this patched normal order.
