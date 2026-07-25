package com.ff1.editor.view.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ff1.editor.data.SkillEffectEdit;
import com.ff1.editor.data.SkillSnapshot;
import com.ff1.editor.data.SkillTarget;
import com.ff1.editor.data.SkillTargetScope;
import org.junit.jupiter.api.Test;

class FxSkillRowViewModelTest {

  @Test
  void omniScopeSetsPerTargetAnimationFlag() {
    FxSkillRowViewModel row = new FxSkillRowViewModel(skill(16, 0));

    row.targetScope(SkillTargetScope.OMNI);

    SkillEffectEdit edit = row.toEdit();
    assertEquals(8, edit.targetMode());
    assertEquals(0x02, edit.animationFlags());
  }

  @Test
  void nonOmniScopeClearsOnlyEditorAddedPerTargetAnimationFlag() {
    FxSkillRowViewModel row = new FxSkillRowViewModel(skill(16, 0));

    row.targetScope(SkillTargetScope.OMNI);
    row.targetScope(SkillTargetScope.SINGLE);

    SkillEffectEdit edit = row.toEdit();
    assertEquals(16, edit.targetMode());
    assertEquals(0, edit.animationFlags());
  }

  @Test
  void nonOmniScopePreservesOriginalPerTargetAnimationFlag() {
    FxSkillRowViewModel row = new FxSkillRowViewModel(skill(8, 0x02));

    row.targetScope(SkillTargetScope.SINGLE);

    SkillEffectEdit edit = row.toEdit();
    assertEquals(16, edit.targetMode());
    assertEquals(0x02, edit.animationFlags());
  }

  private static SkillSnapshot skill(int targetMode, int animationFlags) {
    return SkillSnapshot.builder()
        .id(39)
        .name("Temper")
        .learnableLabel("Black LV2.3")
        .price(400)
        .raw0(2)
        .targetMode(targetMode)
        .targetModeName("")
        .target(SkillTarget.PARTY)
        .targetScope(targetMode == 8 ? SkillTargetScope.OMNI : SkillTargetScope.SINGLE)
        .effectKind(13)
        .effectKindName("Buff")
        .powerOrStatus(14)
        .accuracy(0)
        .raw5(6)
        .animationId(0)
        .animationFlags(animationFlags)
        .elementOrStatusMask(0)
        .permissionMask(0)
        .invokers("")
        .sourceEntry("cp0")
        .sourceOffset(0)
        .build();
  }
}
