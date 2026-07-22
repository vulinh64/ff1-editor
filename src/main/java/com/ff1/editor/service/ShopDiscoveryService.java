package com.ff1.editor.service;

import static com.ff1.editor.utils.EditorSupport.readBigEndianUnsignedShort;

import com.ff1.editor.data.ItemSnapshot;
import com.ff1.editor.data.ShopGoodSnapshot;
import com.ff1.editor.data.ShopLocationSnapshot;
import com.ff1.editor.data.ShopPriceSnapshot;
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
  public static final int ITEM_SHOP_TYPE = 2;
  public static final int WEAPON_SHOP_TYPE = 0;
  public static final int ARMOR_SHOP_TYPE = 1;
  public static final int BLACK_MAGIC_SHOP_TYPE = 3;
  public static final int WHITE_MAGIC_SHOP_TYPE = 4;
  public static final int SPECIAL_SHOP_TYPE = 5;
  public static final int SHOP_SLOT_COUNT = 5;
  public static final int SERVICE_PRICE_CHUNK_INDEX = 14;
  public static final int SERVICE_PRICE_RECORD_SIZE = 4;

  private static final int[] SHOP_CHUNKS = {7, 8, 6, 10, 9, 11};

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
                ShopServiceSnapshot.inventory("Weapon Shop", WEAPON_SHOP_TYPE, 0, "confirmed"),
                ShopServiceSnapshot.inventory("Armor Shop", ARMOR_SHOP_TYPE, 0, "confirmed"),
                ShopServiceSnapshot.inventory("Item Shop", ITEM_SHOP_TYPE, 0, "confirmed"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 1 White Magic Shop", WHITE_MAGIC_SHOP_TYPE, 0, "confirmed"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 1 Black Magic Shop", BLACK_MAGIC_SHOP_TYPE, 0, "confirmed"),
                ShopServiceSnapshot.price("Inn", 0, 0, "confirmed"))),
        new ShopLocationSnapshot(
            1,
            "Pravoka",
            List.of(
                ShopServiceSnapshot.inventory("Weapon Shop", WEAPON_SHOP_TYPE, 1, "wiki-backed"),
                ShopServiceSnapshot.inventory("Armor Shop", ARMOR_SHOP_TYPE, 1, "wiki-backed"),
                ShopServiceSnapshot.inventory("Item Shop", ITEM_SHOP_TYPE, 1, "confirmed"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 2 White Magic Shop", WHITE_MAGIC_SHOP_TYPE, 1, "confirmed"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 2 Black Magic Shop", BLACK_MAGIC_SHOP_TYPE, 1, "confirmed"))),
        new ShopLocationSnapshot(
            2,
            "Elfheim",
            List.of(
                ShopServiceSnapshot.inventory("Weapon Shop", WEAPON_SHOP_TYPE, 2, "wiki-backed"),
                ShopServiceSnapshot.inventory("Armor Shop", ARMOR_SHOP_TYPE, 2, "wiki-backed"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 3 White Magic Shop", WHITE_MAGIC_SHOP_TYPE, 2, "confirmed"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 3 Black Magic Shop", BLACK_MAGIC_SHOP_TYPE, 2, "confirmed"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 4 White Magic Shop", WHITE_MAGIC_SHOP_TYPE, 3, "confirmed"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 4 Black Magic Shop", BLACK_MAGIC_SHOP_TYPE, 3, "confirmed"),
                ShopServiceSnapshot.inventory("Item Shop", ITEM_SHOP_TYPE, 2, "confirmed"))),
        new ShopLocationSnapshot(
            3,
            "Melmond",
            List.of(
                ShopServiceSnapshot.inventory("Weapon Shop", WEAPON_SHOP_TYPE, 3, "wiki-backed"),
                ShopServiceSnapshot.inventory("Armor Shop", ARMOR_SHOP_TYPE, 3, "wiki-backed"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 5 White Magic Shop", WHITE_MAGIC_SHOP_TYPE, 4, "wiki-backed"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 5 Black Magic Shop", BLACK_MAGIC_SHOP_TYPE, 4, "wiki-backed"))),
        new ShopLocationSnapshot(
            4,
            "Crescent Lake",
            List.of(
                ShopServiceSnapshot.inventory("Weapon Shop", WEAPON_SHOP_TYPE, 5, "confirmed"),
                ShopServiceSnapshot.inventory("Armor Shop", ARMOR_SHOP_TYPE, 5, "confirmed"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 6 White Magic Shop", WHITE_MAGIC_SHOP_TYPE, 5, "wiki-backed"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 6 Black Magic Shop", BLACK_MAGIC_SHOP_TYPE, 5, "wiki-backed"))),
        new ShopLocationSnapshot(
            5,
            "Gaia",
            List.of(
                ShopServiceSnapshot.inventory("Weapon Shop", WEAPON_SHOP_TYPE, 4, "confirmed"),
                ShopServiceSnapshot.inventory("Armor Shop", ARMOR_SHOP_TYPE, 4, "confirmed"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 7 White Magic Shop", WHITE_MAGIC_SHOP_TYPE, 6, "likely"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 7 Black Magic Shop", BLACK_MAGIC_SHOP_TYPE, 6, "likely"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 8 White Magic Shop", WHITE_MAGIC_SHOP_TYPE, 7, "likely"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 8 Black Magic Shop", BLACK_MAGIC_SHOP_TYPE, 7, "likely"))),
        new ShopLocationSnapshot(
            6,
            "Onrac",
            List.of(
                ShopServiceSnapshot.inventory(
                    "Lvl. 7 White Magic Shop", WHITE_MAGIC_SHOP_TYPE, 8, "likely"),
                ShopServiceSnapshot.inventory(
                    "Lvl. 7 Black Magic Shop", BLACK_MAGIC_SHOP_TYPE, 8, "likely"))),
        new ShopLocationSnapshot(
            7,
            "Caravan",
            List.of(
                ShopServiceSnapshot.inventory("Special Shop", SPECIAL_SHOP_TYPE, 0, "confirmed"),
                ShopServiceSnapshot.inventory("Evolved Shop", SPECIAL_SHOP_TYPE, 1, "confirmed"))),
        new ShopLocationSnapshot(
            8,
            "Lufenia",
            List.of(
                ShopServiceSnapshot.inventory(
                    "Hidden White Magic Shop", WHITE_MAGIC_SHOP_TYPE, 9, "confirmed"),
                ShopServiceSnapshot.inventory(
                    "Hidden Black Magic Shop", BLACK_MAGIC_SHOP_TYPE, 9, "confirmed"))),
        new ShopLocationSnapshot(
            9,
            "<Unknown>",
            List.of(
                ShopServiceSnapshot.inventory("Item Shop Row 3", ITEM_SHOP_TYPE, 3, "unconfirmed"),
                ShopServiceSnapshot.inventory("Item Shop Row 4", ITEM_SHOP_TYPE, 4, "unconfirmed"),
                ShopServiceSnapshot.inventory("Item Shop Row 5", ITEM_SHOP_TYPE, 5, "unconfirmed"),
                ShopServiceSnapshot.inventory("Item Shop Row 6", ITEM_SHOP_TYPE, 6, "unconfirmed"),
                ShopServiceSnapshot.price("Inn Row 1", 1, 0, "unconfirmed"),
                ShopServiceSnapshot.price("Inn Row 2", 2, 0, "unconfirmed"),
                ShopServiceSnapshot.price("Inn Row 3", 3, 0, "unconfirmed"),
                ShopServiceSnapshot.price("Inn Row 4", 4, 0, "unconfirmed"),
                ShopServiceSnapshot.price("Inn Row 5", 5, 0, "unconfirmed"),
                ShopServiceSnapshot.price("Inn Row 6", 6, 0, "unconfirmed"))));
  }

  public List<ShopSlotSnapshot> slots(ShopServiceSnapshot service) {
    if (service == null || service.kind() != com.ff1.editor.data.ShopServiceKind.INVENTORY) {
      return List.of();
    }
    try {
      byte[] cp0 = Files.readAllBytes(workDir.resolve(ENTRY_NAME));
      Cp0ChunkTable table = new Cp0ChunkTable(cp0);
      int shopType = service.shopType();
      int rowIndex = service.rowIndex();
      int chunkIndex = chunkIndex(shopType);
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
                shopType,
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
          case WEAPON_SHOP_TYPE -> idRange(8, 47);
          case ARMOR_SHOP_TYPE -> idRange(49, 88);
          case ITEM_SHOP_TYPE -> idRange(1, 105);
          case BLACK_MAGIC_SHOP_TYPE -> idRange(33, 64);
          case WHITE_MAGIC_SHOP_TYPE -> idRange(1, 32);
          case SPECIAL_SHOP_TYPE -> idRange(1, 105);
          default -> List.of();
        };
    List<ShopGoodSnapshot> options = new ArrayList<>(ids.size() + 1);
    options.add(new ShopGoodSnapshot(0, StringUtils.EMPTY, "Empty", null));
    for (int id : ids) {
      String name = goodName(service.shopType(), id, items, skills);
      if (name.isBlank()) {
        continue;
      }
      options.add(
          new ShopGoodSnapshot(
              id,
              name,
              category(service.shopType(), id, items, skills),
              price(service.shopType(), id, items, skills)));
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
    if (shopType < 0 || shopType >= SHOP_CHUNKS.length) {
      throw new IllegalArgumentException("Shop type must be 0..5.");
    }
    return SHOP_CHUNKS[shopType];
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
      int shopType,
      int goodId,
      Map<Integer, ItemSnapshot> items,
      Map<Integer, SkillSnapshot> skills) {
    if (goodId == 0) {
      return StringUtils.EMPTY;
    }
    if (shopType == BLACK_MAGIC_SHOP_TYPE || shopType == WHITE_MAGIC_SHOP_TYPE) {
      SkillSnapshot skill = skills.get(goodId);
      return skill == null || skill.name().isBlank() ? "Spell " + goodId : skill.name();
    }
    ItemSnapshot item = items.get(goodId);
    return item == null || item.name().isBlank() ? "Item " + goodId : item.name();
  }

  private static String category(
      int shopType,
      int goodId,
      Map<Integer, ItemSnapshot> items,
      Map<Integer, SkillSnapshot> skills) {
    if (goodId == 0) {
      return "Empty";
    }
    if (shopType == BLACK_MAGIC_SHOP_TYPE || shopType == WHITE_MAGIC_SHOP_TYPE) {
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
      int shopType,
      int goodId,
      Map<Integer, ItemSnapshot> items,
      Map<Integer, SkillSnapshot> skills) {
    if (goodId == 0) {
      return null;
    }
    if (shopType == BLACK_MAGIC_SHOP_TYPE || shopType == WHITE_MAGIC_SHOP_TYPE) {
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
