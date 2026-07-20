package com.ff1.editor.service;

import static com.ff1.editor.utils.EditorSupport.readBigEndianUnsignedShort;
import static com.ff1.editor.utils.EditorSupport.u8;

import com.ff1.editor.data.MonsterSnapshot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;

public final class MonsterDiscoveryService {

  public static final String ENTRY_NAME = "cp0";
  public static final String MONSTER_TEXT_ENTRY = "PACK0_14";
  public static final int ENCOUNTER_CHUNK_INDEX = 12;
  public static final int MONSTER_CHUNK_INDEX = 15;
  public static final int ENCOUNTER_RECORD_SIZE = 15;
  public static final int MONSTER_RECORD_SIZE = 25;
  public static final int MONSTER_COUNT = 128;

  private final Path workDir;

  public MonsterDiscoveryService(Path workDir) {
    this.workDir = workDir;
  }

  public List<MonsterSnapshot> discover() {
    try {
      byte[] cp0 = Files.readAllBytes(workDir.resolve(ENTRY_NAME));
      Cp0ChunkTable table = new Cp0ChunkTable(cp0);
      byte[] monsterChunk = table.chunk(MONSTER_CHUNK_INDEX);
      int count = readBigEndianUnsignedShort(monsterChunk, 0);
      if (count != MONSTER_COUNT
          || monsterChunk.length != Short.BYTES + count * MONSTER_RECORD_SIZE) {
        throw new IllegalStateException("cp0 monster chunk does not match known layout.");
      }

      Map<Integer, String> names = monsterNames();
      Map<Integer, String> bossEncounterIds = bossEncounterIds(table.chunk(ENCOUNTER_CHUNK_INDEX));
      int chunkOffset = table.chunkOffset(MONSTER_CHUNK_INDEX);
      List<MonsterSnapshot> monsters = new ArrayList<>(count);
      for (int id = 0; id < count; id++) {
        int recordOffset = Short.BYTES + id * MONSTER_RECORD_SIZE;
        String encounters = bossEncounterIds.getOrDefault(id, StringUtils.EMPTY);
        monsters.add(
            snapshot(
                monsterChunk,
                id,
                names.getOrDefault(id, StringUtils.EMPTY),
                !encounters.isBlank(),
                encounters,
                chunkOffset + recordOffset));
      }
      return List.copyOf(monsters);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to discover monsters from " + workDir, e);
    }
  }

  private MonsterSnapshot snapshot(
      byte[] chunk, int id, String name, boolean bossOrFixed, String encounters, int sourceOffset) {
    int recordOffset = Short.BYTES + id * MONSTER_RECORD_SIZE;
    return new MonsterSnapshot(
        id,
        name,
        bossOrFixed,
        encounters,
        readBigEndianUnsignedShort(chunk, recordOffset + 4),
        readBigEndianUnsignedShort(chunk, recordOffset + 6),
        readBigEndianUnsignedShort(chunk, recordOffset + 8),
        u8(chunk[recordOffset + 16]),
        u8(chunk[recordOffset + 14]),
        u8(chunk[recordOffset + 12]),
        u8(chunk[recordOffset + 13]),
        u8(chunk[recordOffset + 21]),
        u8(chunk[recordOffset + 20]),
        u8(chunk[recordOffset + 22]),
        u8(chunk[recordOffset + 23]),
        u8(chunk[recordOffset]),
        u8(chunk[recordOffset + 1]),
        u8(chunk[recordOffset + 2]),
        u8(chunk[recordOffset + 3]),
        sourceOffset);
  }

  private Map<Integer, String> monsterNames() throws IOException {
    Map<Integer, String> text =
        new Ff1TextService(workDir).readLengthPrefixedTextTable(MONSTER_TEXT_ENTRY);
    int firstId = text.keySet().stream().mapToInt(Integer::intValue).min().orElseThrow();
    Map<Integer, String> names = new LinkedHashMap<>();
    for (int id = 0; id < MONSTER_COUNT; id++) {
      names.put(id, text.getOrDefault(firstId + id, StringUtils.EMPTY));
    }
    return Map.copyOf(names);
  }

  private static Map<Integer, String> bossEncounterIds(byte[] encounterChunk) {
    int count = readBigEndianUnsignedShort(encounterChunk, 0);
    if (encounterChunk.length != Short.BYTES + count * ENCOUNTER_RECORD_SIZE) {
      throw new IllegalStateException("cp0 encounter chunk does not match known layout.");
    }
    Map<Integer, TreeSet<Integer>> byMonsterId = new LinkedHashMap<>();
    for (int encounterId = 0; encounterId < count; encounterId++) {
      int recordOffset = Short.BYTES + encounterId * ENCOUNTER_RECORD_SIZE;
      if (u8(encounterChunk[recordOffset + 1]) != 1) {
        continue;
      }
      for (int groupOffset = 3; groupOffset <= 12; groupOffset += 3) {
        int monsterId = u8(encounterChunk[recordOffset + groupOffset]);
        int maxCount = u8(encounterChunk[recordOffset + groupOffset + 2]);
        if (maxCount == 0) {
          continue;
        }
        byMonsterId.computeIfAbsent(monsterId, _ -> new TreeSet<>()).add(encounterId);
      }
    }
    Map<Integer, String> labels = new LinkedHashMap<>();
    byMonsterId.forEach(
        (monsterId, encounterIds) ->
            labels.put(
                monsterId,
                encounterIds.stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(", "))));
    return Map.copyOf(labels);
  }
}
