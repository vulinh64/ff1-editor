package com.ff1.editor.service;

import static com.ff1.editor.utils.EditorSupport.readBigEndianUnsignedShort;

public final class CorneliaWeaponShopPatcher {

  public static final String ENTRY_NAME = "cp0";
  public static final int WEAPON_SHOP_CHUNK_INDEX = 7;
  public static final int SHOP_ROW_COUNT = 6;
  public static final int SHOP_ROW_SIZE = 5;
  public static final int CORNELIA_WEAPON_SHOP_ROW = 0;
  public static final int CORNELIA_KNIFE_SLOT = 1;
  public static final int KNIFE_ITEM_ID = 9;
  public static final int MASAMUNE_ITEM_ID = 47;

  private static final int[] EXPECTED_CORNELIA_WEAPONS = {8, KNIFE_ITEM_ID, 10, 11, 12};

  public enum State {
    ORIGINAL,
    PATCHED,
    UNKNOWN
  }

  private CorneliaWeaponShopPatcher() {}

  public static State state(byte[] cp0) {
    try {
      corneliaKnifeOffset(cp0);
      if (matchesCorneliaRow(cp0, KNIFE_ITEM_ID)) {
        return State.ORIGINAL;
      }
      if (matchesCorneliaRow(cp0, MASAMUNE_ITEM_ID)) {
        return State.PATCHED;
      }
      return State.UNKNOWN;
    } catch (RuntimeException _) {
      return State.UNKNOWN;
    }
  }

  public static void apply(byte[] cp0) {
    int offset = corneliaKnifeOffset(cp0);
    if (!matchesCorneliaRow(cp0, KNIFE_ITEM_ID)) {
      throw new IllegalStateException(
          "Cornelia weapon shop row does not match the confirmed original Knife inventory.");
    }
    cp0[offset] = (byte) MASAMUNE_ITEM_ID;
  }

  public static int corneliaKnifeOffset(byte[] cp0) {
    return corneliaWeaponShopRowOffset(cp0) + CORNELIA_KNIFE_SLOT;
  }

  public static int corneliaWeaponShopRowOffset(byte[] cp0) {
    Cp0ChunkTable table = new Cp0ChunkTable(cp0);
    validateWeaponShopChunk(table);
    return table.chunkOffset(WEAPON_SHOP_CHUNK_INDEX)
        + Short.BYTES
        + CORNELIA_WEAPON_SHOP_ROW * SHOP_ROW_SIZE;
  }

  private static boolean matchesCorneliaRow(byte[] cp0, int slotItemId) {
    int offset = corneliaKnifeOffset(cp0) - CORNELIA_KNIFE_SLOT;
    for (int slot = 0; slot < EXPECTED_CORNELIA_WEAPONS.length; slot++) {
      int expected = slot == CORNELIA_KNIFE_SLOT ? slotItemId : EXPECTED_CORNELIA_WEAPONS[slot];
      int actual = cp0[offset + slot] & 0xff;
      if (slot == CorneliaExcaliburShopPatcher.CORNELIA_NUNCHAKU_SLOT
          && actual == CorneliaExcaliburShopPatcher.EXCALIBUR_ITEM_ID) {
        continue;
      }
      if (actual != expected) {
        return false;
      }
    }
    return true;
  }

  private static void validateWeaponShopChunk(Cp0ChunkTable table) {
    int expectedLength = Short.BYTES + SHOP_ROW_COUNT * SHOP_ROW_SIZE;
    int actualLength = table.chunkLength(WEAPON_SHOP_CHUNK_INDEX);
    if (actualLength != expectedLength) {
      throw new IllegalStateException(
          "cp0 weapon-shop chunk length must be %d bytes; found %d."
              .formatted(expectedLength, actualLength));
    }
    int actualRows = readBigEndianUnsignedShort(table.chunk(WEAPON_SHOP_CHUNK_INDEX), 0);
    if (actualRows != SHOP_ROW_COUNT) {
      throw new IllegalStateException(
          "cp0 weapon-shop row count must be %d; found %d.".formatted(SHOP_ROW_COUNT, actualRows));
    }
  }
}
