package com.ff1.editor.view.equipment;

import com.ff1.editor.data.EquipmentPermissionEdit;
import com.ff1.editor.data.EquipmentPermissionSnapshot;
import com.ff1.editor.data.ItemCategory;
import com.ff1.editor.data.MagicClassBit;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public final class FxEquipmentMatrixRowViewModel {

  private final EquipmentPermissionSnapshot source;
  private final BooleanProperty[] classBits = new BooleanProperty[MagicClassBit.values().length];

  public FxEquipmentMatrixRowViewModel(EquipmentPermissionSnapshot source) {
    this.source = source;
    for (MagicClassBit bit : MagicClassBit.values()) {
      classBits[bit.ordinal()] =
          new SimpleBooleanProperty((source.permissionMask() & bit.mask()) != 0);
    }
  }

  public ItemCategory category() {
    return source.category();
  }

  public int itemId() {
    return source.itemId();
  }

  public String name() {
    return source.name();
  }

  public String subtype() {
    return source.subtype();
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

  public EquipmentPermissionEdit toEdit() {
    return EquipmentPermissionEdit.builder()
        .category(source.category())
        .itemId(source.itemId())
        .permissionMask(currentMask())
        .build();
  }

  public boolean matches(String query) {
    if (query == null || query.isBlank()) {
      return true;
    }
    String normalized = query.toLowerCase();
    return String.valueOf(itemId()).contains(normalized)
        || name().toLowerCase().contains(normalized)
        || subtype().toLowerCase().contains(normalized)
        || maskHex().contains(normalized)
        || source().toLowerCase().contains(normalized);
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
