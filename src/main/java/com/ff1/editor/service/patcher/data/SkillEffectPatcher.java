package com.ff1.editor.service.patcher.data;

import com.ff1.editor.data.SkillEffectEdit;
import com.ff1.editor.service.*;

/**
 * Patches cp0 spell and effect price, power/status, and accuracy fields from the Skills editor tab.
 */
public final class SkillEffectPatcher {

  private SkillEffectPatcher() {}

  public static void apply(byte[] cp0, int chunkOffset, SkillEffectEdit edit) {
    if (edit.skillId() < 0 || edit.skillId() >= SkillDiscoveryService.SKILL_COUNT) {
      throw new IllegalArgumentException("Skill/effect id must be 0..93.");
    }
    if (edit.price() < 0 || edit.price() > 0xffff) {
      throw new IllegalArgumentException("Skill/effect price must be 0..65535.");
    }
    validateByte(edit.powerOrStatus(), "Power/status");
    validateByte(edit.accuracy(), "Accuracy");
    int offset =
        chunkOffset + Short.BYTES + edit.skillId() * SkillDiscoveryService.SPELL_RECORD_SIZE;
    cp0[offset + SkillDiscoveryService.PRICE_OFFSET_IN_RECORD] = (byte) (edit.price() >>> 8);
    cp0[offset + SkillDiscoveryService.PRICE_OFFSET_IN_RECORD + 1] = (byte) edit.price();
    cp0[offset + SkillDiscoveryService.POWER_OR_STATUS_OFFSET_IN_RECORD] =
        (byte) edit.powerOrStatus();
    cp0[offset + SkillDiscoveryService.ACCURACY_OFFSET_IN_RECORD] = (byte) edit.accuracy();
  }

  private static void validateByte(int value, String label) {
    if (value < 0 || value > 255) {
      throw new IllegalArgumentException(label + " must be 0..255.");
    }
  }
}
