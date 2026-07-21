package com.ff1.editor.view.items;

import com.ff1.editor.data.ArmorResistance;
import com.ff1.editor.data.ArmorStatsEdit;
import com.ff1.editor.data.EquipmentPermissionEdit;
import com.ff1.editor.data.ItemCategory;
import com.ff1.editor.data.ItemPriceEdit;
import com.ff1.editor.data.ItemSnapshot;
import com.ff1.editor.data.MagicClassBit;
import com.ff1.editor.data.MaskOption;
import com.ff1.editor.data.MonsterArchetype;
import com.ff1.editor.data.MonsterElementAffinity;
import com.ff1.editor.data.WeaponCastSpellEdit;
import com.ff1.editor.data.WeaponStatsEdit;
import java.util.Map;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.commons.lang3.StringUtils;

public final class FxItemRowViewModel {

  public static final String FAMILY_MASK_PATTERN = "%s (0x%02x)";
  public static final String RESISTANCE_BIT_MASK_PATTERN = "0x%02x";
  private final ItemSnapshot item;
  private final Map<Integer, String> skillNames;
  private final Map<Integer, String> elementEffectivenessLabels;
  private final Map<Integer, String> familyEffectivenessLabels;
  private final Map<Integer, String> familyEffectivenessCombinationLabels;
  private final IntegerProperty price;
  private final IntegerProperty castSpellId;
  private final IntegerProperty damage;
  private final IntegerProperty accuracy;
  private final IntegerProperty absorb;
  private final IntegerProperty evasionPenalty;
  private final IntegerProperty armorResistanceMask;
  private final IntegerProperty equipmentPermissionMask;
  private final IntegerProperty weaponAffinityMask;
  private final IntegerProperty weaponFamilyMask;

  public FxItemRowViewModel(
      ItemSnapshot item,
      Map<Integer, String> skillNames,
      Map<Integer, String> elementEffectivenessLabels,
      Map<Integer, String> familyEffectivenessLabels,
      Map<Integer, String> familyEffectivenessCombinationLabels) {
    this.item = item;
    this.skillNames = skillNames;
    this.elementEffectivenessLabels = elementEffectivenessLabels;
    this.familyEffectivenessLabels = familyEffectivenessLabels;
    this.familyEffectivenessCombinationLabels = familyEffectivenessCombinationLabels;
    this.price = new SimpleIntegerProperty(item.price());
    this.castSpellId = new SimpleIntegerProperty(originalCastSpellId());
    this.damage = new SimpleIntegerProperty(originalDamage());
    this.accuracy = new SimpleIntegerProperty(originalAccuracy());
    this.absorb = new SimpleIntegerProperty(originalAbsorb());
    this.evasionPenalty = new SimpleIntegerProperty(originalEvasionPenalty());
    this.armorResistanceMask = new SimpleIntegerProperty(originalArmorResistanceMask());
    this.equipmentPermissionMask = new SimpleIntegerProperty(originalEquipmentPermissionMask());
    this.weaponAffinityMask = new SimpleIntegerProperty(originalWeaponAffinityMask());
    this.weaponFamilyMask = new SimpleIntegerProperty(originalWeaponFamilyMask());
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
    return price.get();
  }

  public IntegerProperty priceProperty() {
    return price;
  }

  public boolean priceChanged() {
    return price.get() != item.price();
  }

  public ItemPriceEdit toItemPriceEdit() {
    return ItemPriceEdit.builder().itemId(id()).price(price.get()).build();
  }

  public String metadataBytes() {
    return "%d, %d".formatted(item.metadataByte1(), item.metadataByte2());
  }

  public String equipMask() {
    return item.equipMask() == null
        ? StringUtils.EMPTY
        : "0x%04x".formatted(equipmentPermissionMask.get());
  }

  public String allowedClasses() {
    return MagicClassBit.namesForMask(equipmentPermissionMask.get());
  }

  public int equipmentPermissionMaskValue() {
    return equipmentPermissionMask.get();
  }

  public void equipmentPermissionMaskValue(int mask) {
    equipmentPermissionMask.set(mask);
  }

  public boolean equipmentPermissionChanged() {
    return (category() == ItemCategory.WEAPON || category() == ItemCategory.ARMOR)
        && equipmentPermissionMask.get() != originalEquipmentPermissionMask();
  }

  public EquipmentPermissionEdit toEquipmentPermissionEdit() {
    return EquipmentPermissionEdit.builder()
        .category(category())
        .itemId(id())
        .permissionMask(equipmentPermissionMask.get())
        .build();
  }

  public String damage() {
    return format(damage.get());
  }

  public IntegerProperty damageProperty() {
    return damage;
  }

  public String accuracy() {
    return format(accuracy.get());
  }

  public IntegerProperty accuracyProperty() {
    return accuracy;
  }

  public boolean weaponStatsChanged() {
    return category() == ItemCategory.WEAPON
        && (damage.get() != originalDamage()
            || accuracy.get() != originalAccuracy()
            || weaponAffinityMask.get() != originalWeaponAffinityMask()
            || weaponFamilyMask.get() != originalWeaponFamilyMask());
  }

  public WeaponStatsEdit toWeaponStatsEdit() {
    return WeaponStatsEdit.builder()
        .weaponItemId(id())
        .damage(damage.get())
        .accuracy(accuracy.get())
        .affinityMask(weaponAffinityMask.get())
        .familyMask(weaponFamilyMask.get())
        .build();
  }

  public IntegerProperty absorbProperty() {
    return absorb;
  }

  public IntegerProperty evasionPenaltyProperty() {
    return evasionPenalty;
  }

  public boolean armorStatsChanged() {
    return category() == ItemCategory.ARMOR
        && (absorb.get() != originalAbsorb()
            || evasionPenalty.get() != originalEvasionPenalty()
            || armorResistanceMask.get() != originalArmorResistanceMask());
  }

  public ArmorStatsEdit toArmorStatsEdit() {
    return ArmorStatsEdit.builder()
        .armorItemId(id())
        .absorb(absorb.get())
        .evasionPenalty(evasionPenalty.get())
        .resistanceMask(armorResistanceMask.get())
        .build();
  }

  public String castSpell() {
    return castSpellId.get() == 0
        ? StringUtils.EMPTY
        : castSpellLabel(castSpellId.get(), skillNames);
  }

  public IntegerProperty castSpellIdProperty() {
    return castSpellId;
  }

  public boolean weaponCastChanged() {
    return category() == ItemCategory.WEAPON && castSpellId.get() != originalCastSpellId();
  }

  public WeaponCastSpellEdit toWeaponCastSpellEdit() {
    return WeaponCastSpellEdit.builder().weaponItemId(id()).castSpellId(castSpellId.get()).build();
  }

  public String resistanceMask() {
    return armorResistanceMask.get() == 0
        ? StringUtils.EMPTY
        : RESISTANCE_BIT_MASK_PATTERN.formatted(armorResistanceMask.get());
  }

  public String armorResistances() {
    return labelsForMask(armorResistanceMask.get(), ArmorResistance.values());
  }

  public int armorResistanceMaskValue() {
    return armorResistanceMask.get();
  }

  public void armorResistanceMaskValue(int mask) {
    armorResistanceMask.set(mask);
  }

  public String weaponAffinityMask() {
    return weaponAffinityMask.get() == 0
        ? StringUtils.EMPTY
        : RESISTANCE_BIT_MASK_PATTERN.formatted(weaponAffinityMask.get());
  }

  public String weaponFamilyMask() {
    return weaponFamilyMask.get() == 0
        ? StringUtils.EMPTY
        : RESISTANCE_BIT_MASK_PATTERN.formatted(weaponFamilyMask.get());
  }

  public String weaponAffinities() {
    return labelsForMask(weaponAffinityMask.get(), MonsterElementAffinity.values());
  }

  public int weaponAffinityMaskValue() {
    return weaponAffinityMask.get();
  }

  public void weaponAffinityMaskValue(int mask) {
    weaponAffinityMask.set(mask);
  }

  public String weaponFamilies() {
    return labelsForMask(weaponFamilyMask.get(), MonsterArchetype.values());
  }

  public int weaponFamilyMaskValue() {
    return weaponFamilyMask.get();
  }

  public void weaponFamilyMaskValue(int mask) {
    weaponFamilyMask.set(mask);
  }

  public String weaponEffectiveness() {
    if (item.weaponSpecialByte1() == null || item.weaponSpecialByte2() == null) {
      return StringUtils.EMPTY;
    }
    return joinNonBlank(
        labelsForMask(weaponAffinityMask.get(), elementEffectivenessLabels, "Affinity"),
        labelsForFamilyMask(weaponFamilyMask.get()));
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
        || armorResistances().toLowerCase().contains(normalized)
        || resistanceMask().toLowerCase().contains(normalized)
        || weaponAffinities().toLowerCase().contains(normalized)
        || weaponFamilies().toLowerCase().contains(normalized)
        || weaponAffinityMask().toLowerCase().contains(normalized)
        || weaponFamilyMask().toLowerCase().contains(normalized)
        || weaponEffectiveness().toLowerCase().contains(normalized)
        || source().toLowerCase().contains(normalized)
        || notes().toLowerCase().contains(normalized);
  }

  private static String format(Integer value) {
    return value == null ? StringUtils.EMPTY : String.valueOf(value);
  }

  private int originalCastSpellId() {
    return item.castSpellId() == null ? 0 : item.castSpellId();
  }

  private int originalDamage() {
    return item.damage() == null ? 0 : item.damage();
  }

  private int originalAccuracy() {
    return item.accuracy() == null ? 0 : item.accuracy();
  }

  private int originalAbsorb() {
    return item.absorb() == null ? 0 : item.absorb();
  }

  private int originalEvasionPenalty() {
    return item.evasionPenalty() == null ? 0 : item.evasionPenalty();
  }

  private int originalArmorResistanceMask() {
    return item.resistanceMask() == null ? 0 : item.resistanceMask();
  }

  private int originalEquipmentPermissionMask() {
    return item.equipMask() == null ? 0 : item.equipMask();
  }

  private int originalWeaponAffinityMask() {
    return item.weaponSpecialByte1() == null ? 0 : item.weaponSpecialByte1();
  }

  private int originalWeaponFamilyMask() {
    return item.weaponSpecialByte2() == null ? 0 : item.weaponSpecialByte2();
  }

  public static String castSpellLabel(int id, Map<Integer, String> skillNames) {
    if (id == 0) {
      return "0 - None";
    }
    String name = skillNames.getOrDefault(id, StringUtils.EMPTY);
    return name.isBlank() ? "%d - Effect %d".formatted(id, id) : "%d - %s".formatted(id, name);
  }

  private String labelsForFamilyMask(int mask) {
    if (mask == 0) {
      return StringUtils.EMPTY;
    }
    String combinationLabel = familyEffectivenessCombinationLabels.get(mask);
    String bitLabels = labelsForMask(mask, familyEffectivenessLabels, "Family");
    if (combinationLabel == null || combinationLabel.isBlank()) {
      return bitLabels;
    }
    if (bitLabels.isBlank()) {
      return FAMILY_MASK_PATTERN.formatted(combinationLabel, mask);
    }
    return "%s; %s".formatted(bitLabels, FAMILY_MASK_PATTERN.formatted(combinationLabel, mask));
  }

  private static String labelsForMask(
      int mask, Map<Integer, String> labels, String fallbackPrefix) {
    if (mask == 0) {
      return StringUtils.EMPTY;
    }
    StringBuilder out = new StringBuilder();
    int unknownBits = mask;
    for (Map.Entry<Integer, String> entry : labels.entrySet()) {
      int bit = entry.getKey();
      if ((mask & bit) == 0) {
        continue;
      }
      append(out, FAMILY_MASK_PATTERN.formatted(entry.getValue(), bit));
      unknownBits &= ~bit;
    }
    for (int bit = 1; bit <= 0x80; bit <<= 1) {
      if ((unknownBits & bit) != 0) {
        append(out, "%s 0x%02x".formatted(fallbackPrefix, bit));
      }
    }
    return out.toString();
  }

  private static String labelsForMask(int mask, MaskOption[] options) {
    if (mask == 0) {
      return StringUtils.EMPTY;
    }
    StringBuilder out = new StringBuilder();
    int unknownBits = mask;
    for (MaskOption option : options) {
      if ((mask & option.bit()) == 0) {
        continue;
      }
      append(out, option.label());
      unknownBits &= ~option.bit();
    }
    for (int bit = 1; bit <= 0x80; bit <<= 1) {
      if ((unknownBits & bit) != 0) {
        append(out, RESISTANCE_BIT_MASK_PATTERN.formatted(bit));
      }
    }
    return out.toString();
  }

  private static String joinNonBlank(String first, String second) {
    if (first == null || first.isBlank()) {
      return second == null ? StringUtils.EMPTY : second;
    }
    if (second == null || second.isBlank()) {
      return first;
    }
    return first + "; " + second;
  }

  private static void append(StringBuilder out, String value) {
    if (!out.isEmpty()) {
      out.append("; ");
    }
    out.append(value);
  }
}
