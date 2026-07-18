package com.ff1.editor.view.items;

import com.ff1.editor.data.ItemCategory;
import com.ff1.editor.data.ItemSnapshot;

public final class FxItemRowViewModel {

  private final ItemSnapshot item;

  public FxItemRowViewModel(ItemSnapshot item) {
    this.item = item;
  }

  public int id() {
    return item.id();
  }

  public String name() {
    return item.name();
  }

  public String description() {
    return item.description();
  }

  public ItemCategory category() {
    return item.category();
  }

  public String categoryName() {
    return item.categoryName();
  }

  public String armorSubtype() {
    return item.armorSubtypeName();
  }

  public int price() {
    return item.price();
  }

  public String metadataBytes() {
    return "%d, %d".formatted(item.metadataByte1(), item.metadataByte2());
  }

  public String equipMask() {
    return item.equipMask() == null ? "" : "0x%04x".formatted(item.equipMask());
  }

  public String allowedClasses() {
    return item.allowedClasses();
  }

  public String damage() {
    return format(item.damage());
  }

  public String accuracy() {
    return format(item.accuracy());
  }

  public String absorb() {
    return format(item.absorb());
  }

  public String evasionPenalty() {
    return format(item.evasionPenalty());
  }

  public String castSpell() {
    return item.castSpellLabel();
  }

  public String resistanceMask() {
    return item.resistanceMask() == null || item.resistanceMask() == 0
        ? ""
        : "0x%02x".formatted(item.resistanceMask());
  }

  public String weaponSpecialBytes() {
    if (item.weaponSpecialByte1() == null || item.weaponSpecialByte2() == null) {
      return "";
    }
    return "%d, %d".formatted(item.weaponSpecialByte1(), item.weaponSpecialByte2());
  }

  public String source() {
    return "%s @ 0x%08x".formatted(item.sourceEntry(), item.sourceOffset());
  }

  public String notes() {
    return item.notes();
  }

  public boolean matches(String query) {
    if (query == null || query.isBlank()) {
      return true;
    }
    String normalized = query.toLowerCase();
    return String.valueOf(id()).contains(normalized)
        || name().toLowerCase().contains(normalized)
        || description().toLowerCase().contains(normalized)
        || categoryName().toLowerCase().contains(normalized)
        || armorSubtype().toLowerCase().contains(normalized)
        || allowedClasses().toLowerCase().contains(normalized)
        || castSpell().toLowerCase().contains(normalized)
        || source().toLowerCase().contains(normalized)
        || notes().toLowerCase().contains(normalized);
  }

  private static String format(Integer value) {
    return value == null ? "" : String.valueOf(value);
  }
}
