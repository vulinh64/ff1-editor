package com.ff1.editor.service;

import com.ff1.editor.data.HeroClassStatsEdit;

public final class HeroClassStatsPatcher {

  public static final String ENTRY_NAME = "cp0";
  public static final int TABLE_OFFSET = 0x3f05;
  public static final int RECORD_SIZE = 10;
  public static final int BASE_CLASS_COUNT = 6;

  private HeroClassStatsPatcher() {}

  public static void apply(byte[] cp0, HeroClassStatsEdit edit) {
    if (edit.classId() < 0 || edit.classId() >= BASE_CLASS_COUNT) {
      throw new IllegalArgumentException(
          "Only base class stats are stored in cp0; class id was " + edit.classId());
    }
    int offset = TABLE_OFFSET + edit.classId() * RECORD_SIZE;
    cp0[offset] = checkedByte(edit.hp(), 127, "HP");
    cp0[offset + 1] = checkedByte(edit.strength(), 99, "STR");
    cp0[offset + 2] = checkedByte(edit.agility(), 99, "AGL");
    cp0[offset + 3] = checkedByte(edit.intelligence(), 99, "INT");
    cp0[offset + 4] = checkedByte(edit.stamina(), 99, "STA");
    cp0[offset + 5] = checkedByte(edit.luck(), 99, "LCK");
  }

  private static byte checkedByte(int value, int max, String label) {
    if (value < 0 || value > max) {
      throw new IllegalArgumentException(
          "%s must be 0..%d for the confirmed cp0 table.".formatted(label, max));
    }
    return (byte) value;
  }
}
