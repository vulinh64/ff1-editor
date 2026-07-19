package com.ff1.editor.service.patcher;

import static com.ff1.editor.utils.EditorSupport.readBigEndianUnsignedShort;

import com.ff1.editor.service.*;

/** Patches cp0 so Cornelia armor shop fills its two empty slots with Ribbon and Protect Ring. */
public final class CorneliaArmorShopPatcher {

  public static final String ENTRY_NAME = "cp0";
  public static final int ARMOR_SHOP_CHUNK_INDEX = 8;
  public static final int SHOP_ROW_COUNT = 6;
  public static final int SHOP_ROW_SIZE = 5;
  public static final int CORNELIA_ARMOR_SHOP_ROW = 0;
  public static final int CORNELIA_RIBBON_SLOT = 3;
  public static final int CORNELIA_PROTECT_RING_SLOT = 4;
  public static final int EMPTY_ITEM_ID = 0;
  public static final int RIBBON_ITEM_ID = 80;
  public static final int PROTECT_RING_ITEM_ID = 88;

  private static final int[] EXPECTED_ORIGINAL_CORNELIA_ARMOR = {
    49, 50, 51, EMPTY_ITEM_ID, EMPTY_ITEM_ID
  };
  private static final int[] EXPECTED_PATCHED_CORNELIA_ARMOR = {49, 50, 51, 80, 88};

  public enum State {
    ORIGINAL,
    PATCHED,
    UNKNOWN
  }

  private CorneliaArmorShopPatcher() {}

  public static State state(byte[] cp0) {
    try {
      corneliaArmorShopRowOffset(cp0);
      if (matchesCorneliaRow(cp0, EXPECTED_ORIGINAL_CORNELIA_ARMOR)) {
        return State.ORIGINAL;
      }
      if (matchesCorneliaRow(cp0, EXPECTED_PATCHED_CORNELIA_ARMOR)) {
        return State.PATCHED;
      }
      return State.UNKNOWN;
    } catch (RuntimeException _) {
      return State.UNKNOWN;
    }
  }

  public static void apply(byte[] cp0) {
    int offset = corneliaArmorShopRowOffset(cp0);
    if (!matchesCorneliaRow(cp0, EXPECTED_ORIGINAL_CORNELIA_ARMOR)) {
      throw new IllegalStateException(
          "Cornelia armor shop row does not match the confirmed original inventory.");
    }
    cp0[offset + CORNELIA_RIBBON_SLOT] = (byte) RIBBON_ITEM_ID;
    cp0[offset + CORNELIA_PROTECT_RING_SLOT] = (byte) PROTECT_RING_ITEM_ID;
  }

  public static int corneliaArmorShopRowOffset(byte[] cp0) {
    Cp0ChunkTable table = new Cp0ChunkTable(cp0);
    validateArmorShopChunk(table);
    return table.chunkOffset(ARMOR_SHOP_CHUNK_INDEX)
        + Short.BYTES
        + CORNELIA_ARMOR_SHOP_ROW * SHOP_ROW_SIZE;
  }

  private static boolean matchesCorneliaRow(byte[] cp0, int[] expectedRow) {
    int offset = corneliaArmorShopRowOffset(cp0);
    for (int slot = 0; slot < expectedRow.length; slot++) {
      if ((cp0[offset + slot] & 0xff) != expectedRow[slot]) {
        return false;
      }
    }
    return true;
  }

  private static void validateArmorShopChunk(Cp0ChunkTable table) {
    int expectedLength = Short.BYTES + SHOP_ROW_COUNT * SHOP_ROW_SIZE;
    int actualLength = table.chunkLength(ARMOR_SHOP_CHUNK_INDEX);
    if (actualLength != expectedLength) {
      throw new IllegalStateException(
          "cp0 armor-shop chunk length must be %d bytes; found %d."
              .formatted(expectedLength, actualLength));
    }
    int actualRows = readBigEndianUnsignedShort(table.chunk(ARMOR_SHOP_CHUNK_INDEX), 0);
    if (actualRows != SHOP_ROW_COUNT) {
      throw new IllegalStateException(
          "cp0 armor-shop row count must be %d; found %d.".formatted(SHOP_ROW_COUNT, actualRows));
    }
  }
}
