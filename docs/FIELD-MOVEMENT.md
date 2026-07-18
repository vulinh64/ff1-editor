# Field Movement

This file records confirmed world-map movement and vehicle notes for the Namco
Bandai `Final Fantasy` J2ME jar.

## Vehicle State

World-map vehicle state is stored on the save object as `j.a.e`:

| Value | Meaning                 | Evidence                                  |
|------:|-------------------------|-------------------------------------------|
|     0 | walking                 | normal party position uses `j.a.a[]`      |
|     1 | ship-looking state      | rendered with vehicle sprite resource 65  |
|     2 | canoe/river-looking state | rendered with vehicle sprite resource 66 |
|     3 | airship                 | uses `j.a.d[]` and requires flag `j.a.a[26]` |

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
