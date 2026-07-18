package com.ff1.editor.view.magic;

import com.ff1.editor.data.MagicClassBit;
import com.ff1.editor.data.MagicMatrixEdit;
import com.ff1.editor.data.MagicSpellSnapshot;
import com.ff1.editor.data.SpellSchool;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public final class FxMagicMatrixRowViewModel {

  private final MagicSpellSnapshot source;
  private final BooleanProperty[] classBits = new BooleanProperty[MagicClassBit.values().length];

  public FxMagicMatrixRowViewModel(MagicSpellSnapshot source) {
    this.source = source;
    for (MagicClassBit bit : MagicClassBit.values()) {
      classBits[bit.ordinal()] =
          new SimpleBooleanProperty((source.permissionMask() & bit.mask()) != 0);
    }
  }

  public int spellId() {
    return source.spellId();
  }

  public String name() {
    return source.name();
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
    return "0x%04x".formatted(currentMask());
  }

  public String source() {
    return "%s @ 0x%08x".formatted(source.sourceEntry(), source.sourceOffset());
  }

  public BooleanProperty classBitProperty(MagicClassBit bit) {
    return classBits[bit.ordinal()];
  }

  public boolean changed() {
    return currentMask() != source.permissionMask();
  }

  public void reset() {
    for (MagicClassBit bit : MagicClassBit.values()) {
      classBits[bit.ordinal()].set((source.permissionMask() & bit.mask()) != 0);
    }
  }

  public MagicMatrixEdit toEdit() {
    return MagicMatrixEdit.builder()
        .spellId(source.spellId())
        .permissionMask(currentMask())
        .build();
  }

  public boolean matches(String query) {
    if (query == null || query.isBlank()) {
      return true;
    }
    String normalized = query.toLowerCase();
    return String.valueOf(spellId()).contains(normalized)
        || name().toLowerCase().contains(normalized)
        || schoolName().toLowerCase().contains(normalized)
        || source().toLowerCase().contains(normalized)
        || maskHex().contains(normalized);
  }

  private int currentMask() {
    int mask = 0;
    for (MagicClassBit bit : MagicClassBit.values()) {
      if (classBitProperty(bit).get()) {
        mask |= bit.mask();
      }
    }
    return mask;
  }
}
