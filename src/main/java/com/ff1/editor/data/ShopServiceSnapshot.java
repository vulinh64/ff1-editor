package com.ff1.editor.data;

public record ShopServiceSnapshot(
    ShopServiceName name,
    ShopServiceKind kind,
    ShopInventoryType shopType,
    Integer rowIndex,
    Integer serviceColumn,
    ShopMappingStatus status) {

  public static ShopServiceSnapshot inventory(
      ShopServiceName name, ShopInventoryType shopType, int rowIndex, ShopMappingStatus status) {
    return new ShopServiceSnapshot(
        name, ShopServiceKind.INVENTORY, shopType, rowIndex, null, status);
  }

  public static ShopServiceSnapshot price(
      ShopServiceName name, int rowIndex, int serviceColumn, ShopMappingStatus status) {
    return new ShopServiceSnapshot(
        name, ShopServiceKind.PRICE, null, rowIndex, serviceColumn, status);
  }

  @Override
  public String toString() {
    String displayName = name.label();
    return status == null ? displayName : "%s (%s)".formatted(displayName, status.label());
  }
}
