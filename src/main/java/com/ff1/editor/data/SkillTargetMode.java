package com.ff1.editor.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum SkillTargetMode implements LabeledValue {
  NONE(0, "No battle target", SkillTarget.NONE, SkillTargetScope.NONE),
  ENEMY_OMNI(1, "Omni/Enemy", SkillTarget.ENEMY, SkillTargetScope.OMNI),
  ENEMY_SINGLE(2, "Single target/Enemy", SkillTarget.ENEMY, SkillTargetScope.SINGLE),
  SELF(4, "Self/Self", SkillTarget.SELF, SkillTargetScope.SELF),
  PARTY_OMNI(8, "Omni/Party", SkillTarget.PARTY, SkillTargetScope.OMNI),
  PARTY_SINGLE(16, "Single target/Party", SkillTarget.PARTY, SkillTargetScope.SINGLE);

  private final int id;
  private final String label;
  private final SkillTarget target;
  private final SkillTargetScope scope;

  public static String displayName(int id) {
    for (SkillTargetMode mode : values()) {
      if (mode.id == id) {
        return mode.label;
      }
    }
    return "Unknown";
  }

  public static SkillTarget target(int id) {
    SkillTargetMode mode = fromId(id);
    return mode == null ? SkillTarget.NONE : mode.target;
  }

  public static SkillTargetScope scope(int id) {
    SkillTargetMode mode = fromId(id);
    return mode == null ? SkillTargetScope.NONE : mode.scope;
  }

  public static int targetModeForScope(int currentTargetMode, SkillTargetScope scope) {
    SkillTargetMode current = fromId(currentTargetMode);
    if (current == null || current == NONE || scope == SkillTargetScope.NONE) {
      return NONE.id;
    }
    if (scope == SkillTargetScope.SELF || current == SELF) {
      return current.id;
    }
    if (current.target == SkillTarget.PARTY) {
      return scope == SkillTargetScope.OMNI ? PARTY_OMNI.id : PARTY_SINGLE.id;
    }
    if (current.target == SkillTarget.ENEMY) {
      return scope == SkillTargetScope.OMNI ? ENEMY_OMNI.id : ENEMY_SINGLE.id;
    }
    return current.id;
  }

  public static int targetModeForTargetAndScope(SkillTarget target, SkillTargetScope scope) {
    SkillTargetScope selectedScope =
        scope == null || scope == SkillTargetScope.NONE || scope == SkillTargetScope.SELF
            ? SkillTargetScope.SINGLE
            : scope;
    if (target == SkillTarget.SELF) {
      return SELF.id;
    }
    if (target == SkillTarget.PARTY) {
      return selectedScope == SkillTargetScope.OMNI ? PARTY_OMNI.id : PARTY_SINGLE.id;
    }
    if (target == SkillTarget.ENEMY) {
      return selectedScope == SkillTargetScope.OMNI ? ENEMY_OMNI.id : ENEMY_SINGLE.id;
    }
    return NONE.id;
  }

  private static SkillTargetMode fromId(int id) {
    for (SkillTargetMode mode : values()) {
      if (mode.id == id) {
        return mode;
      }
    }
    return null;
  }
}
