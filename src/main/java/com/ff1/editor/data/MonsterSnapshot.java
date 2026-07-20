package com.ff1.editor.data;

public record MonsterSnapshot(
    int id,
    String name,
    boolean bossOrFixed,
    String bossEncounterIds,
    int exp,
    int gil,
    int hp,
    int attack,
    int hitCount,
    int defense,
    int evasion,
    int magicDefense,
    int typeMask,
    int weaknessMask,
    int resistanceMask,
    int raw0,
    int raw1,
    int raw2,
    int raw3,
    int sourceOffset) {}
