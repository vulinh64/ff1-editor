package com.ff1.editor.service.patcher;

import com.ff1.editor.data.WeaponCastSpellEdit;
import com.ff1.editor.service.*;

/** Patches cp0 weapon cast-on-use spell ids from the Items editor tab. */
public final class WeaponCastSpellPatcher {

  private WeaponCastSpellPatcher() {}

  public static void apply(byte[] cp0, int chunkOffset, WeaponCastSpellEdit edit) {
    int weaponIndex = edit.weaponItemId() - ItemEquipmentDiscoveryService.WEAPON_ITEM_ID_OFFSET;
    if (weaponIndex < 0 || weaponIndex >= ItemEquipmentDiscoveryService.WEAPON_COUNT) {
      throw new IllegalArgumentException("Weapon item id must be 7..47.");
    }
    if (edit.castSpellId() < 0 || edit.castSpellId() >= SkillDiscoveryService.SKILL_COUNT) {
      throw new IllegalArgumentException("Weapon cast skill id must be 0..93.");
    }
    int offset =
        chunkOffset
            + Short.BYTES
            + weaponIndex * ItemEquipmentDiscoveryService.WEAPON_RECORD_SIZE
            + ItemEquipmentDiscoveryService.WEAPON_CAST_SPELL_OFFSET_IN_RECORD;
    cp0[offset] = (byte) edit.castSpellId();
  }
}
