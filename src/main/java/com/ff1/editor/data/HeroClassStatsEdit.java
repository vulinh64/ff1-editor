package com.ff1.editor.data;

import lombok.Builder;
import lombok.With;

@Builder
@With
public record HeroClassStatsEdit(
    int classId, int hp, int strength, int agility, int intelligence, int stamina, int luck) {}
