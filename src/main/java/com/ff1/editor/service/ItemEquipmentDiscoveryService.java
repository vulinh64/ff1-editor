package com.ff1.editor.service;

import static com.ff1.editor.utils.EditorSupport.readBigEndianUnsignedShort;

import com.ff1.editor.data.ArmorSubtype;
import com.ff1.editor.data.ItemCategory;
import com.ff1.editor.data.ItemSnapshot;
import com.ff1.editor.data.MagicClassBit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ItemEquipmentDiscoveryService {

  public static final String ITEM_METADATA_ENTRY = "cp0";
  public static final int ITEM_METADATA_CHUNK_INDEX = 0;
  public static final int ARMOR_CHUNK_INDEX = 2;
  public static final int WEAPON_CHUNK_INDEX = 3;
  public static final int ITEM_METADATA_RECORD_SIZE = 4;
  public static final int ARMOR_RECORD_SIZE = 6;
  public static final int WEAPON_RECORD_SIZE = 9;
  public static final int ITEM_COUNT = 106;
  public static final int WEAPON_COUNT = 41;
  public static final int ARMOR_COUNT = 41;
  public static final int WEAPON_ITEM_ID_OFFSET = 7;
  public static final int ARMOR_ITEM_ID_OFFSET = 48;
  public static final int WEAPON_CAST_SPELL_OFFSET_IN_RECORD = 6;

  private static final String ITEM_TEXT_ENTRY = "PACK0_3";

  private final Path workDir;
  private final Ff1TextService textService;

  public ItemEquipmentDiscoveryService(Path workDir) {
    this.workDir = workDir;
    this.textService = new Ff1TextService(workDir);
  }

  public List<ItemSnapshot> discover() {
    try {
      byte[] cp0 = Files.readAllBytes(workDir.resolve(ITEM_METADATA_ENTRY));
      Cp0ChunkTable table = new Cp0ChunkTable(cp0);
      Map<Integer, TextPair> text = itemText();
      Map<Integer, String> spellNames =
          new SpellTextService(workDir).spellNames(SkillDiscoveryService.SKILL_COUNT);
      List<ItemSnapshot> items = metadataSnapshots(table, text);
      applyWeaponRecords(table, items, spellNames);
      applyArmorRecords(table, items, spellNames);
      return List.copyOf(items);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to discover items from " + workDir, e);
    }
  }

  private List<ItemSnapshot> metadataSnapshots(Cp0ChunkTable table, Map<Integer, TextPair> text) {
    byte[] chunk = table.chunk(ITEM_METADATA_CHUNK_INDEX);
    int count = readBigEndianUnsignedShort(chunk, 0);
    if (count != ITEM_COUNT || chunk.length != Short.BYTES + count * ITEM_METADATA_RECORD_SIZE) {
      throw new IllegalStateException("cp0 item metadata chunk does not match known layout.");
    }
    int chunkOffset = table.chunkOffset(ITEM_METADATA_CHUNK_INDEX);
    List<ItemSnapshot> items = new ArrayList<>(count);
    for (int id = 0; id < count; id++) {
      int recordOffset = Short.BYTES + id * ITEM_METADATA_RECORD_SIZE;
      TextPair textPair = text.getOrDefault(id, TextPair.EMPTY);
      items.add(
          ItemSnapshot.builder()
              .id(id)
              .name(textPair.name())
              .description(textPair.description())
              .category(category(id, textPair.name()))
              .price(readBigEndianUnsignedShort(chunk, recordOffset))
              .metadataByte1(chunk[recordOffset + 2] & 0xff)
              .metadataByte2(chunk[recordOffset + 3] & 0xff)
              .allowedClasses("")
              .sourceEntry(ITEM_METADATA_ENTRY)
              .sourceOffset(chunkOffset + recordOffset)
              .notes(notes(id))
              .build());
    }
    return items;
  }

  private static ItemCategory category(int id, String name) {
    if (id == 7 || id == 48 || name == null || name.isBlank()) {
      return ItemCategory.BLANK;
    }
    if (id >= 1 && id <= 6) {
      return ItemCategory.CONSUMABLE;
    }
    if (id >= 8 && id <= 47) {
      return ItemCategory.WEAPON;
    }
    if (id >= 49 && id <= 88) {
      return ItemCategory.ARMOR;
    }
    if (id >= 90 && id <= 105) {
      return ItemCategory.KEY_ITEM;
    }
    return ItemCategory.UNKNOWN;
  }

  private static String notes(int id) {
    return switch (id) {
      case 7, 48 -> "Blank item slot; likely unused or sentinel.";
      case 89 -> "Blank slot before key-item range.";
      default -> "";
    };
  }

  private void applyWeaponRecords(
      Cp0ChunkTable table, List<ItemSnapshot> items, Map<Integer, String> spellNames) {
    byte[] chunk = table.chunk(WEAPON_CHUNK_INDEX);
    int count = readBigEndianUnsignedShort(chunk, 0);
    if (chunk.length != Short.BYTES + count * WEAPON_RECORD_SIZE) {
      throw new IllegalStateException("cp0 weapon chunk does not match known layout.");
    }
    int chunkOffset = table.chunkOffset(WEAPON_CHUNK_INDEX);
    for (int weaponIndex = 0; weaponIndex < count; weaponIndex++) {
      int itemId = weaponIndex + WEAPON_ITEM_ID_OFFSET;
      int recordOffset = Short.BYTES + weaponIndex * WEAPON_RECORD_SIZE;
      int equipMask = readBigEndianUnsignedShort(chunk, recordOffset + 2);
      int castSpellId = chunk[recordOffset + WEAPON_CAST_SPELL_OFFSET_IN_RECORD] & 0xff;
      items.set(
          itemId,
          items
              .get(itemId)
              .withEquipMask(equipMask)
              .withAllowedClasses(MagicClassBit.namesForMask(equipMask))
              .withDamage(chunk[recordOffset + 4] & 0xff)
              .withAccuracy(chunk[recordOffset + 5] & 0xff)
              .withCastSpellId(castSpellId == 0 ? null : castSpellId)
              .withCastSpellName(spellNames.getOrDefault(castSpellId, ""))
              .withWeaponSpecialByte1(chunk[recordOffset + 7] & 0xff)
              .withWeaponSpecialByte2(chunk[recordOffset + 8] & 0xff)
              .withSourceOffset(chunkOffset + recordOffset));
    }
  }

  private void applyArmorRecords(
      Cp0ChunkTable table, List<ItemSnapshot> items, Map<Integer, String> spellNames) {
    byte[] chunk = table.chunk(ARMOR_CHUNK_INDEX);
    int count = readBigEndianUnsignedShort(chunk, 0);
    if (chunk.length != Short.BYTES + count * ARMOR_RECORD_SIZE) {
      throw new IllegalStateException("cp0 armor chunk does not match known layout.");
    }
    int chunkOffset = table.chunkOffset(ARMOR_CHUNK_INDEX);
    for (int armorIndex = 1; armorIndex < count; armorIndex++) {
      int itemId = armorIndex + ARMOR_ITEM_ID_OFFSET;
      int recordOffset = Short.BYTES + armorIndex * ARMOR_RECORD_SIZE;
      int equipMask = readBigEndianUnsignedShort(chunk, recordOffset);
      int castSpellId = chunk[recordOffset + 4] & 0xff;
      items.set(
          itemId,
          items
              .get(itemId)
              .withArmorSubtype(armorSubtype(itemId))
              .withEquipMask(equipMask)
              .withAllowedClasses(MagicClassBit.namesForMask(equipMask))
              .withAbsorb(chunk[recordOffset + 2] & 0xff)
              .withEvasionPenalty(chunk[recordOffset + 3] & 0xff)
              .withCastSpellId(castSpellId == 0 ? null : castSpellId)
              .withCastSpellName(spellNames.getOrDefault(castSpellId, ""))
              .withResistanceMask(chunk[recordOffset + 5] & 0xff)
              .withSourceOffset(chunkOffset + recordOffset));
    }
  }

  private static ArmorSubtype armorSubtype(int itemId) {
    if (itemId >= 49 && itemId <= 64) {
      return ArmorSubtype.BODY;
    }
    if (itemId >= 65 && itemId <= 73) {
      return ArmorSubtype.SHIELD;
    }
    if (itemId >= 74 && itemId <= 80) {
      return ArmorSubtype.HELM;
    }
    if (itemId >= 81 && itemId <= 88) {
      return ArmorSubtype.GLOVES;
    }
    throw new IllegalArgumentException("Item id is not armor: " + itemId);
  }

  private Map<Integer, TextPair> itemText() throws IOException {
    Map<Integer, String> decoded = textService.readLengthPrefixedTextTable(ITEM_TEXT_ENTRY);
    int firstId = decoded.keySet().stream().mapToInt(Integer::intValue).min().orElseThrow();

    Map<Integer, TextPair> items = new HashMap<>();
    for (int id = 0; id < ITEM_COUNT; id++) {
      int nameId = firstId + 2 * id;
      items.put(
          id, new TextPair(decoded.getOrDefault(nameId, ""), decoded.getOrDefault(nameId + 1, "")));
    }
    return items;
  }

  private record TextPair(String name, String description) {
    private static final TextPair EMPTY = new TextPair("", "");
  }
}
