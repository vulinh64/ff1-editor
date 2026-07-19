package com.ff1.editor.service.patcher;

import com.ff1.editor.data.ArmorStatsEdit;
import com.ff1.editor.data.EquipmentPermissionEdit;
import com.ff1.editor.data.WeaponStatsEdit;
import com.ff1.editor.service.*;

/** Patches cp0 weapon and armor stat fields plus equip permission masks from editor tabs. */
public final class ItemEquipmentPatcher {

  public static final int WEAPON_MASK_OFFSET_IN_RECORD = 2;
  public static final int WEAPON_DAMAGE_OFFSET_IN_RECORD = 4;
  public static final int WEAPON_ACCURACY_OFFSET_IN_RECORD = 5;
  public static final int ARMOR_MASK_OFFSET_IN_RECORD = 0;
  public static final int ARMOR_ABSORB_OFFSET_IN_RECORD = 2;
  public static final int ARMOR_EVASION_PENALTY_OFFSET_IN_RECORD = 3;
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

  public static void applyWeaponStats(byte[] cp0, WeaponStatsEdit edit) {
    validateByte(edit.damage(), "Weapon damage");
    validateByte(edit.accuracy(), "Weapon accuracy");
    int offset = weaponRecordOffset(cp0, edit.weaponItemId());
    cp0[offset + WEAPON_DAMAGE_OFFSET_IN_RECORD] = (byte) edit.damage();
    cp0[offset + WEAPON_ACCURACY_OFFSET_IN_RECORD] = (byte) edit.accuracy();
  }

  public static void applyArmorStats(byte[] cp0, ArmorStatsEdit edit) {
    validateByte(edit.absorb(), "Armor absorb");
    validateByte(edit.evasionPenalty(), "Armor evasion lower");
    int offset = armorRecordOffset(cp0, edit.armorItemId());
    cp0[offset + ARMOR_ABSORB_OFFSET_IN_RECORD] = (byte) edit.absorb();
    cp0[offset + ARMOR_EVASION_PENALTY_OFFSET_IN_RECORD] = (byte) edit.evasionPenalty();
  }

  private static int weaponMaskOffset(byte[] cp0, int itemId) {
    return weaponRecordOffset(cp0, itemId) + WEAPON_MASK_OFFSET_IN_RECORD;
  }

  private static int weaponRecordOffset(byte[] cp0, int itemId) {
    int weaponIndex = itemId - ItemEquipmentDiscoveryService.WEAPON_ITEM_ID_OFFSET;
    if (weaponIndex <= 0 || weaponIndex >= ItemEquipmentDiscoveryService.WEAPON_COUNT) {
      throw new IllegalArgumentException("Weapon item id must be 8..47.");
    }
    return new Cp0ChunkTable(cp0).chunkOffset(ItemEquipmentDiscoveryService.WEAPON_CHUNK_INDEX)
        + Short.BYTES
        + weaponIndex * ItemEquipmentDiscoveryService.WEAPON_RECORD_SIZE;
  }

  private static int armorMaskOffset(byte[] cp0, int itemId) {
    return armorRecordOffset(cp0, itemId) + ARMOR_MASK_OFFSET_IN_RECORD;
  }

  private static int armorRecordOffset(byte[] cp0, int itemId) {
    int armorIndex = itemId - ItemEquipmentDiscoveryService.ARMOR_ITEM_ID_OFFSET;
    if (armorIndex <= 0 || armorIndex >= ItemEquipmentDiscoveryService.ARMOR_COUNT) {
      throw new IllegalArgumentException("Armor item id must be 49..88.");
    }
    return new Cp0ChunkTable(cp0).chunkOffset(ItemEquipmentDiscoveryService.ARMOR_CHUNK_INDEX)
        + Short.BYTES
        + armorIndex * ItemEquipmentDiscoveryService.ARMOR_RECORD_SIZE;
  }

  private static void validateByte(int value, String label) {
    if (value < 0 || value > 255) {
      throw new IllegalArgumentException(label + " must be 0..255.");
    }
  }
}
