package com.ff1.editor.data;

import lombok.Builder;

@Builder
public record WeaponStatsEdit(
    int weaponItemId, int damage, int accuracy, int affinityMask, int familyMask) {}
