package com.ff1.editor.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum MonsterArchetype implements MaskOption {
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
}
