package com.ff1.editor.data;

import org.apache.commons.lang3.StringUtils;

public enum MagicClassBit implements MaskOption {
  WARRIOR(HeroClass.WARRIOR, 0x0001),
  THIEF(HeroClass.THIEF, 0x0002),
  MONK(HeroClass.MONK, 0x0004),
  RED_MAGE(HeroClass.RED_MAGE, 0x0008),
  WHITE_MAGE(HeroClass.WHITE_MAGE, 0x0010),
  BLACK_MAGE(HeroClass.BLACK_MAGE, 0x0020),
  KNIGHT(HeroClass.KNIGHT, 0x0100),
  NINJA(HeroClass.NINJA, 0x0200),
  MASTER(HeroClass.MASTER, 0x0400),
  RED_WIZARD(HeroClass.RED_WIZARD, 0x0800),
  WHITE_WIZARD(HeroClass.WHITE_WIZARD, 0x1000),
  BLACK_WIZARD(HeroClass.BLACK_WIZARD, 0x2000);

  private final HeroClass heroClass;
  private final int mask;

  MagicClassBit(HeroClass heroClass, int mask) {
    this.heroClass = heroClass;
    this.mask = mask;
  }

  public String displayName() {
    return heroClass.displayName();
  }

  public String label() {
    return displayName();
  }

  public int mask() {
    return mask;
  }

  public int bit() {
    return mask();
  }

  public static String namesForMask(int mask) {
    StringBuilder out = new StringBuilder();
    int count = 0;
    for (MagicClassBit bit : values()) {
      if ((mask & bit.mask()) == 0) {
        continue;
      }
      if (!out.isEmpty()) {
        out.append(", ");
      }
      out.append(bit.displayName());
      count++;
    }
    if (count == 0) {
      return StringUtils.EMPTY;
    }
    return count == values().length ? "All" : out.toString();
  }
}
