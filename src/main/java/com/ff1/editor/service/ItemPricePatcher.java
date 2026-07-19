package com.ff1.editor.service;

import com.ff1.editor.data.ItemPriceEdit;

public final class ItemPricePatcher {

  private ItemPricePatcher() {}

  public static void apply(byte[] cp0, int chunkOffset, ItemPriceEdit edit) {
    if (edit.itemId() < 0 || edit.itemId() >= ItemEquipmentDiscoveryService.ITEM_COUNT) {
      throw new IllegalArgumentException("Item id must be 0..105.");
    }
    if (edit.price() < 0 || edit.price() > 0xffff) {
      throw new IllegalArgumentException("Item price must be 0..65535.");
    }
    int offset =
        chunkOffset
            + Short.BYTES
            + edit.itemId() * ItemEquipmentDiscoveryService.ITEM_METADATA_RECORD_SIZE;
    cp0[offset] = (byte) (edit.price() >>> 8);
    cp0[offset + 1] = (byte) edit.price();
  }
}
