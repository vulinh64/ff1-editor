package com.ff1.editor.data;

import lombok.Builder;

@Builder
public record ItemPriceEdit(int itemId, int price) {}
