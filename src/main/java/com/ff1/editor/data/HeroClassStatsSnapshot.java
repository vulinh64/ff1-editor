package com.ff1.editor.data;

import lombok.Builder;
import lombok.With;

@Builder
@With
public record HeroClassStatsSnapshot(
    int hp, int strength, int agility, int intelligence, int stamina, int luck, String sourceNote) {

  public String compact() {
    return "HP=%d STR=%d AGL=%d INT=%d STA=%d LCK=%d"
        .formatted(hp, strength, agility, intelligence, stamina, luck);
  }
}
