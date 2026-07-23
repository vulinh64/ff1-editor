package com.ff1.editor.service.patcher.data;

import com.ff1.editor.service.*;

/**
 * Patches cp0 spell-charge growth data so every class can eventually reach fifteen charges per
 * spell level.
 */
public final class FifteenSpellChargeGrowthPatcher {

  public static final String ENTRY_NAME = UniversalSpellChargeGrowthPatcher.ENTRY_NAME;
  public static final int MAX_CHARGES = 15;

  private static final int GROWTH_CHUNK_INDEX =
      UniversalSpellChargeGrowthPatcher.GROWTH_CHUNK_INDEX;
  private static final int CLASS_GROUP_COUNT = UniversalSpellChargeGrowthPatcher.CLASS_GROUP_COUNT;
  private static final int LEVEL_UP_ROWS = UniversalSpellChargeGrowthPatcher.LEVEL_UP_ROWS;
  private static final int GROWTH_RECORD_SIZE =
      UniversalSpellChargeGrowthPatcher.GROWTH_RECORD_SIZE;
  private static final int CHARGE_SLOT_START = 6;
  private static final int CHARGE_SLOT_END_EXCLUSIVE = 14;
  private static final int[] CHARGE_GAIN_ROWS = {
    1, 4, 7, 10, 13, 16, 19, 22, 25, 28, 31, 34, 37, 40, 43
  };

  private FifteenSpellChargeGrowthPatcher() {}

  public static DataPatcherState state(byte[] cp0) {
    try {
      Cp0ChunkTable table = new Cp0ChunkTable(cp0);
      validateGrowthChunk(table);
      int chunkOffset = table.chunkOffset(GROWTH_CHUNK_INDEX);
      return isFifteenChargeSchedule(cp0, chunkOffset)
          ? DataPatcherState.PATCHED
          : DataPatcherState.ORIGINAL;
    } catch (RuntimeException _) {
      return DataPatcherState.UNKNOWN;
    }
  }

  public static void apply(byte[] cp0) {
    Cp0ChunkTable table = new Cp0ChunkTable(cp0);
    validateGrowthChunk(table);
    int chunkOffset = table.chunkOffset(GROWTH_CHUNK_INDEX);
    for (int group = 0; group < CLASS_GROUP_COUNT; group++) {
      for (int row = 0; row < LEVEL_UP_ROWS; row++) {
        byte increment = (byte) (isChargeGainRow(row) ? 1 : 0);
        for (int slot = CHARGE_SLOT_START; slot < CHARGE_SLOT_END_EXCLUSIVE; slot++) {
          cp0[growthOffset(chunkOffset, group, row, slot)] = increment;
        }
      }
    }
  }

  private static boolean isFifteenChargeSchedule(byte[] cp0, int chunkOffset) {
    for (int group = 0; group < CLASS_GROUP_COUNT; group++) {
      for (int row = 0; row < LEVEL_UP_ROWS; row++) {
        byte expected = (byte) (isChargeGainRow(row) ? 1 : 0);
        for (int slot = CHARGE_SLOT_START; slot < CHARGE_SLOT_END_EXCLUSIVE; slot++) {
          if (cp0[growthOffset(chunkOffset, group, row, slot)] != expected) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private static boolean isChargeGainRow(int row) {
    for (int chargeGainRow : CHARGE_GAIN_ROWS) {
      if (row == chargeGainRow) {
        return true;
      }
    }
    return false;
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
