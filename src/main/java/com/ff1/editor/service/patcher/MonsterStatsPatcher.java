package com.ff1.editor.service.patcher;

import com.ff1.editor.data.MonsterStatsEdit;
import com.ff1.editor.service.Cp0ChunkTable;
import com.ff1.editor.service.MonsterDiscoveryService;

/** Patches confirmed monster reward and combat stat fields from the Monsters editor tab. */
public final class MonsterStatsPatcher {

  public static final int EXP_OFFSET_IN_RECORD = 4;
  public static final int GIL_OFFSET_IN_RECORD = 6;
  public static final int HP_OFFSET_IN_RECORD = 8;
  public static final int DEFENSE_OFFSET_IN_RECORD = 12;
  public static final int EVASION_OFFSET_IN_RECORD = 13;
  public static final int HIT_COUNT_OFFSET_IN_RECORD = 14;
  public static final int ATTACK_OFFSET_IN_RECORD = 16;
  public static final int ARCHETYPE_MASK_OFFSET_IN_RECORD = 20;
  public static final int MAGIC_DEFENSE_OFFSET_IN_RECORD = 21;
  public static final int WEAKNESS_MASK_OFFSET_IN_RECORD = 22;
  public static final int RESISTANCE_MASK_OFFSET_IN_RECORD = 23;

  private MonsterStatsPatcher() {}

  public static void apply(byte[] cp0, MonsterStatsEdit edit) {
    validateUnsignedShort(edit.exp(), "Monster EXP");
    validateUnsignedShort(edit.gil(), "Monster Gil");
    if (edit.hp() < 0 || edit.hp() > 999) {
      throw new IllegalArgumentException("Monster HP must be 0..999.");
    }
    validateByte(edit.attack(), "Monster attack");
    validateByte(edit.hitCount(), "Monster hit count");
    validateByte(edit.defense(), "Monster defense");
    validateByte(edit.evasion(), "Monster evasion");
    validateByte(edit.magicDefense(), "Monster magic defense");
    validateByte(edit.archetypeMask(), "Monster archetype mask");
    validateByte(edit.weaknessMask(), "Monster weakness mask");
    validateByte(edit.resistanceMask(), "Monster resistance mask");
    if (Integer.bitCount(edit.archetypeMask()) > 3) {
      throw new IllegalArgumentException("Monster archetypes are limited to 3 selections.");
    }
    if ((edit.weaknessMask() & edit.resistanceMask()) != 0) {
      throw new IllegalArgumentException("Monster weakness and resistance masks cannot overlap.");
    }

    int offset = monsterRecordOffset(cp0, edit.monsterId());
    writeUnsignedShort(cp0, offset + EXP_OFFSET_IN_RECORD, edit.exp());
    writeUnsignedShort(cp0, offset + GIL_OFFSET_IN_RECORD, edit.gil());
    writeUnsignedShort(cp0, offset + HP_OFFSET_IN_RECORD, edit.hp());
    cp0[offset + DEFENSE_OFFSET_IN_RECORD] = (byte) edit.defense();
    cp0[offset + EVASION_OFFSET_IN_RECORD] = (byte) edit.evasion();
    cp0[offset + HIT_COUNT_OFFSET_IN_RECORD] = (byte) edit.hitCount();
    cp0[offset + ATTACK_OFFSET_IN_RECORD] = (byte) edit.attack();
    cp0[offset + ARCHETYPE_MASK_OFFSET_IN_RECORD] = (byte) edit.archetypeMask();
    cp0[offset + MAGIC_DEFENSE_OFFSET_IN_RECORD] = (byte) edit.magicDefense();
    cp0[offset + WEAKNESS_MASK_OFFSET_IN_RECORD] = (byte) edit.weaknessMask();
    cp0[offset + RESISTANCE_MASK_OFFSET_IN_RECORD] = (byte) edit.resistanceMask();
  }

  private static int monsterRecordOffset(byte[] cp0, int monsterId) {
    if (monsterId < 0 || monsterId >= MonsterDiscoveryService.MONSTER_COUNT) {
      throw new IllegalArgumentException("Monster id must be 0..127.");
    }
    return new Cp0ChunkTable(cp0).chunkOffset(MonsterDiscoveryService.MONSTER_CHUNK_INDEX)
        + Short.BYTES
        + monsterId * MonsterDiscoveryService.MONSTER_RECORD_SIZE;
  }

  private static void writeUnsignedShort(byte[] bytes, int offset, int value) {
    bytes[offset] = (byte) ((value >>> 8) & 0xff);
    bytes[offset + 1] = (byte) (value & 0xff);
  }

  private static void validateUnsignedShort(int value, String label) {
    if (value < 0 || value > 0xffff) {
      throw new IllegalArgumentException(label + " must be 0..65535.");
    }
  }

  private static void validateByte(int value, String label) {
    if (value < 0 || value > 255) {
      throw new IllegalArgumentException(label + " must be 0..255.");
    }
  }
}
