package com.ff1.editor.data;

public enum ItemCategory {
  CONSUMABLE("Consumable"),
  WEAPON("Weapon"),
  ARMOR("Armor"),
  KEY_ITEM("Key Item"),
  BLANK("Blank"),
  UNKNOWN("Unknown");

  private final String displayName;

  ItemCategory(String displayName) {
    this.displayName = displayName;
  }

  public String displayName() {
    return displayName;
  }
}
