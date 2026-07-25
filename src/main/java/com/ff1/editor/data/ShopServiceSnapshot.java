package com.ff1.editor.data;

import lombok.Builder;

@Builder
public record ShopServiceSnapshot(
    ShopServiceName name,
    ShopServiceKind kind,
    ShopInventoryType shopType,
    Integer rowIndex,
    Integer serviceColumn,
    ShopMappingStatus status) {

  public static ShopServiceSnapshot inventory(
      ShopServiceName name, ShopInventoryType shopType, int rowIndex, ShopMappingStatus status) {
    return ShopServiceSnapshot.builder()
        .name(name)
        .kind(ShopServiceKind.INVENTORY)
        .shopType(shopType)
        .rowIndex(rowIndex)
        .status(status)
        .build();
  }

  public static ShopServiceSnapshot price(
      ShopServiceName name, int rowIndex, int serviceColumn, ShopMappingStatus status) {
    return ShopServiceSnapshot.builder()
        .name(name)
        .kind(ShopServiceKind.PRICE)
        .rowIndex(rowIndex)
        .serviceColumn(serviceColumn)
        .status(status)
        .build();
  }

  @Override
  public String toString() {
    String displayName = name.label();

    return status == null ? displayName : "%s (%s)".formatted(displayName, status.label());
  }
}
