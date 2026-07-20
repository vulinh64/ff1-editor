package com.ff1.editor.data;

import lombok.Builder;

@Builder
public record MonsterStatsEdit(
    int monsterId,
    int exp,
    int gil,
    int hp,
    int attack,
    int hitCount,
    int defense,
    int evasion,
    int magicDefense,
    int archetypeMask,
    int weaknessMask,
    int resistanceMask) {}
