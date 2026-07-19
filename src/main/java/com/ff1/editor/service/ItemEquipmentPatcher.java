package com.ff1.editor.service;

import com.ff1.editor.data.EquipmentPermissionEdit;

public final class ItemEquipmentPatcher {

  public static final int WEAPON_MASK_OFFSET_IN_RECORD = 2;
  public static final int ARMOR_MASK_OFFSET_IN_RECORD = 0;
  private static final int KNOWN_CLASS_MASK_BITS = 0x3f3f;

  private ItemEquipmentPatcher() {}

  public static void applyPermission(byte[] cp0, EquipmentPermissionEdit edit) {
    if ((edit.permissionMask() & ~KNOWN_CLASS_MASK_BITS) != 0) {
      throw new IllegalArgumentException("Equipment permission mask contains unknown class bits.");
    }
    int offset =
        switch (edit.category()) {
          case WEAPON -> weaponMaskOffset(cp0, edit.itemId());
          case ARMOR -> armorMaskOffset(cp0, edit.itemId());
          default ->
              throw new IllegalArgumentException(
                  "Equipment permission edit must be weapon or armor.");
        };
    cp0[offset] = (byte) ((edit.permissionMask() >>> 8) & 0xff);
    cp0[offset + 1] = (byte) (edit.permissionMask() & 0xff);
  }

  private static int weaponMaskOffset(byte[] cp0, int itemId) {
    int weaponIndex = itemId - ItemEquipmentDiscoveryService.WEAPON_ITEM_ID_OFFSET;
    if (weaponIndex <= 0 || weaponIndex >= ItemEquipmentDiscoveryService.WEAPON_COUNT) {
      throw new IllegalArgumentException("Weapon item id must be 8..47.");
    }
    return new Cp0ChunkTable(cp0).chunkOffset(ItemEquipmentDiscoveryService.WEAPON_CHUNK_INDEX)
        + Short.BYTES
        + weaponIndex * ItemEquipmentDiscoveryService.WEAPON_RECORD_SIZE
        + WEAPON_MASK_OFFSET_IN_RECORD;
  }

  private static int armorMaskOffset(byte[] cp0, int itemId) {
    int armorIndex = itemId - ItemEquipmentDiscoveryService.ARMOR_ITEM_ID_OFFSET;
    if (armorIndex <= 0 || armorIndex >= ItemEquipmentDiscoveryService.ARMOR_COUNT) {
      throw new IllegalArgumentException("Armor item id must be 49..88.");
    }
    return new Cp0ChunkTable(cp0).chunkOffset(ItemEquipmentDiscoveryService.ARMOR_CHUNK_INDEX)
        + Short.BYTES
        + armorIndex * ItemEquipmentDiscoveryService.ARMOR_RECORD_SIZE
        + ARMOR_MASK_OFFSET_IN_RECORD;
  }
}
