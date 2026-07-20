package com.ff1.editor.data;

public enum MonsterArchetype implements MonsterMaskOption {
  MAGICAL(0x01, "Magical"),
  DRAGON(0x02, "Dragon"),
  GIANT_OGRE_GOBLIN(0x04, "Giant/Ogre/Goblin"),
  UNDEAD(0x08, "Undead"),
  WEREBEAST(0x10, "Werebeast"),
  AQUATIC(0x20, "Aquatic"),
  MAGE(0x40, "Mage"),
  REGENERATIVE(0x80, "Regenerative");

  private final int bit;
  private final String label;

  MonsterArchetype(int bit, String label) {
    this.bit = bit;
    this.label = label;
  }

  public int bit() {
    return bit;
  }

  public String label() {
    return label;
  }
}
