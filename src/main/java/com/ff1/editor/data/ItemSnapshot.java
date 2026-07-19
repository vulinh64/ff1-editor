package com.ff1.editor.data;

import lombok.Builder;
import lombok.With;
import org.apache.commons.lang3.StringUtils;

@Builder
@With
public record ItemSnapshot(
    int id,
    String name,
    String description,
    ItemCategory category,
    ArmorSubtype armorSubtype,
    int price,
    int metadataByte1,
    int metadataByte2,
    Integer equipMask,
    String allowedClasses,
    Integer damage,
    Integer accuracy,
    Integer absorb,
    Integer evasionPenalty,
    Integer castSpellId,
    String castSpellName,
    Integer resistanceMask,
    Integer weaponSpecialByte1,
    Integer weaponSpecialByte2,
    String sourceEntry,
    int sourceOffset,
    String notes) {

  public String categoryName() {
    return category == null ? StringUtils.EMPTY : category.displayName();
  }

  public String armorSubtypeName() {
    return armorSubtype == null ? StringUtils.EMPTY : armorSubtype.displayName();
  }
}
