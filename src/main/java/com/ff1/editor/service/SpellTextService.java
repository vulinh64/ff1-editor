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
    try {
      Map<Integer, String> text = textService.readLengthPrefixedTextTable(SPELL_TEXT_ENTRY);
      if (text.isEmpty()) {
        return Map.of();
      }
      int firstTextId = text.keySet().stream().mapToInt(Integer::intValue).min().orElseThrow();
      Map<Integer, String> names = new HashMap<>();
      for (int spellId = 1; spellId < spellCount; spellId++) {
        names.put(spellId, text.getOrDefault(firstTextId + spellId * 2, StringUtils.EMPTY));
      }
      return Map.copyOf(names);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read spell names from " + SPELL_TEXT_ENTRY, e);
    }
  }
}
