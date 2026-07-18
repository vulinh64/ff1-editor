package com.ff1.editor.service;

import static com.ff1.editor.utils.EditorSupport.readBigEndianUnsignedShort;

import com.ff1.editor.data.ItemCategory;
import com.ff1.editor.data.ItemSnapshot;
import com.ff1.editor.data.SkillEffectKind;
import com.ff1.editor.data.SkillSnapshot;
import com.ff1.editor.data.SpellSchool;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SkillDiscoveryService {

  public static final String ENTRY_NAME = MagicMatrixDiscoveryService.ENTRY_NAME;
  public static final int SPELL_CHUNK_INDEX = MagicMatrixDiscoveryService.SPELL_CHUNK_INDEX;
  public static final int SPELL_RECORD_SIZE = MagicMatrixDiscoveryService.SPELL_RECORD_SIZE;
  public static final int SKILL_COUNT = 94;
  public static final int POWER_OR_STATUS_OFFSET_IN_RECORD = 5;
  public static final int ACCURACY_OFFSET_IN_RECORD = 6;

  private static final Map<Integer, Integer> CONSUMABLE_EFFECT_SPELL_IDS =
      Map.of(1, 91, 2, 92, 3, 93);

  private final Path workDir;

  public SkillDiscoveryService(Path workDir) {
    this.workDir = workDir;
  }

  public List<SkillSnapshot> discover() {
    try {
      byte[] cp0 = Files.readAllBytes(workDir.resolve(ENTRY_NAME));
      Cp0ChunkTable table = new Cp0ChunkTable(cp0);
      byte[] spellChunk = table.chunk(SPELL_CHUNK_INDEX);
      int count = readBigEndianUnsignedShort(spellChunk, 0);
      int expectedLength = Short.BYTES + count * SPELL_RECORD_SIZE;
      if (count != SKILL_COUNT || spellChunk.length != expectedLength) {
        throw new IllegalStateException("cp0 spell/effect chunk does not match known layout.");
      }

      Map<Integer, List<String>> invokers = invokers();
      int chunkOffset = table.chunkOffset(SPELL_CHUNK_INDEX);
      List<SkillSnapshot> skills = new ArrayList<>(count);
      for (int id = 0; id < count; id++) {
        int recordOffset = Short.BYTES + id * SPELL_RECORD_SIZE;
        skills.add(snapshot(spellChunk, id, chunkOffset + recordOffset, invokers.get(id)));
      }
      return List.copyOf(skills);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to discover spell/effect records from " + workDir, e);
    }
  }

  private SkillSnapshot snapshot(
      byte[] spellChunk, int id, int sourceOffset, List<String> invokers) {
    int recordOffset = Short.BYTES + id * SPELL_RECORD_SIZE;
    return SkillSnapshot.builder()
        .id(id)
        .name(skillName(id))
        .learnableLabel(learnableLabel(id))
        .price(readBigEndianUnsignedShort(spellChunk, recordOffset))
        .raw0(spellChunk[recordOffset + 2] & 0xff)
        .effectId(spellChunk[recordOffset + 3] & 0xff)
        .effectKind(spellChunk[recordOffset + 4] & 0xff)
        .effectKindName(SkillEffectKind.displayName(spellChunk[recordOffset + 4] & 0xff))
        .powerOrStatus(spellChunk[recordOffset + POWER_OR_STATUS_OFFSET_IN_RECORD] & 0xff)
        .accuracy(spellChunk[recordOffset + ACCURACY_OFFSET_IN_RECORD] & 0xff)
        .raw5(spellChunk[recordOffset + 7] & 0xff)
        .animationId(spellChunk[recordOffset + 8] & 0xff)
        .animationFlags(spellChunk[recordOffset + 9] & 0xff)
        .elementOrStatusMask(spellChunk[recordOffset + 10] & 0xff)
        .permissionMask(readBigEndianUnsignedShort(spellChunk, recordOffset + 11))
        .invokers(invokers == null ? "" : String.join("; ", invokers))
        .sourceEntry(ENTRY_NAME)
        .sourceOffset(sourceOffset)
        .build();
  }

  private Map<Integer, List<String>> invokers() {
    Map<Integer, List<String>> invokers = new LinkedHashMap<>();
    for (int id = 1; id <= MagicMatrixDiscoveryService.LEARNABLE_SPELL_COUNT; id++) {
      addInvoker(invokers, id, "Learnable spell");
    }

    for (ItemSnapshot item : new ItemEquipmentDiscoveryService(workDir).discover()) {
      Integer castSpellId = item.castSpellId();
      if (castSpellId != null && castSpellId != 0) {
        addInvoker(invokers, castSpellId, "%s: %s".formatted(item.categoryName(), item.name()));
      }
      Integer consumableEffectSpellId = CONSUMABLE_EFFECT_SPELL_IDS.get(item.id());
      if (consumableEffectSpellId != null && item.category() == ItemCategory.CONSUMABLE) {
        addInvoker(invokers, consumableEffectSpellId, "Consumable: " + item.name());
      }
    }
    return invokers;
  }

  private static void addInvoker(Map<Integer, List<String>> invokers, int spellId, String invoker) {
    invokers.computeIfAbsent(spellId, _ -> new ArrayList<>()).add(invoker);
  }

  public static String skillName(int id) {
    String spellName = MagicMatrixDiscoveryService.spellName(id);
    if (!spellName.isBlank()) {
      return spellName;
    }
    return switch (id) {
      case 91 -> "Potion effect";
      case 92 -> "Antidote effect";
      case 93 -> "Gold Needle effect";
      default -> "";
    };
  }

  private static String learnableLabel(int id) {
    if (id < 1 || id > MagicMatrixDiscoveryService.LEARNABLE_SPELL_COUNT) {
      return "";
    }
    SpellSchool school = id >= 33 ? SpellSchool.BLACK : SpellSchool.WHITE;
    int index = id >= 33 ? id - 33 : id - 1;
    return "%s LV%d.%d".formatted(school.displayName(), index / 4 + 1, index % 4 + 1);
  }
}
