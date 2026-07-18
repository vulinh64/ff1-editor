# Decompiled Method Ledger

Track class and method roles here as they are confirmed. Avoid dumping full
decompiled source; keep this as a concise map from obfuscated symbols to meaning.

| Symbol         | Status    | Notes                                                                                                                                       |
|----------------|-----------|---------------------------------------------------------------------------------------------------------------------------------------------|
| `FinalFantasy` | Confirmed | MIDlet entry point. Manifest uses `FinalFantasy` as the MIDlet class.                                                                       |
| `i`            | Suspected | Very large class with display, input, menu, battle, save/load-looking methods and many state fields. Shop/menu code uses `aZ`/`ba` indexes. |
| `i.L()`        | Confirmed | Main field update/input routine. Includes airship boarding/landing; stock landing accepts terrain `0` and `10..14`.                         |
| `i.l(int)`     | Confirmed | Field recovery helper for inn/shelters. Inn is `0`, Sleeping Bag `1`, Tent `2`, Cottage `3`; Inn and Cottage share spell-charge recovery.   |
| `b.C()`        | Confirmed | Loads `cp` chunks into spell metadata, weapon/armor records, shop inventory tables, growth data, prices, and other shared data arrays.      |
| `j.e()`/`j.f()`/`j.i()` | Confirmed | Equipment helpers. `j.e(hero)` and `j.f(hero)` compute weapon hit/attack; `j.i(itemId)` counts equipped copies by weapon/armor subtype. |
