package com.ff1.editor.service.patcher;

import com.ff1.editor.data.ShopInventoryEdit;
import com.ff1.editor.data.ShopPriceEdit;
import com.ff1.editor.service.Cp0ChunkTable;
import com.ff1.editor.service.ShopDiscoveryService;

public final class ShopPatcher {

  private ShopPatcher() {}

  public static void applyInventory(byte[] cp0, ShopInventoryEdit edit) {
    if (edit.slotIndex() < 0 || edit.slotIndex() >= ShopDiscoveryService.SHOP_SLOT_COUNT) {
      throw new IllegalArgumentException("Shop inventory slot must be 0..4.");
    }
    if (edit.goodId() < 0 || edit.goodId() > 255) {
      throw new IllegalArgumentException("Shop inventory good id must be 0..255.");
    }
    Cp0ChunkTable table = new Cp0ChunkTable(cp0);
    int chunkIndex = ShopDiscoveryService.chunkIndex(edit.shopType());
    int chunkOffset = table.chunkOffset(chunkIndex);
    int rowCount = ((cp0[chunkOffset] & 0xff) << 8) | (cp0[chunkOffset + 1] & 0xff);
    if (edit.rowIndex() < 0 || edit.rowIndex() >= rowCount) {
      throw new IllegalArgumentException("Shop inventory row is outside the chunk row count.");
    }
    int offset =
        chunkOffset
            + Short.BYTES
            + edit.rowIndex() * ShopDiscoveryService.SHOP_SLOT_COUNT
            + edit.slotIndex();
    cp0[offset] = (byte) edit.goodId();
  }

  public static void applyPrice(byte[] cp0, ShopPriceEdit edit) {
    if (edit.serviceColumn() < 0 || edit.serviceColumn() > 1) {
      throw new IllegalArgumentException("Shop service price column must be 0..1.");
    }
    if (edit.price() < 0 || edit.price() > 0xffff) {
      throw new IllegalArgumentException("Shop service price must be 0..65535.");
    }
    Cp0ChunkTable table = new Cp0ChunkTable(cp0);
    int chunkOffset = table.chunkOffset(ShopDiscoveryService.SERVICE_PRICE_CHUNK_INDEX);
    int rowCount = ((cp0[chunkOffset] & 0xff) << 8) | (cp0[chunkOffset + 1] & 0xff);
    if (edit.rowIndex() < 0 || edit.rowIndex() >= rowCount) {
      throw new IllegalArgumentException("Shop service price row is outside the chunk row count.");
    }
    int offset =
        chunkOffset
            + Short.BYTES
            + edit.rowIndex() * ShopDiscoveryService.SERVICE_PRICE_RECORD_SIZE
            + edit.serviceColumn() * Short.BYTES;
    cp0[offset] = (byte) (edit.price() >>> 8);
    cp0[offset + 1] = (byte) edit.price();
  }
}
