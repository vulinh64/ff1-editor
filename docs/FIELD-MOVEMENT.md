# Field Movement

This file records confirmed world-map movement and vehicle notes for the Namco
Bandai `Final Fantasy` J2ME jar.

## Vehicle State

World-map vehicle state is stored on the save object as `j.a.e`:

| Value | Meaning                  | Evidence                                  |
|------:|--------------------------|-------------------------------------------|
|     0 | walking                  | normal party position uses `j.a.a[]`      |
|     1 | ship/sea-looking state   | rendered with vehicle sprite resource 65  |
|     2 | canoe/river-looking state | rendered with vehicle sprite resource 66 |
|     3 | airship                  | uses `j.a.d[]` and requires flag `j.a.a[26]` |

The airship position/direction is stored in `j.a.d[0..2]`.

Airship boarding is a separate check from landing. `i.h(int x, int y)` returns
true when the party is on the world map, flag `j.a.a[26]` says the airship is
owned, and the party coordinates match `j.a.d[0..1]`. It does not check the
terrain byte, so an airship parked by the expanded landing patch remains
boardable from that tile.

## Terrain Ranges

World-map terrain bytes are read by `i.a(int x, int y)`.

Confirmed water-like ranges from traversal and encounter logic:

| Terrain range | Meaning                      | Evidence                         |
|--------------:|------------------------------|----------------------------------|
|       `55..60` | sea/coast water              | sea encounter branch in `i.j()`  |
|       `61..65` | river/canoe water            | river encounter branch in `i.j()` |

## Random Encounter Rate

The random encounter trigger is in `i.class`, private method `V()`. This port
does not appear to use a classic `random(255)` threshold directly. Instead it
uses a rising encounter counter, `aI`:

```text
if random(100) < aI:
    aI = -2 - random(4)
    start encounter
else:
    aI += 1
    if aI > 15:
        aI = 15
```

The counter starts negative (`-4` in the field object, and `-2 - random(4)` when
entering/loading a map). That creates a short grace period, then the chance rises
by one percentage point per eligible movement step and caps at `15%`.

Airship movement does not roll random encounters:

```text
if j.a.e == 3:
    no encounter
```

The only confirmed vehicle-specific encounter-rate clamp is for
`j.a.e == 2`, the canoe/river-looking state:

```text
if j.a.e == 2 and aI > 1:
    aI = 1
```

So code evidence says canoe/river travel gets the direct counter reduction, not
ship/sea travel. The ship/sea state is `j.a.e == 1`; no equivalent `j.a.e == 1`
rate clamp has been found in `i.V()`.

The formation-zone picker is separate from the chance roll:

```text
sea terrain 55..60: randomly choose encounter zone 64 or 65
river terrain 61..65: use encounter zone 66
world land: use ((x - 1) / 32) + (y / 32) * 8
non-world maps: use the current map encounter zone
```

This means ship travel can still feel less frustrating in play because it uses
different encounter zones/formations, even though the direct rate clamp is on
the canoe state.

Least-breaking future patch candidates should target `i.V()` only:

- lower the `aI` cap from `15` to a smaller value such as `8`, `10`, or `12`;
- extend the post-encounter grace reset from `-2 - random(4)` to a lower range;
- change only this call site from `random(100)` to a larger denominator such as
  `random(160)`, `random(200)`, or `random(255)`.

The stock airship landing check is in `i.class`, private method `L()`. While
`j.a.e == 3`, pressing the action key checks the current terrain byte and lands
only when the tile is `0` or `10..14`; otherwise it plays the failed landing
animation.

## Airship Landing Patch

The optional `Airship lands on safe terrain` patch changes only that landing
predicate. It keeps stock's `0` landing tile and expands the upper safe land
range from `10..14` to `10..33`. This deliberately rejects:

- terrain bytes `1..9`, which overlap water/vehicle boarding behavior;
- terrain bytes `34+`, which include higher blocked/special map tiles such as
  mountains, town roofs/walls, and other terrain that can confuse entry/boarding
  or leave the party unable to walk away.

The patch is an in-place bytecode replacement in `i.L()`. It keeps the original
code length and existing branch target offsets stable, so the surrounding CLDC
`StackMap` data is left untouched.
