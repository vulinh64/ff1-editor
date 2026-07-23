package com.ff1.editor.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum MonsterElementAffinity implements MaskOption {
  POISON_STONE(0x01, "Poison/Stone"),
  DEATH(0x02, "Death"),
  STATUS(0x04, "Status"),
  TIME(0x08, "Time"),
  FIRE(0x10, "Fire"),
  ICE(0x20, "Ice"),
  THUNDER(0x40, "Thunder"),
  EARTH(0x80, "Earth");

  private final int bit;
  private final String label;
}
