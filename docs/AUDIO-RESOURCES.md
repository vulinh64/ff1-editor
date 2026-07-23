# Audio Resources

The local Namco Bandai `Final Fantasy` J2ME jar contains extensionless `a0`
through `a21` resources that appear to be in-game music/audio tracks. Current
theme labels are based on user in-game identification and should be treated as
resource-name mappings until the playback loader and file format are fully
confirmed.

| Resource | Current Label                         | Confidence |
|----------|---------------------------------------|------------|
| `a0`     | Key item received theme               | observed   |
| `a1`     | Final Fantasy main theme              | observed   |
| `a2`     | Castle Cornelia theme                 | observed   |
| `a3`     | Overworld theme                       | observed   |
| `a4`     | Chaos Shrine theme                    | observed   |
| `a5`     | Matoya's Cave theme                   | observed   |
| `a6`     | Town theme                            | observed   |
| `a7`     | Shop theme                            | observed   |
| `a8`     | Sea theme                             | observed   |
| `a9`     | Chaos Shrine in the Past theme        | observed   |
| `a10`    | Marsh Cave theme                      | observed   |
| `a11`    | Interface theme                       | observed   |
| `a12`    | Airship theme                         | observed   |
| `a13`    | Volcano theme                         | observed   |
| `a14`    | Flying Fortress theme                 | observed   |
| `a15`    | Normal battle theme                   | observed   |
| `a16`    | Victory theme                         | observed   |
| `a17`    | Credit theme                          | observed   |
| `a18`    | Game over theme                       | observed   |
| `a19`    | Inn sleep theme                       | observed   |
| `a20`    | Main menu theme                       | observed   |
| `a21`    | Chaos battle-like theme, exact use TBD | tentative  |

## Replacement Prospect

Potential audio replacement should be implemented as JAR resource replacement,
not as a bytecode patch, if the game loads these tracks directly by entry name.
The replacement should keep the exact entry name (`a0`, `a1`, etc.).

The practical target for this project is emulator-first playback on PC. Newer
KEmulator builds can route MIDI to the host MIDI stack, including CoolSoft
VirtualMIDISynth, so richer MIDI arrangements can use a host-side soundfont
instead of consuming Java ME heap for sampled/orchestral playback. Real-device
Java ME compatibility is no longer the main design target; treat it as
best-effort only.

Before exposing this in the UI, confirm:

- whether each `a*` resource is standard MIDI (`MThd`) or another Java ME audio
  format;
- where the game loads `a0..a21`;
- whether larger replacement tracks work in the current KEmulator +
  CoolSoft VirtualMIDISynth workflow;
- whether looping is encoded in the file itself or in the playback code.
