package com.ff1.editor.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum AudioResource implements LabeledValue {
  A0("a0", "Key item received theme"),
  A1("a1", "Final Fantasy main theme"),
  A2("a2", "Castle Cornelia theme"),
  A3("a3", "Overworld theme"),
  A4("a4", "Chaos Shrine theme"),
  A5("a5", "Matoya's Cave theme"),
  A6("a6", "Town theme"),
  A7("a7", "Shop theme"),
  A8("a8", "Sea theme"),
  A9("a9", "Chaos Shrine in the Past theme"),
  A10("a10", "Marsh Cave theme"),
  A11("a11", "Interface theme"),
  A12("a12", "Airship theme"),
  A13("a13", "Volcano theme"),
  A14("a14", "Flying Fortress theme"),
  A15("a15", "Normal battle theme"),
  A16("a16", "Victory theme"),
  A17("a17", "Credit theme"),
  A18("a18", "Game over theme"),
  A19("a19", "Inn sleep theme"),
  A20("a20", "Main menu theme"),
  A21("a21", "Chaos battle-like theme");

  private final String entryName;
  private final String label;
}
