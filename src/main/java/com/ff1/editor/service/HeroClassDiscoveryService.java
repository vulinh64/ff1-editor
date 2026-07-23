package com.ff1.editor.service;

import com.ff1.editor.data.HeroClass;
import com.ff1.editor.data.HeroClassSnapshot;
import com.ff1.editor.data.HeroClassStatsSnapshot;
import com.ff1.editor.service.patcher.bytecode.*;
import com.ff1.editor.service.patcher.data.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class HeroClassDiscoveryService {

  private static final String SOURCE_ENTRY = "PACK0_1";

  private final Path extractedDir;
  private final Ff1TextService textService;

  public HeroClassDiscoveryService(Path extractedDir) {
    this.extractedDir = extractedDir;
    this.textService = new Ff1TextService(extractedDir);
  }

  public List<HeroClassSnapshot> discover() {
    try {
      byte[] source = Files.readAllBytes(extractedDir.resolve(SOURCE_ENTRY));
      byte[] cp0 = Files.readAllBytes(extractedDir.resolve(HeroClassStatsPatcher.ENTRY_NAME));
      List<HeroClassSnapshot> snapshots = new ArrayList<>();
      for (HeroClass heroClass : HeroClass.values()) {
        byte[] encoded = textService.encodeText(heroClass.label());
        int offset = indexOf(source, encoded);
        if (offset < 0) {
          throw new IllegalStateException(
              "Could not find hero class name in " + SOURCE_ENTRY + ": " + heroClass.label());
        }
        snapshots.add(
            HeroClassSnapshot.builder()
                .heroClass(heroClass)
                .stats(stats(cp0, heroClass))
                .sourceEntry(SOURCE_ENTRY)
                .sourceOffset(offset)
                .build());
      }
      return List.copyOf(snapshots);
    } catch (IOException e) {
      throw new IllegalStateException("Could not discover hero classes from " + extractedDir, e);
    }
  }

  private static HeroClassStatsSnapshot stats(byte[] cp0, HeroClass heroClass) {
    int baseClassId = heroClass.upgraded() ? heroClass.upgradeFromId() : heroClass.id();
    int offset =
        HeroClassStatsPatcher.TABLE_OFFSET + baseClassId * HeroClassStatsPatcher.RECORD_SIZE;
    return HeroClassStatsSnapshot.builder()
        .hp(cp0[offset])
        .strength(cp0[offset + 1])
        .agility(cp0[offset + 2])
        .intelligence(cp0[offset + 3])
        .stamina(cp0[offset + 4])
        .luck(cp0[offset + 5])
        .sourceNote(sourceNote(heroClass))
        .build();
  }

  private static String sourceNote(HeroClass heroClass) {
    return heroClass.upgraded()
        ? "Class-change form of %s; live stats are inherited from the character."
            .formatted(HeroClass.values()[heroClass.upgradeFromId()].label())
        : "Read from confirmed cp0 starting-stat table.";
  }

  private static int indexOf(byte[] haystack, byte[] needle) {
    for (int i = 0; i <= haystack.length - needle.length; i++) {
      boolean match = true;
      for (int j = 0; j < needle.length; j++) {
        if (haystack[i + j] != needle[j]) {
          match = false;
          break;
        }
      }
      if (match) {
        return i;
      }
    }
    return -1;
  }
}
