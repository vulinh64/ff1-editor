package com.ff1.editor.service;

import com.ff1.editor.data.SkillEffectEdit;

public final class SkillEffectPatcher {

  private SkillEffectPatcher() {}

  public static void apply(byte[] cp0, int chunkOffset, SkillEffectEdit edit) {
    if (edit.skillId() < 0 || edit.skillId() >= SkillDiscoveryService.SKILL_COUNT) {
      throw new IllegalArgumentException("Skill/effect id must be 0..93.");
    }
    validateByte(edit.powerOrStatus(), "Power/status");
    validateByte(edit.accuracy(), "Accuracy");
    int offset =
        chunkOffset + Short.BYTES + edit.skillId() * SkillDiscoveryService.SPELL_RECORD_SIZE;
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
