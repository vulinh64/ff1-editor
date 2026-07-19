package com.ff1.editor.service.patcher;

import com.ff1.editor.service.*;

/** Patches cp0 spell-charge growth data so all class groups use the strong mage charge template. */
public final class UniversalSpellChargeGrowthPatcher {

  public static final String ENTRY_NAME = "cp0";
  public static final int GROWTH_CHUNK_INDEX = 4;
  public static final int CLASS_GROUP_COUNT = 6;
  public static final int LEVEL_UP_ROWS = 49;
  public static final int GROWTH_RECORD_SIZE = 14;

  private static final int CHARGE_SLOT_START = 6;
  private static final int CHARGE_SLOT_END_EXCLUSIVE = 14;
  private static final int WHITE_MAGE_GROUP = 4;
  private static final int BLACK_MAGE_GROUP = 5;

  public enum State {
    ORIGINAL,
    PATCHED,
    UNKNOWN
  }

  private UniversalSpellChargeGrowthPatcher() {}

  public static State state(byte[] cp0) {
    try {
      Cp0ChunkTable table = new Cp0ChunkTable(cp0);
      validateGrowthChunk(table);
      int chunkOffset = table.chunkOffset(GROWTH_CHUNK_INDEX);
      return isUniversal(cp0, chunkOffset) ? State.PATCHED : State.ORIGINAL;
    } catch (RuntimeException _) {
      return State.UNKNOWN;
    }
  }

  public static void apply(byte[] cp0) {
    Cp0ChunkTable table = new Cp0ChunkTable(cp0);
    validateGrowthChunk(table);
    int chunkOffset = table.chunkOffset(GROWTH_CHUNK_INDEX);
    for (int row = 0; row < LEVEL_UP_ROWS; row++) {
      for (int slot = CHARGE_SLOT_START; slot < CHARGE_SLOT_END_EXCLUSIVE; slot++) {
        byte template = strongerTemplateCharge(cp0, chunkOffset, row, slot);
        for (int group = 0; group < CLASS_GROUP_COUNT; group++) {
          cp0[growthOffset(chunkOffset, group, row, slot)] = template;
        }
      }
    }
  }

  private static boolean isUniversal(byte[] cp0, int chunkOffset) {
    for (int row = 0; row < LEVEL_UP_ROWS; row++) {
      for (int slot = CHARGE_SLOT_START; slot < CHARGE_SLOT_END_EXCLUSIVE; slot++) {
        byte template = strongerTemplateCharge(cp0, chunkOffset, row, slot);
        for (int group = 0; group < CLASS_GROUP_COUNT; group++) {
          if (cp0[growthOffset(chunkOffset, group, row, slot)] != template) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private static byte strongerTemplateCharge(byte[] cp0, int chunkOffset, int row, int slot) {
    int white = cp0[growthOffset(chunkOffset, WHITE_MAGE_GROUP, row, slot)] & 0xff;
    int black = cp0[growthOffset(chunkOffset, BLACK_MAGE_GROUP, row, slot)] & 0xff;
    return (byte) Math.max(white, black);
  }

  private static void validateGrowthChunk(Cp0ChunkTable table) {
    int expectedLength = CLASS_GROUP_COUNT * LEVEL_UP_ROWS * GROWTH_RECORD_SIZE;
    int actualLength = table.chunkLength(GROWTH_CHUNK_INDEX);
    if (actualLength != expectedLength) {
      throw new IllegalStateException(
          "cp0 growth chunk length must be %d bytes; found %d."
              .formatted(expectedLength, actualLength));
    }
  }

  private static int growthOffset(int chunkOffset, int group, int row, int slot) {
    return chunkOffset
        + group * LEVEL_UP_ROWS * GROWTH_RECORD_SIZE
        + row * GROWTH_RECORD_SIZE
        + slot;
  }
}
