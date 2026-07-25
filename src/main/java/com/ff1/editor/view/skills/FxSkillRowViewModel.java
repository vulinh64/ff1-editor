package com.ff1.editor.view.skills;

import com.ff1.editor.data.SkillEffectEdit;
import com.ff1.editor.data.SkillSnapshot;
import com.ff1.editor.data.SkillTarget;
import com.ff1.editor.data.SkillTargetMode;
import com.ff1.editor.data.SkillTargetScope;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

public final class FxSkillRowViewModel {

  private static final int OMNI_PER_TARGET_ANIMATION_FLAG = 0x02;

  private final SkillSnapshot skill;
  private final IntegerProperty price;
  private final IntegerProperty targetMode;
  private final ObjectProperty<SkillTarget> target;
  private final ObjectProperty<SkillTargetScope> targetScope;
  private final IntegerProperty powerOrStatus;
  private final IntegerProperty accuracy;
  private final IntegerProperty animationFlags;

  public FxSkillRowViewModel(SkillSnapshot skill) {
    this.skill = skill;
    this.price = new SimpleIntegerProperty(skill.price());
    this.targetMode = new SimpleIntegerProperty(skill.targetMode());
    this.target = new SimpleObjectProperty<>(skill.target());
    this.targetScope = new SimpleObjectProperty<>(skill.targetScope());
    this.powerOrStatus = new SimpleIntegerProperty(skill.powerOrStatus());
    this.accuracy = new SimpleIntegerProperty(skill.accuracy());
    this.animationFlags = new SimpleIntegerProperty(skill.animationFlags());
  }

  public int id() {
    return skill.id();
  }

  public String name() {
    return skill.name();
  }

  public String learnableLabel() {
    return skill.learnableLabel();
  }

  public int targetMode() {
    return targetMode.get();
  }

  public String targetModeName() {
    return SkillTargetMode.displayName(targetMode.get());
  }

  public String targetName() {
    return target.get().label();
  }

  public ObjectProperty<SkillTarget> targetProperty() {
    return target;
  }

  public ObjectProperty<SkillTargetScope> targetScopeProperty() {
    return targetScope;
  }

  public int effectKind() {
    return skill.effectKind();
  }

  public String effectKindName() {
    return skill.effectKindName();
  }

  public IntegerProperty priceProperty() {
    return price;
  }

  public int raw0() {
    return skill.raw0();
  }

  public IntegerProperty powerOrStatusProperty() {
    return powerOrStatus;
  }

  public IntegerProperty accuracyProperty() {
    return accuracy;
  }

  public int raw5() {
    return skill.raw5();
  }

  public int animationId() {
    return skill.animationId();
  }

  public String animationFlags() {
    return hexByte(animationFlags.get());
  }

  public String elementOrStatusMask() {
    return hexByte(skill.elementOrStatusMask());
  }

  public String permissionMask() {
    return "0x%04x".formatted(skill.permissionMask());
  }

  public String invokers() {
    return skill.invokers();
  }

  public String source() {
    return "%s @ 0x%08x".formatted(skill.sourceEntry(), skill.sourceOffset());
  }

  public boolean internalOnly() {
    return skill.learnableLabel().isBlank();
  }

  public boolean invoked() {
    return !skill.invokers().isBlank();
  }

  public boolean changed() {
    return price.get() != skill.price()
        || targetMode.get() != skill.targetMode()
        || powerOrStatus.get() != skill.powerOrStatus()
        || accuracy.get() != skill.accuracy()
        || animationFlags.get() != skill.animationFlags();
  }

  public boolean targetEditable() {
    return skill.targetMode() == 4;
  }

  public boolean affectsEditable() {
    return targetMode.get() != 0 && targetMode.get() != 4;
  }

  public void target(SkillTarget target) {
    if (!targetEditable() || target == null || target == SkillTarget.ENEMY) {
      return;
    }
    int nextMode = SkillTargetMode.targetModeForTargetAndScope(target, targetScope.get());
    targetMode.set(nextMode);
    this.target.set(SkillTargetMode.target(nextMode));
    targetScope.set(SkillTargetMode.scope(nextMode));
    updateAnimationFlagsForTargetMode();
  }

  public void targetScope(SkillTargetScope scope) {
    if (!affectsEditable() || scope == SkillTargetScope.SELF) {
      return;
    }
    SkillTargetScope next = scope == null ? targetScope.get() : scope;
    targetMode.set(SkillTargetMode.targetModeForScope(targetMode.get(), next));
    target.set(SkillTargetMode.target(targetMode.get()));
    targetScope.set(SkillTargetMode.scope(targetMode.get()));
    updateAnimationFlagsForTargetMode();
  }

  public SkillEffectEdit toEdit() {
    return SkillEffectEdit.builder()
        .skillId(skill.id())
        .price(price.get())
        .targetMode(targetMode.get())
        .powerOrStatus(powerOrStatus.get())
        .accuracy(accuracy.get())
        .animationFlags(animationFlags.get())
        .build();
  }

  public boolean matches(String query) {
    if (query == null || query.isBlank()) {
      return true;
    }
    String normalized = query.toLowerCase();
    return String.valueOf(id()).contains(normalized)
        || name().toLowerCase().contains(normalized)
        || learnableLabel().toLowerCase().contains(normalized)
        || String.valueOf(targetMode()).contains(normalized)
        || targetModeName().toLowerCase().contains(normalized)
        || targetName().toLowerCase().contains(normalized)
        || targetScope.get().label().toLowerCase().contains(normalized)
        || String.valueOf(effectKind()).contains(normalized)
        || String.valueOf(price.get()).contains(normalized)
        || String.valueOf(powerOrStatus.get()).contains(normalized)
        || String.valueOf(accuracy.get()).contains(normalized)
        || effectKindName().toLowerCase().contains(normalized)
        || permissionMask().toLowerCase().contains(normalized)
        || invokers().toLowerCase().contains(normalized)
        || source().toLowerCase().contains(normalized);
  }

  private static String hexByte(int value) {
    return "0x%02x".formatted(value);
  }

  private void updateAnimationFlagsForTargetMode() {
    if (targetScope.get() == SkillTargetScope.OMNI) {
      animationFlags.set(animationFlags.get() | OMNI_PER_TARGET_ANIMATION_FLAG);
    } else if ((skill.animationFlags() & OMNI_PER_TARGET_ANIMATION_FLAG) == 0) {
      animationFlags.set(animationFlags.get() & ~OMNI_PER_TARGET_ANIMATION_FLAG);
    }
  }
}
