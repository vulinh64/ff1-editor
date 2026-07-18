package com.ff1.editor.service;

import static com.ff1.editor.utils.EditorSupport.readBigEndianUnsignedShort;

import com.ff1.editor.data.MagicSpellSnapshot;
import com.ff1.editor.data.SpellSchool;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MagicMatrixDiscoveryService {

  public static final String ENTRY_NAME = "cp0";
  public static final int SPELL_CHUNK_INDEX = 1;
  public static final int SPELL_RECORD_SIZE = 13;
  public static final int LEARNABLE_SPELL_COUNT = 64;
  public static final int MASK_OFFSET_IN_RECORD = 11;

  private static final String[] WHITE_SPELLS = {
    "Cure", "Dia", "Protect", "Blink",
    "Blindna", "NulShock", "Invis", "Silence",
    "Cura", "Diara", "NulBlaze", "Heal",
    "Poisona", "Fear", "NulFrost", "Vox",
    "Curaga", "Life", "Diaga", "Healara",
    "Stona", "Exit", "Protera", "Invisira",
    "Curaja", "Diaja", "NulDeath", "Healaga",
    "Holy", "NulAll", "Dispel", "Full-Life"
  };
  private static final String[] BLACK_SPELLS = {
    "Fire", "Sleep", "Focus", "Thunder",
    "Blizzard", "Dark", "Temper", "Slow",
    "Fira", "Hold", "Thundara", "Focara",
    "Sleepra", "Haste", "Confuse", "Blizzara",
    "Firaga", "Scourge", "Teleport", "Slowra",
    "Thundaga", "Death", "Quake", "Stun",
    "Blizzaga", "Break", "Saber", "Blind",
    "Flare", "Stop", "Warp", "Kill"
  };

  private final Path workDir;

  public MagicMatrixDiscoveryService(Path workDir) {
    this.workDir = workDir;
  }

  public List<MagicSpellSnapshot> discover() {
    try {
      byte[] cp0 = Files.readAllBytes(workDir.resolve(ENTRY_NAME));
      Cp0ChunkTable table = new Cp0ChunkTable(cp0);
      byte[] spellChunk = table.chunk(SPELL_CHUNK_INDEX);
      int count = readBigEndianUnsignedShort(spellChunk, 0);
      int expectedLength = 2 + count * SPELL_RECORD_SIZE;
      if (spellChunk.length != expectedLength || count < LEARNABLE_SPELL_COUNT + 1) {
        throw new IllegalStateException(
            "cp0 spell chunk does not match the confirmed 13-byte spell metadata table.");
      }

      int chunkOffset = table.chunkOffset(SPELL_CHUNK_INDEX);
      List<MagicSpellSnapshot> spells = new ArrayList<>(LEARNABLE_SPELL_COUNT);
      for (int spellId = 1; spellId <= LEARNABLE_SPELL_COUNT; spellId++) {
        int recordOffset = 2 + spellId * SPELL_RECORD_SIZE;
        int mask = readBigEndianUnsignedShort(spellChunk, recordOffset + MASK_OFFSET_IN_RECORD);
        spells.add(snapshot(spellId, mask, chunkOffset + recordOffset + MASK_OFFSET_IN_RECORD));
      }
      return spells;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read cp0 spell metadata.", e);
    }
  }

  private static MagicSpellSnapshot snapshot(int spellId, int mask, int maskOffset) {
    boolean black = spellId >= 33;
    int index = black ? spellId - 33 : spellId - 1;
    SpellSchool school = black ? SpellSchool.BLACK : SpellSchool.WHITE;
    return MagicSpellSnapshot.builder()
        .spellId(spellId)
        .name(spellName(spellId))
        .school(school)
        .level(index / 4 + 1)
        .slot(index % 4 + 1)
        .permissionMask(mask)
        .sourceEntry(ENTRY_NAME)
        .sourceOffset(maskOffset)
        .build();
  }

  public static String spellName(int spellId) {
    boolean black = spellId >= 33;
    int index = black ? spellId - 33 : spellId - 1;
    String[] names = black ? BLACK_SPELLS : WHITE_SPELLS;
    if (index < 0 || index >= names.length) {
      return "";
    }
    return names[index];
  }
}
