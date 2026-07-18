package com.ff1.editor.view.skills;

import com.ff1.editor.data.SkillEffectEdit;
import com.ff1.editor.data.SkillSnapshot;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public final class FxSkillRowViewModel {

  private final SkillSnapshot skill;
  private final IntegerProperty powerOrStatus;
  private final IntegerProperty accuracy;

  public FxSkillRowViewModel(SkillSnapshot skill) {
    this.skill = skill;
    this.powerOrStatus = new SimpleIntegerProperty(skill.powerOrStatus());
    this.accuracy = new SimpleIntegerProperty(skill.accuracy());
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

  public int effectId() {
    return skill.effectId();
  }

  public int effectKind() {
    return skill.effectKind();
  }

  public String effectKindName() {
    return "%s (%d)".formatted(skill.effectKindName(), skill.effectKind());
  }

  public int price() {
    return skill.price();
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
    return hexByte(skill.animationFlags());
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
    return powerOrStatus.get() != skill.powerOrStatus() || accuracy.get() != skill.accuracy();
  }

  public SkillEffectEdit toEdit() {
    return SkillEffectEdit.builder()
        .skillId(skill.id())
        .powerOrStatus(powerOrStatus.get())
        .accuracy(accuracy.get())
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
        || String.valueOf(effectId()).contains(normalized)
        || String.valueOf(effectKind()).contains(normalized)
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
}
