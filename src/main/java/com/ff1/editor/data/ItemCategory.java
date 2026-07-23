package com.ff1.editor.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ItemCategory implements LabeledValue {
  CONSUMABLE("Consumable"),
  WEAPON("Weapon"),
  ARMOR("Armor"),
  KEY_ITEM("Key Item"),
  BLANK("Blank"),
  UNKNOWN("Unknown");

  private final String label;
}
