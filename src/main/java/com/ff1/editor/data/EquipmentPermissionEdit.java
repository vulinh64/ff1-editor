package com.ff1.editor.data;

import lombok.Builder;

@Builder
public record EquipmentPermissionEdit(ItemCategory category, int itemId, int permissionMask) {}
