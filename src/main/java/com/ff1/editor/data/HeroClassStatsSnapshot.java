package com.ff1.editor.data;

import lombok.Builder;
import lombok.With;

@Builder
@With
public record HeroClassStatsSnapshot(
    int hp,
    int strength,
    int agility,
    int intelligence,
    int stamina,
    int luck,
    int accuracy,
    int evasion,
    String sourceNote) {

  public String compact() {
    return "HP=%d STR=%d AGL=%d INT=%d STA=%d LCK=%d ACC=%d EVA=%d"
        .formatted(hp, strength, agility, intelligence, stamina, luck, accuracy, evasion);
  }
}
