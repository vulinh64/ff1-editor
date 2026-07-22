package com.ff1.editor.data;

public record ShopSlotSnapshot(
    int shopType,
    int rowIndex,
    int slotIndex,
    int goodId,
    String goodName,
    String category,
    Integer price,
    String sourceEntry,
    int sourceOffset) {}
