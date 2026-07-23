package com.ff1.editor.service;

import static com.ff1.editor.utils.EditorSupport.readBigEndianUnsignedShort;

import com.ff1.editor.data.ItemSnapshot;
import com.ff1.editor.data.ShopGoodSnapshot;
import com.ff1.editor.data.ShopInventoryType;
import com.ff1.editor.data.ShopLocationSnapshot;
import com.ff1.editor.data.ShopMappingStatus;
import com.ff1.editor.data.ShopPriceSnapshot;
import com.ff1.editor.data.ShopServiceName;
import com.ff1.editor.data.ShopServiceSnapshot;
import com.ff1.editor.data.ShopSlotSnapshot;
import com.ff1.editor.data.SkillSnapshot;
import com.ff1.editor.data.SpellSchool;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public final class ShopDiscoveryService {

  public static final String ENTRY_NAME = "cp0";
  public static final int SHOP_SLOT_COUNT = 5;
  public static final int SERVICE_PRICE_CHUNK_INDEX = 14;
  public static final int SERVICE_PRICE_RECORD_SIZE = 4;

  private final Path workDir;

  public ShopDiscoveryService(Path workDir) {
    this.workDir = workDir;
  }

  public List<ShopLocationSnapshot> locations() {
    return List.of(
        new ShopLocationSnapshot(
            0,
            "Cornelia",
            List.of(
                ShopServiceSnapshot.inventory(
                    ShopServiceName.WEAPON_SHOP,
                    ShopInventoryType.WEAPON,
                    0,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.ARMOR_SHOP,
                    ShopInventoryType.ARMOR,
                    0,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.ITEM_SHOP,
                    ShopInventoryType.ITEM,
                    0,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_1_WHITE_MAGIC_SHOP,
                    ShopInventoryType.WHITE_MAGIC,
                    0,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_1_BLACK_MAGIC_SHOP,
                    ShopInventoryType.BLACK_MAGIC,
                    0,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.price(ShopServiceName.INN, 0, 0, ShopMappingStatus.CONFIRMED))),
        new ShopLocationSnapshot(
            1,
            "Pravoka",
            List.of(
                ShopServiceSnapshot.inventory(
                    ShopServiceName.WEAPON_SHOP,
                    ShopInventoryType.WEAPON,
                    1,
                    ShopMappingStatus.WIKI_BACKED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.ARMOR_SHOP,
                    ShopInventoryType.ARMOR,
                    1,
                    ShopMappingStatus.WIKI_BACKED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.ITEM_SHOP,
                    ShopInventoryType.ITEM,
                    1,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_2_WHITE_MAGIC_SHOP,
                    ShopInventoryType.WHITE_MAGIC,
                    1,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_2_BLACK_MAGIC_SHOP,
                    ShopInventoryType.BLACK_MAGIC,
                    1,
                    ShopMappingStatus.CONFIRMED))),
        new ShopLocationSnapshot(
            2,
            "Elfheim",
            List.of(
                ShopServiceSnapshot.inventory(
                    ShopServiceName.WEAPON_SHOP,
                    ShopInventoryType.WEAPON,
                    2,
                    ShopMappingStatus.WIKI_BACKED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.ARMOR_SHOP,
                    ShopInventoryType.ARMOR,
                    2,
                    ShopMappingStatus.WIKI_BACKED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_3_WHITE_MAGIC_SHOP,
                    ShopInventoryType.WHITE_MAGIC,
                    2,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_3_BLACK_MAGIC_SHOP,
                    ShopInventoryType.BLACK_MAGIC,
                    2,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_4_WHITE_MAGIC_SHOP,
                    ShopInventoryType.WHITE_MAGIC,
                    3,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_4_BLACK_MAGIC_SHOP,
                    ShopInventoryType.BLACK_MAGIC,
                    3,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.ITEM_SHOP,
                    ShopInventoryType.ITEM,
                    2,
                    ShopMappingStatus.CONFIRMED))),
        new ShopLocationSnapshot(
            3,
            "Melmond",
            List.of(
                ShopServiceSnapshot.inventory(
                    ShopServiceName.WEAPON_SHOP,
                    ShopInventoryType.WEAPON,
                    3,
                    ShopMappingStatus.WIKI_BACKED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.ARMOR_SHOP,
                    ShopInventoryType.ARMOR,
                    3,
                    ShopMappingStatus.WIKI_BACKED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_5_WHITE_MAGIC_SHOP,
                    ShopInventoryType.WHITE_MAGIC,
                    4,
                    ShopMappingStatus.WIKI_BACKED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_5_BLACK_MAGIC_SHOP,
                    ShopInventoryType.BLACK_MAGIC,
                    4,
                    ShopMappingStatus.WIKI_BACKED))),
        new ShopLocationSnapshot(
            4,
            "Crescent Lake",
            List.of(
                ShopServiceSnapshot.inventory(
                    ShopServiceName.WEAPON_SHOP,
                    ShopInventoryType.WEAPON,
                    5,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.ARMOR_SHOP,
                    ShopInventoryType.ARMOR,
                    5,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_6_WHITE_MAGIC_SHOP,
                    ShopInventoryType.WHITE_MAGIC,
                    5,
                    ShopMappingStatus.WIKI_BACKED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_6_BLACK_MAGIC_SHOP,
                    ShopInventoryType.BLACK_MAGIC,
                    5,
                    ShopMappingStatus.WIKI_BACKED))),
        new ShopLocationSnapshot(
            5,
            "Gaia",
            List.of(
                ShopServiceSnapshot.inventory(
                    ShopServiceName.WEAPON_SHOP,
                    ShopInventoryType.WEAPON,
                    4,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.ARMOR_SHOP,
                    ShopInventoryType.ARMOR,
                    4,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_7_WHITE_MAGIC_SHOP,
                    ShopInventoryType.WHITE_MAGIC,
                    6,
                    ShopMappingStatus.LIKELY),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_7_BLACK_MAGIC_SHOP,
                    ShopInventoryType.BLACK_MAGIC,
                    6,
                    ShopMappingStatus.LIKELY),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_8_WHITE_MAGIC_SHOP,
                    ShopInventoryType.WHITE_MAGIC,
                    7,
                    ShopMappingStatus.LIKELY),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_8_BLACK_MAGIC_SHOP,
                    ShopInventoryType.BLACK_MAGIC,
                    7,
                    ShopMappingStatus.LIKELY))),
        new ShopLocationSnapshot(
            6,
            "Onrac",
            List.of(
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_7_WHITE_MAGIC_SHOP,
                    ShopInventoryType.WHITE_MAGIC,
                    8,
                    ShopMappingStatus.LIKELY),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.LEVEL_7_BLACK_MAGIC_SHOP,
                    ShopInventoryType.BLACK_MAGIC,
                    8,
                    ShopMappingStatus.LIKELY))),
        new ShopLocationSnapshot(
            7,
            "Caravan",
            List.of(
                ShopServiceSnapshot.inventory(
                    ShopServiceName.SPECIAL_SHOP,
                    ShopInventoryType.SPECIAL,
                    0,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.EVOLVED_SHOP,
                    ShopInventoryType.SPECIAL,
                    1,
                    ShopMappingStatus.CONFIRMED))),
        new ShopLocationSnapshot(
            8,
            "Lufenia",
            List.of(
                ShopServiceSnapshot.inventory(
                    ShopServiceName.HIDDEN_WHITE_MAGIC_SHOP,
                    ShopInventoryType.WHITE_MAGIC,
                    9,
                    ShopMappingStatus.CONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.HIDDEN_BLACK_MAGIC_SHOP,
                    ShopInventoryType.BLACK_MAGIC,
                    9,
                    ShopMappingStatus.CONFIRMED))),
        new ShopLocationSnapshot(
            9,
            "<Unknown>",
            List.of(
                ShopServiceSnapshot.inventory(
                    ShopServiceName.ITEM_SHOP_ROW_3,
                    ShopInventoryType.ITEM,
                    3,
                    ShopMappingStatus.UNCONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.ITEM_SHOP_ROW_4,
                    ShopInventoryType.ITEM,
                    4,
                    ShopMappingStatus.UNCONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.ITEM_SHOP_ROW_5,
                    ShopInventoryType.ITEM,
                    5,
                    ShopMappingStatus.UNCONFIRMED),
                ShopServiceSnapshot.inventory(
                    ShopServiceName.ITEM_SHOP_ROW_6,
                    ShopInventoryType.ITEM,
                    6,
                    ShopMappingStatus.UNCONFIRMED),
                ShopServiceSnapshot.price(
                    ShopServiceName.INN_ROW_1, 1, 0, ShopMappingStatus.UNCONFIRMED),
                ShopServiceSnapshot.price(
                    ShopServiceName.INN_ROW_2, 2, 0, ShopMappingStatus.UNCONFIRMED),
                ShopServiceSnapshot.price(
                    ShopServiceName.INN_ROW_3, 3, 0, ShopMappingStatus.UNCONFIRMED),
                ShopServiceSnapshot.price(
                    ShopServiceName.INN_ROW_4, 4, 0, ShopMappingStatus.UNCONFIRMED),
                ShopServiceSnapshot.price(
                    ShopServiceName.INN_ROW_5, 5, 0, ShopMappingStatus.UNCONFIRMED),
                ShopServiceSnapshot.price(
                    ShopServiceName.INN_ROW_6, 6, 0, ShopMappingStatus.UNCONFIRMED))));
  }

  public List<ShopSlotSnapshot> slots(ShopServiceSnapshot service) {
    if (service == null || service.kind() != com.ff1.editor.data.ShopServiceKind.INVENTORY) {
      return List.of();
    }
    try {
      byte[] cp0 = Files.readAllBytes(workDir.resolve(ENTRY_NAME));
      Cp0ChunkTable table = new Cp0ChunkTable(cp0);
      ShopInventoryType shopType = service.shopType();
      int rowIndex = service.rowIndex();
      int chunkIndex = shopType.chunkIndex();
      byte[] chunk = table.chunk(chunkIndex);
      int rows = readBigEndianUnsignedShort(chunk, 0);
      if (rowIndex < 0 || rowIndex >= rows) {
        throw new IllegalArgumentException("Shop row index is outside chunk row count.");
      }
      int rowOffset = Short.BYTES + rowIndex * SHOP_SLOT_COUNT;
      Map<Integer, ItemSnapshot> items = itemLookup();
      Map<Integer, SkillSnapshot> skills = skillLookup();
      List<ShopSlotSnapshot> slots = new ArrayList<>(SHOP_SLOT_COUNT);
      for (int slot = 0; slot < SHOP_SLOT_COUNT; slot++) {
        int goodId = chunk[rowOffset + slot] & 0xff;
        slots.add(
            new ShopSlotSnapshot(
                shopType.id(),
                rowIndex,
                slot,
                goodId,
                goodName(shopType, goodId, items, skills),
                category(shopType, goodId, items, skills),
                price(shopType, goodId, items, skills),
                ENTRY_NAME,
                table.chunkOffset(chunkIndex) + rowOffset + slot));
      }
      return List.copyOf(slots);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read shop inventory rows from " + workDir, e);
    }
  }

  public List<ShopGoodSnapshot> goodOptions(ShopServiceSnapshot service) {
    if (service == null || service.kind() != com.ff1.editor.data.ShopServiceKind.INVENTORY) {
      return List.of();
    }
    Map<Integer, ItemSnapshot> items = itemLookup();
    Map<Integer, SkillSnapshot> skills = skillLookup();
    List<Integer> ids =
        switch (service.shopType()) {
          case WEAPON -> idRange(8, 47);
          case ARMOR -> idRange(49, 88);
          case ITEM, SPECIAL -> idRange(1, 105);
          case BLACK_MAGIC -> idRange(33, 64);
          case WHITE_MAGIC -> idRange(1, 32);
        };
    List<ShopGoodSnapshot> options = new ArrayList<>(ids.size() + 1);
    options.add(new ShopGoodSnapshot(0, StringUtils.EMPTY, "Empty", null));
    for (int id : ids) {
      ShopInventoryType shopType = service.shopType();
      String name = goodName(shopType, id, items, skills);
      if (name.isBlank()) {
        continue;
      }
      options.add(
          new ShopGoodSnapshot(
              id, name, category(shopType, id, items, skills), price(shopType, id, items, skills)));
    }
    return List.copyOf(options);
  }

  public ShopPriceSnapshot price(ShopServiceSnapshot service) {
    if (service == null || service.kind() != com.ff1.editor.data.ShopServiceKind.PRICE) {
      throw new IllegalArgumentException("Service is not a price-backed service.");
    }
    try {
      byte[] cp0 = Files.readAllBytes(workDir.resolve(ENTRY_NAME));
      Cp0ChunkTable table = new Cp0ChunkTable(cp0);
      byte[] chunk = table.chunk(SERVICE_PRICE_CHUNK_INDEX);
      int rows = readBigEndianUnsignedShort(chunk, 0);
      int rowIndex = service.rowIndex();
      int serviceColumn = service.serviceColumn();
      if (rowIndex < 0 || rowIndex >= rows || serviceColumn < 0 || serviceColumn > 1) {
        throw new IllegalArgumentException("Service price index is outside chunk row bounds.");
      }
      int offset = Short.BYTES + rowIndex * SERVICE_PRICE_RECORD_SIZE + serviceColumn * Short.BYTES;
      return new ShopPriceSnapshot(
          rowIndex,
          serviceColumn,
          readBigEndianUnsignedShort(chunk, offset),
          ENTRY_NAME,
          table.chunkOffset(SERVICE_PRICE_CHUNK_INDEX) + offset);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read service prices from " + workDir, e);
    }
  }

  public static int chunkIndex(int shopType) {
    return ShopInventoryType.fromId(shopType).chunkIndex();
  }

  private Map<Integer, ItemSnapshot> itemLookup() {
    return new ItemEquipmentDiscoveryService(workDir)
        .discover().stream()
            .collect(Collectors.toMap(ItemSnapshot::id, Function.identity(), (_, newer) -> newer));
  }

  private Map<Integer, SkillSnapshot> skillLookup() {
    return new SkillDiscoveryService(workDir)
        .discover().stream()
            .sorted(Comparator.comparingInt(SkillSnapshot::id))
            .collect(
                Collectors.toMap(
                    SkillSnapshot::id,
                    Function.identity(),
                    (_, newer) -> newer,
                    LinkedHashMap::new));
  }

  private static String goodName(
      ShopInventoryType shopType,
      int goodId,
      Map<Integer, ItemSnapshot> items,
      Map<Integer, SkillSnapshot> skills) {
    if (goodId == 0) {
      return StringUtils.EMPTY;
    }
    if (shopType == ShopInventoryType.BLACK_MAGIC || shopType == ShopInventoryType.WHITE_MAGIC) {
      SkillSnapshot skill = skills.get(goodId);
      return skill == null || skill.name().isBlank() ? "Spell " + goodId : skill.name();
    }
    ItemSnapshot item = items.get(goodId);
    return item == null || item.name().isBlank() ? "Item " + goodId : item.name();
  }

  private static String category(
      ShopInventoryType shopType,
      int goodId,
      Map<Integer, ItemSnapshot> items,
      Map<Integer, SkillSnapshot> skills) {
    if (goodId == 0) {
      return "Empty";
    }
    if (shopType == ShopInventoryType.BLACK_MAGIC || shopType == ShopInventoryType.WHITE_MAGIC) {
      SkillSnapshot skill = skills.get(goodId);
      if (skill != null
          && goodId >= 1
          && goodId <= MagicMatrixDiscoveryService.LEARNABLE_SPELL_COUNT) {
        SpellSchool school = goodId >= 33 ? SpellSchool.BLACK : SpellSchool.WHITE;
        int index = goodId >= 33 ? goodId - 33 : goodId - 1;
        return "%s LV%d.%d".formatted(school.displayName(), index / 4 + 1, index % 4 + 1);
      }
      return "Magic";
    }
    ItemSnapshot item = items.get(goodId);
    return item == null ? "Item" : item.categoryName();
  }

  private static Integer price(
      ShopInventoryType shopType,
      int goodId,
      Map<Integer, ItemSnapshot> items,
      Map<Integer, SkillSnapshot> skills) {
    if (goodId == 0) {
      return null;
    }
    if (shopType == ShopInventoryType.BLACK_MAGIC || shopType == ShopInventoryType.WHITE_MAGIC) {
      SkillSnapshot skill = skills.get(goodId);
      return skill == null ? null : skill.price();
    }
    ItemSnapshot item = items.get(goodId);
    return item == null ? null : item.price();
  }

  private static List<Integer> idRange(int first, int last) {
    List<Integer> ids = new ArrayList<>(last - first + 1);
    for (int id = first; id <= last; id++) {
      ids.add(id);
    }
    return ids;
  }
}
