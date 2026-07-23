package com.ff1.editor.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum SkillEffectKind implements LabeledValue {
  NONE(0, "None"),
  DAMAGE(1, "Damage"),
  UNDEAD_DAMAGE(2, "Undead damage"),
  STATUS_INFLICT(3, "Status inflict"),
  SLEEP_STAGE_DOWN(4, "Sleep stage down"),
  MIND_BLAST(5, "Mind blast"),
  UNUSED_NOOP(6, "Unused/no-op"),
  HEALING(7, "Healing"),
  STATUS_RECOVERY(8, "Status recovery"),
  DEFENSE_UP(9, "Defense up"),
  RESISTANCE_UP(10, "Resistance up"),
  ATTACK_ACCURACY_UP(11, "Attack/accuracy up"),
  HASTE(12, "Haste"),
  ATTACK_UP(13, "Attack up"),
  DEFENSE_DOWN(14, "Defense down"),
  FULL_HEAL_OR_DEATH(15, "Full heal/death"),
  EVASION_UP(16, "Evasion up"),
  RESISTANCE_CLEAR(17, "Resistance clear"),
  CONDITIONAL_STATUS(18, "Conditional status");

  private final int id;
  private final String label;

  public static String displayName(int id) {
    for (SkillEffectKind kind : values()) {
      if (kind.id == id) {
        return kind.label;
      }
    }
    return "Unknown";
  }
}
