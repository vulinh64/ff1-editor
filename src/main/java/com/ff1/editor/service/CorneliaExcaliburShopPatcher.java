package com.ff1.editor.service;

public final class CorneliaExcaliburShopPatcher {

  public static final String ENTRY_NAME = "cp0";
  public static final int CORNELIA_NUNCHAKU_SLOT = 0;
  public static final int NUNCHAKU_ITEM_ID = 8;
  public static final int EXCALIBUR_ITEM_ID = 46;

  private static final int[] EXPECTED_CORNELIA_WEAPONS = {
    NUNCHAKU_ITEM_ID, CorneliaWeaponShopPatcher.KNIFE_ITEM_ID, 10, 11, 12
  };

  public enum State {
    ORIGINAL,
    PATCHED,
    UNKNOWN
  }

  private CorneliaExcaliburShopPatcher() {}

  public static State state(byte[] cp0) {
    try {
      corneliaNunchakuOffset(cp0);
      if (matchesCorneliaRow(cp0, NUNCHAKU_ITEM_ID)) {
        return State.ORIGINAL;
      }
      if (matchesCorneliaRow(cp0, EXCALIBUR_ITEM_ID)) {
        return State.PATCHED;
      }
      return State.UNKNOWN;
    } catch (RuntimeException _) {
      return State.UNKNOWN;
    }
  }

  public static void apply(byte[] cp0) {
    int offset = corneliaNunchakuOffset(cp0);
    if (!matchesCorneliaRow(cp0, NUNCHAKU_ITEM_ID)) {
      throw new IllegalStateException(
          "Cornelia weapon shop row does not match the confirmed original Nunchaku inventory.");
    }
    cp0[offset] = (byte) EXCALIBUR_ITEM_ID;
  }

  public static int corneliaNunchakuOffset(byte[] cp0) {
    return CorneliaWeaponShopPatcher.corneliaWeaponShopRowOffset(cp0) + CORNELIA_NUNCHAKU_SLOT;
  }

  private static boolean matchesCorneliaRow(byte[] cp0, int slotItemId) {
    int offset = corneliaNunchakuOffset(cp0) - CORNELIA_NUNCHAKU_SLOT;
    for (int slot = 0; slot < EXPECTED_CORNELIA_WEAPONS.length; slot++) {
      int expected = slot == CORNELIA_NUNCHAKU_SLOT ? slotItemId : EXPECTED_CORNELIA_WEAPONS[slot];
      int actual = cp0[offset + slot] & 0xff;
      if (slot == CorneliaWeaponShopPatcher.CORNELIA_KNIFE_SLOT
          && actual == CorneliaWeaponShopPatcher.MASAMUNE_ITEM_ID) {
        continue;
      }
      if (actual != expected) {
        return false;
      }
    }
    return true;
  }
}
