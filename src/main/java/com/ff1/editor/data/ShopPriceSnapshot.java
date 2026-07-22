package com.ff1.editor.data;

public record ShopPriceSnapshot(
    int rowIndex, int serviceColumn, int price, String sourceEntry, int sourceOffset) {}
