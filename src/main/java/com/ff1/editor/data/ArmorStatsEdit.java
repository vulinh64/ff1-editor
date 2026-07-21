package com.ff1.editor.data;

import lombok.Builder;

@Builder
public record ArmorStatsEdit(int armorItemId, int absorb, int evasionPenalty, int resistanceMask) {}
