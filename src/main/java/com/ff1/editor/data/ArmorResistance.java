package com.ff1.editor.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ArmorResistance implements MaskOption {
  POISON_STATUS(0x01, "Poison/Status"),
  STONE(0x02, "Stone"),
  TIME_STOP(0x04, "Time/Stop"),
  DEATH(0x08, "Death"),
  FIRE(0x10, "Fire"),
  ICE(0x20, "Ice"),
  THUNDER(0x40, "Thunder"),
  EARTH(0x80, "Earth");

  private final int bit;
  private final String label;
}
