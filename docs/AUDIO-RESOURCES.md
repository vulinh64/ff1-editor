# Audio Resources

The local Namco Bandai `Final Fantasy` J2ME jar contains extensionless `a0`
through `a21` resources that appear to be in-game music/audio tracks. Current theme labels are based on user in-game
identification and should be treated as resource-name mappings until the playback loader and file format are fully
confirmed.

| Resource | Current Label                  | Confidence |
|----------|--------------------------------|------------|
| `a0`     | Key item received theme        | observed   |
| `a1`     | Final Fantasy main theme       | observed   |
| `a2`     | Castle Cornelia theme          | observed   |
| `a3`     | Overworld theme                | observed   |
| `a4`     | Chaos Shrine theme             | observed   |
| `a5`     | Matoya's Cave theme            | observed   |
| `a6`     | Town theme                     | observed   |
| `a7`     | Shop theme                     | observed   |
| `a8`     | Sea theme                      | observed   |
| `a9`     | Chaos Shrine in the Past theme | observed   |
| `a10`    | Marsh Cave theme               | observed   |
| `a11`    | Interface theme                | observed   |
| `a12`    | Airship theme                  | observed   |
| `a13`    | Volcano theme                  | observed   |
| `a14`    | Flying Fortress theme          | observed   |
| `a15`    | Normal battle theme            | observed   |
| `a16`    | Victory theme                  | observed   |
| `a17`    | Credit theme                   | observed   |
| `a18`    | Game over theme                | observed   |
| `a19`    | Inn sleep theme                | observed   |
| `a20`    | Main menu theme                | observed   |
| `a21`    | Chaos theme                    | observed   |

## Replacement Prospect

Audio replacement is selected in the Music tab and applied during
`Build Patched JAR` as exact JAR resource replacement, not as a bytecode patch. Each selected replacement file is
written back under the same entry name (`a0`,
`a1`, etc.), and the original input JAR is not mutated.

The Music tab can preview tracks through the desktop MIDI sequencer. Preview uses the selected replacement file when one
is configured, otherwise it plays the extracted original `a*` file from the loaded workspace. Preview does not loop,
pressing Preview for the already-playing file is a no-op, pressing Preview for another file switches the active
sequencer to that file immediately, and Stop closes the active sequencer immediately. Preview uses an unconnected Java
Sound sequencer and explicitly routes MIDI to
`VirtualMIDISynth #1` when available, then `CoolSoft MIDIMapper`, before falling back to the system default MIDI
receiver.

The practical target for this project is emulator-first playback on PC. Newer KEmulator builds can route MIDI to the
host MIDI stack, including CoolSoft VirtualMIDISynth, so richer MIDI arrangements can use a host-side soundfont instead
of consuming Java ME heap for sampled/orchestral playback. Real-device Java ME compatibility is no longer the main
design target; treat it as best-effort only.

Still confirm through emulator testing:

- whether each `a*` resource is standard MIDI (`MThd`) or another Java ME audio format;
- whether replacement tracks work in the current KEmulator + CoolSoft VirtualMIDISynth workflow;
- whether looping is encoded in the file itself or in the playback code.

Confirmed loader behavior:

- `f.class` opens `a` plus the selected numeric track id masked by `0x1ff`, and creates a Java ME player with MIME type
  `audio/midi`;
- `j.class` has a hardcoded stock BGM slot list from `32768` through `32789`, which maps back to `a0..a21` after the
  `0x1ff` mask;
- replacing existing tracks is therefore resource-only, but adding selectable track ids beyond the stock 22-track list
  is class/script-hook work.

## Additional Track Policy

Do not blindly add new music slots just because the runtime can load resources by numeric names such as `a21`.
Additional-track work should be tied to concrete places where this port currently falls back to a shared theme, or to
boss/fixed encounters where another version of Final Fantasy uses a distinct track.

The GBA version gives a useful target list for possible future overrides:

- Western Keep / Northwest Castle:
    - currently expected to fall back to the Castle Cornelia-style theme (`a2`);
    - this covers Astos's castle and the Castle/Citadel of Trials flow where the Rat Tail is loaded;
    - candidate for a creepier Castle Cornelia variant.
- Lich battle:
    - candidate for a dedicated fiend battle track where Lich is loaded.
- Marilith battle:
    - candidate for a dedicated fiend battle track where Marilith is loaded.
- Kraken battle:
    - candidate for a dedicated fiend battle track where Kraken is loaded.
- Tiamat battle:
    - candidate for a dedicated fiend battle track where Tiamat is loaded.

Use Chaos as the reference model for this kind of investigation: confirm where the game chooses the battle/map music id,
then confirm that the intended resource is loaded. User comparison against the GBA version confirms `a21` is the Chaos
theme.

Current investigation notes:

- Encounter data is in `cp0` chunk `12`, with 245 records of 15 bytes each. Encounter byte `1` is the no-run/boss-style
  flag. The encounter row does not currently show an obvious per-row music byte.
- Monster data is in `cp0` chunk `15`, with 128 records of 25 bytes each.
- Boss/fixed music overrides should therefore start from the encounter id and monster id data, then identify the
  class/script branch that calls the BGM selector for that transition.

Confirmed boss/fixed targets from the stock data:

| Target               | Monster Ids  | No-run Encounter Ids | Notes                                                      |
|----------------------|--------------|----------------------|------------------------------------------------------------|
| Astos / Western Keep | `113`        | `125`                | Candidate creepy Castle Cornelia-style override.           |
| Lich                 | `119`, `120` | `122`, `115`         | Includes the Chaos Shrine in the Past version.             |
| Marilith             | `121`, `122` | `121`, `116`         | Includes the Chaos Shrine in the Past version.             |
| Kraken               | `123`, `124` | `120`, `117`         | Includes the Chaos Shrine in the Past version.             |
| Tiamat               | `125`, `126` | `119`, `118`         | Includes the Chaos Shrine in the Past version.             |
| Chaos                | `127`        | `123`                | Use as the first reference model for `a21`-style behavior. |
