package com.ff1.editor.data;

import lombok.Builder;
import lombok.With;

@Builder
@With
public record HeroClassSnapshot(
    HeroClass heroClass, HeroClassStatsSnapshot stats, String sourceEntry, int sourceOffset) {

  public int id() {
    return heroClass.id();
  }

  public String name() {
    return heroClass.label();
  }

  public boolean upgraded() {
    return heroClass.upgraded();
  }

  public int upgradeFromId() {
    return heroClass.upgradeFromId();
  }

  @Override
  public String toString() {
    String upgrade = upgraded() ? "upgrade of " + upgradeFromId() : "base";
    return "%02d %-12s %-12s %-48s %s @ 0x%08x"
        .formatted(id(), name(), upgrade, stats.compact(), sourceEntry, sourceOffset);
  }
}
