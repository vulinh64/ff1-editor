package com.ff1.editor.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public final class SpellTextService {

  private static final String SPELL_TEXT_ENTRY = "PACK0_4";

  private final Ff1TextService textService;

  public SpellTextService(Path workDir) {
    this.textService = new Ff1TextService(workDir);
  }

  public Map<Integer, String> spellNames(int spellCount) {
    return spellTexts(spellCount, 0, "names");
  }

  public Map<Integer, String> spellDescriptions(int spellCount) {
    return spellTexts(spellCount, 1, "descriptions");
  }

  private Map<Integer, String> spellTexts(int spellCount, int textOffset, String kind) {
    try {
      Map<Integer, String> text = textService.readLengthPrefixedTextTable(SPELL_TEXT_ENTRY);
      if (text.isEmpty()) {
        return Map.of();
      }
      int firstTextId = text.keySet().stream().mapToInt(Integer::intValue).min().orElseThrow();
      Map<Integer, String> spells = new HashMap<>();
      for (int spellId = 1; spellId < spellCount; spellId++) {
        spells.put(
            spellId, text.getOrDefault(firstTextId + spellId * 2 + textOffset, StringUtils.EMPTY));
      }
      return Map.copyOf(spells);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Unable to read spell " + kind + " from " + SPELL_TEXT_ENTRY, e);
    }
  }
}
