package com.ff1.editor.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SkillTargetModeTest {

  @Test
  void partyAndEnemyScopesCanToggleOnlySingleAndOmni() {
    assertEquals(8, SkillTargetMode.targetModeForScope(16, SkillTargetScope.OMNI));
    assertEquals(16, SkillTargetMode.targetModeForScope(8, SkillTargetScope.SINGLE));
    assertEquals(1, SkillTargetMode.targetModeForScope(2, SkillTargetScope.OMNI));
    assertEquals(2, SkillTargetMode.targetModeForScope(1, SkillTargetScope.SINGLE));
    assertEquals(16, SkillTargetMode.targetModeForScope(16, SkillTargetScope.SELF));
  }

  @Test
  void selfTargetCanMoveOnlyToSelfOrParty() {
    assertEquals(
        4, SkillTargetMode.targetModeForTargetAndScope(SkillTarget.SELF, SkillTargetScope.OMNI));
    assertEquals(
        16, SkillTargetMode.targetModeForTargetAndScope(SkillTarget.PARTY, SkillTargetScope.SELF));
    assertEquals(
        8, SkillTargetMode.targetModeForTargetAndScope(SkillTarget.PARTY, SkillTargetScope.OMNI));
  }
}
