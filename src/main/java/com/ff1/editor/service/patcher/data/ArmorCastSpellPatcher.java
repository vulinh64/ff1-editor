package com.ff1.editor.service.patcher.data;

import com.ff1.editor.data.ArmorCastSpellEdit;
import com.ff1.editor.service.*;

/** Patches cp0 armor cast-on-use spell ids from the Items editor tab. */
public final class ArmorCastSpellPatcher {

  private ArmorCastSpellPatcher() {}

  public static void apply(byte[] cp0, int chunkOffset, ArmorCastSpellEdit edit) {
    int armorIndex = edit.armorItemId() - ItemEquipmentDiscoveryService.ARMOR_ITEM_ID_OFFSET;
    if (armorIndex <= 0 || armorIndex >= ItemEquipmentDiscoveryService.ARMOR_COUNT) {
      throw new IllegalArgumentException("Armor item id must be 49..88.");
    }
    if (edit.castSpellId() < 0 || edit.castSpellId() >= SkillDiscoveryService.SKILL_COUNT) {
      throw new IllegalArgumentException("Armor cast skill id must be 0..93.");
    }
    int offset =
        chunkOffset
            + Short.BYTES
            + armorIndex * ItemEquipmentDiscoveryService.ARMOR_RECORD_SIZE
            + ItemEquipmentDiscoveryService.ARMOR_CAST_SPELL_OFFSET_IN_RECORD;
    cp0[offset] = (byte) edit.castSpellId();
  }
}
