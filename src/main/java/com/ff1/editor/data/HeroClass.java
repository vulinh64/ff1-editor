package com.ff1.editor.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum HeroClass implements LabeledValue {
  WARRIOR(0, "Warrior", -1),
  THIEF(1, "Thief", -1),
  MONK(2, "Monk", -1),
  RED_MAGE(3, "Red Mage", -1),
  WHITE_MAGE(4, "White Mage", -1),
  BLACK_MAGE(5, "Black Mage", -1),
  KNIGHT(6, "Knight", 0),
  NINJA(7, "Ninja", 1),
  MASTER(8, "Master", 2),
  RED_WIZARD(9, "Red Wizard", 3),
  WHITE_WIZARD(10, "White Wizard", 4),
  BLACK_WIZARD(11, "Black Wizard", 5);

  private final int id;
  private final String label;
  private final int upgradeFromId;

  public boolean upgraded() {
    return upgradeFromId >= 0;
  }
}
