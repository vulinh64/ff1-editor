package com.ff1.editor.service.patcher;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ff1.editor.data.EquipmentPermissionEdit;
import com.ff1.editor.data.HeroClassStatsEdit;
import com.ff1.editor.data.ItemCategory;
import com.ff1.editor.data.ItemPriceEdit;
import com.ff1.editor.data.MagicMatrixEdit;
import com.ff1.editor.data.SkillEffectEdit;
import com.ff1.editor.data.WeaponCastSpellEdit;
import com.ff1.editor.service.Cp0ChunkTable;
import com.ff1.editor.service.ItemEquipmentDiscoveryService;
import com.ff1.editor.service.MagicMatrixDiscoveryService;
import com.ff1.editor.service.SkillDiscoveryService;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DataPatcherIntegrityTest {

  @Test
  void heroClassStatsPatchWritesOnlyBaseClassTemplateBytes() {
    byte[] cp0 =
        new byte[HeroClassStatsPatcher.TABLE_OFFSET + HeroClassStatsPatcher.RECORD_SIZE * 6];
    Arrays.fill(cp0, (byte) 0x7f);

    HeroClassStatsPatcher.apply(
        cp0,
        HeroClassStatsEdit.builder()
            .classId(2)
            .hp(33)
            .strength(44)
            .agility(55)
            .intelligence(66)
            .stamina(77)
            .luck(88)
            .build());

    int offset = HeroClassStatsPatcher.TABLE_OFFSET + 2 * HeroClassStatsPatcher.RECORD_SIZE;
    assertArrayEquals(
        new byte[] {33, 44, 55, 66, 77, 88}, Arrays.copyOfRange(cp0, offset, offset + 6));
    assertEquals(0x7f, cp0[offset - 1] & 0xff);
    assertEquals(0x7f, cp0[offset + 6] & 0xff);
  }

  @Test
  void heroClassStatsPatchRejectsSignedHpOverflow() {
    byte[] cp0 =
        new byte[HeroClassStatsPatcher.TABLE_OFFSET + HeroClassStatsPatcher.RECORD_SIZE * 6];

    assertThrows(
        IllegalArgumentException.class,
        () ->
            HeroClassStatsPatcher.apply(
                cp0,
                HeroClassStatsEdit.builder()
                    .classId(0)
                    .hp(128)
                    .strength(1)
                    .agility(1)
                    .intelligence(1)
                    .stamina(1)
                    .luck(1)
                    .build()));
  }

  @Test
  void magicMatrixPatchWritesBigEndianPermissionMask() {
    byte[] cp0 =
        cp0(
            chunk(
                2
                    + ItemEquipmentDiscoveryService.ITEM_COUNT
                        * ItemEquipmentDiscoveryService.ITEM_METADATA_RECORD_SIZE,
                ItemEquipmentDiscoveryService.ITEM_COUNT),
            chunk(
                2 + SkillDiscoveryService.SKILL_COUNT * SkillDiscoveryService.SPELL_RECORD_SIZE,
                SkillDiscoveryService.SKILL_COUNT));
    int chunkOffset =
        new Cp0ChunkTable(cp0).chunkOffset(MagicMatrixDiscoveryService.SPELL_CHUNK_INDEX);

    MagicMatrixPatcher.apply(cp0, chunkOffset, new MagicMatrixEdit(30, 0x3f3f));

    int offset =
        chunkOffset
            + 2
            + 30 * MagicMatrixDiscoveryService.SPELL_RECORD_SIZE
            + MagicMatrixDiscoveryService.MASK_OFFSET_IN_RECORD;
    assertEquals(0x3f, cp0[offset] & 0xff);
    assertEquals(0x3f, cp0[offset + 1] & 0xff);
  }

  @Test
  void equipmentPermissionPatchWritesWeaponAndArmorMasks() {
    byte[] cp0 = standardCp0();
    Cp0ChunkTable table = new Cp0ChunkTable(cp0);

    ItemEquipmentPatcher.applyPermission(
        cp0, new EquipmentPermissionEdit(ItemCategory.WEAPON, 47, 0x3f3f));
    ItemEquipmentPatcher.applyPermission(
        cp0, new EquipmentPermissionEdit(ItemCategory.ARMOR, 80, 0x1001));

    int weaponOffset =
        table.chunkOffset(ItemEquipmentDiscoveryService.WEAPON_CHUNK_INDEX)
            + 2
            + 40 * ItemEquipmentDiscoveryService.WEAPON_RECORD_SIZE
            + ItemEquipmentPatcher.WEAPON_MASK_OFFSET_IN_RECORD;
    int armorOffset =
        table.chunkOffset(ItemEquipmentDiscoveryService.ARMOR_CHUNK_INDEX)
            + 2
            + 32 * ItemEquipmentDiscoveryService.ARMOR_RECORD_SIZE
            + ItemEquipmentPatcher.ARMOR_MASK_OFFSET_IN_RECORD;
    assertArrayEquals(
        new byte[] {0x3f, 0x3f}, Arrays.copyOfRange(cp0, weaponOffset, weaponOffset + 2));
    assertArrayEquals(
        new byte[] {0x10, 0x01}, Arrays.copyOfRange(cp0, armorOffset, armorOffset + 2));
  }

  @Test
  void itemPricePatchWritesSharedMetadataPrice() {
    byte[] cp0 = standardCp0();
    int chunkOffset =
        new Cp0ChunkTable(cp0).chunkOffset(ItemEquipmentDiscoveryService.ITEM_METADATA_CHUNK_INDEX);

    ItemPricePatcher.apply(cp0, chunkOffset, new ItemPriceEdit(88, 65535));

    int offset = chunkOffset + 2 + 88 * ItemEquipmentDiscoveryService.ITEM_METADATA_RECORD_SIZE;
    assertArrayEquals(
        new byte[] {(byte) 0xff, (byte) 0xff}, Arrays.copyOfRange(cp0, offset, offset + 2));
  }

  @Test
  void skillEffectPatchWritesPricePowerAndAccuracy() {
    byte[] cp0 = standardCp0();
    int chunkOffset = new Cp0ChunkTable(cp0).chunkOffset(SkillDiscoveryService.SPELL_CHUNK_INDEX);

    SkillEffectPatcher.apply(cp0, chunkOffset, new SkillEffectEdit(93, 12345, 200, 201));

    int offset = chunkOffset + 2 + 93 * SkillDiscoveryService.SPELL_RECORD_SIZE;
    assertArrayEquals(new byte[] {0x30, 0x39}, Arrays.copyOfRange(cp0, offset, offset + 2));
    assertEquals(200, cp0[offset + SkillDiscoveryService.POWER_OR_STATUS_OFFSET_IN_RECORD] & 0xff);
    assertEquals(201, cp0[offset + SkillDiscoveryService.ACCURACY_OFFSET_IN_RECORD] & 0xff);
  }

  @Test
  void weaponCastPatchWritesWeaponCastSkillId() {
    byte[] cp0 = standardCp0();
    int chunkOffset =
        new Cp0ChunkTable(cp0).chunkOffset(ItemEquipmentDiscoveryService.WEAPON_CHUNK_INDEX);

    WeaponCastSpellPatcher.apply(cp0, chunkOffset, new WeaponCastSpellEdit(36, 10));

    int offset =
        chunkOffset
            + 2
            + 29 * ItemEquipmentDiscoveryService.WEAPON_RECORD_SIZE
            + ItemEquipmentDiscoveryService.WEAPON_CAST_SPELL_OFFSET_IN_RECORD;
    assertEquals(10, cp0[offset] & 0xff);
  }

  @Test
  void corneliaArmorPatchFillsOnlyConfirmedEmptySlots() {
    byte[] cp0 = standardCp0();

    assertEquals(CorneliaArmorShopPatcher.State.ORIGINAL, CorneliaArmorShopPatcher.state(cp0));
    CorneliaArmorShopPatcher.apply(cp0);

    int offset = CorneliaArmorShopPatcher.corneliaArmorShopRowOffset(cp0);
    assertArrayEquals(new byte[] {49, 50, 51, 80, 88}, Arrays.copyOfRange(cp0, offset, offset + 5));
    assertEquals(CorneliaArmorShopPatcher.State.PATCHED, CorneliaArmorShopPatcher.state(cp0));
  }

  @Test
  void corneliaArmorPatchRejectsUnexpectedShopRow() {
    byte[] cp0 = standardCp0();
    cp0[CorneliaArmorShopPatcher.corneliaArmorShopRowOffset(cp0)] = 52;

    assertEquals(CorneliaArmorShopPatcher.State.UNKNOWN, CorneliaArmorShopPatcher.state(cp0));
    assertThrows(IllegalStateException.class, () -> CorneliaArmorShopPatcher.apply(cp0));
  }

  private static byte[] standardCp0() {
    byte[] itemMetadata =
        chunk(
            2
                + ItemEquipmentDiscoveryService.ITEM_COUNT
                    * ItemEquipmentDiscoveryService.ITEM_METADATA_RECORD_SIZE,
            ItemEquipmentDiscoveryService.ITEM_COUNT);
    byte[] spells =
        chunk(
            2 + SkillDiscoveryService.SKILL_COUNT * SkillDiscoveryService.SPELL_RECORD_SIZE,
            SkillDiscoveryService.SKILL_COUNT);
    byte[] armor =
        chunk(
            2
                + ItemEquipmentDiscoveryService.ARMOR_COUNT
                    * ItemEquipmentDiscoveryService.ARMOR_RECORD_SIZE,
            ItemEquipmentDiscoveryService.ARMOR_COUNT);
    byte[] weapons =
        chunk(
            2
                + ItemEquipmentDiscoveryService.WEAPON_COUNT
                    * ItemEquipmentDiscoveryService.WEAPON_RECORD_SIZE,
            ItemEquipmentDiscoveryService.WEAPON_COUNT);
    byte[] itemShop = shopChunk(7, new int[] {1, 2, 4, 0, 0}, new int[] {1, 2, 3, 4, 5, 6});
    byte[] weaponShop =
        shopChunk(
            CorneliaWeaponShopPatcher.SHOP_ROW_COUNT,
            new int[] {8, 9, 10, 11, 12},
            new int[] {8, 9, 10, 11, 12});
    byte[] armorShop =
        shopChunk(
            CorneliaArmorShopPatcher.SHOP_ROW_COUNT,
            new int[] {49, 50, 51, 0, 0},
            new int[] {49, 50, 51, 0, 0});
    return cp0(
        itemMetadata,
        spells,
        armor,
        weapons,
        new byte[0],
        new byte[0],
        itemShop,
        weaponShop,
        armorShop);
  }

  private static byte[] shopChunk(int rows, int[] firstRow, int[] fillerRow) {
    byte[] chunk = chunk(2 + rows * 5, rows);
    for (int row = 0; row < rows; row++) {
      int[] source = row == 0 ? firstRow : fillerRow;
      for (int slot = 0; slot < 5; slot++) {
        chunk[2 + row * 5 + slot] = (byte) source[slot];
      }
    }
    return chunk;
  }

  private static byte[] chunk(int length, int count) {
    byte[] chunk = new byte[length];
    chunk[0] = (byte) (count >>> 8);
    chunk[1] = (byte) count;
    return chunk;
  }

  private static byte[] cp0(byte[]... chunks) {
    byte[] data =
        new byte[30 * Integer.BYTES + Arrays.stream(chunks).mapToInt(c -> c.length).sum()];
    int offset = 30 * Integer.BYTES;
    for (int i = 0; i < chunks.length; i++) {
      int length = chunks[i].length;
      data[i * Integer.BYTES] = (byte) length;
      data[i * Integer.BYTES + 1] = (byte) (length >>> 8);
      data[i * Integer.BYTES + 2] = (byte) (length >>> 16);
      data[i * Integer.BYTES + 3] = (byte) (length >>> 24);
      System.arraycopy(chunks[i], 0, data, offset, length);
      offset += length;
    }
    return data;
  }
}
