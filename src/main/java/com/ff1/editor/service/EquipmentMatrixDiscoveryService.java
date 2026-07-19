package com.ff1.editor.service;

import com.ff1.editor.data.EquipmentPermissionSnapshot;
import com.ff1.editor.data.ItemCategory;
import com.ff1.editor.data.ItemSnapshot;
import java.nio.file.Path;
import java.util.List;

public final class EquipmentMatrixDiscoveryService {

  private final Path workDir;

  public EquipmentMatrixDiscoveryService(Path workDir) {
    this.workDir = workDir;
  }

  public List<EquipmentPermissionSnapshot> discover() {
    return new ItemEquipmentDiscoveryService(workDir).discover().stream()
        .filter(
            item -> item.category() == ItemCategory.WEAPON || item.category() == ItemCategory.ARMOR)
        .map(EquipmentMatrixDiscoveryService::snapshot)
        .toList();
  }

  private static EquipmentPermissionSnapshot snapshot(ItemSnapshot item) {
    int maskOffset =
        item.category() == ItemCategory.WEAPON
            ? item.sourceOffset() + ItemEquipmentPatcher.WEAPON_MASK_OFFSET_IN_RECORD
            : item.sourceOffset() + ItemEquipmentPatcher.ARMOR_MASK_OFFSET_IN_RECORD;
    return EquipmentPermissionSnapshot.builder()
        .category(item.category())
        .itemId(item.id())
        .name(item.name())
        .subtype(item.armorSubtypeName())
        .permissionMask(item.equipMask() == null ? 0 : item.equipMask())
        .sourceEntry(item.sourceEntry())
        .sourceOffset(maskOffset)
        .build();
  }
}
