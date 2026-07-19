package com.ff1.editor.data;

import lombok.Builder;

@Builder
public record EquipmentPermissionSnapshot(
    ItemCategory category,
    int itemId,
    String name,
    String subtype,
    int permissionMask,
    String sourceEntry,
    int sourceOffset) {}
