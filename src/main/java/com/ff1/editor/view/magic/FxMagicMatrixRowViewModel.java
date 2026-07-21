package com.ff1.editor.view.magic;

import com.ff1.editor.data.MagicClassBit;
import com.ff1.editor.data.MagicMatrixEdit;
import com.ff1.editor.data.MagicSpellSnapshot;
import com.ff1.editor.data.SpellSchool;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public final class FxMagicMatrixRowViewModel {

  private final MagicSpellSnapshot source;
  private final IntegerProperty permissionMask;

  public FxMagicMatrixRowViewModel(MagicSpellSnapshot source) {
    this.source = source;
    permissionMask = new SimpleIntegerProperty(source.permissionMask());
  }

  public int spellId() {
    return source.spellId();
  }

  public String name() {
    return source.name();
  }

  public String description() {
    return source.description();
  }

  public SpellSchool school() {
    return source.school();
  }

  public String schoolName() {
    return school().displayName();
  }

  public int level() {
    return source.level();
  }

  public int slot() {
    return source.slot();
  }

  public String maskHex() {
    return "0x%04x".formatted(permissionMaskValue());
  }

  public String source() {
    return "%s @ 0x%08x".formatted(source.sourceEntry(), source.sourceOffset());
  }

  public String allowedClasses() {
    return MagicClassBit.namesForMask(permissionMaskValue());
  }

  public int permissionMaskValue() {
    return permissionMask.get();
  }

  public void permissionMaskValue(int mask) {
    permissionMask.set(mask);
  }

  public boolean changed() {
    return permissionMaskValue() != source.permissionMask();
  }

  public void reset() {
    permissionMaskValue(source.permissionMask());
  }

  public MagicMatrixEdit toEdit() {
    return MagicMatrixEdit.builder()
        .spellId(source.spellId())
        .permissionMask(permissionMaskValue())
        .build();
  }

  public boolean matches(String query) {
    if (query == null || query.isBlank()) {
      return true;
    }
    String normalized = query.toLowerCase();
    return String.valueOf(spellId()).contains(normalized)
        || name().toLowerCase().contains(normalized)
        || description().toLowerCase().contains(normalized)
        || schoolName().toLowerCase().contains(normalized)
        || allowedClasses().toLowerCase().contains(normalized)
        || source().toLowerCase().contains(normalized)
        || maskHex().contains(normalized);
  }
}
