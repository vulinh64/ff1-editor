package com.ff1.editor.data;

public record ShopServiceSnapshot(
    String name,
    ShopServiceKind kind,
    Integer shopType,
    Integer rowIndex,
    Integer serviceColumn,
    String status) {

  public static ShopServiceSnapshot inventory(
      String name, int shopType, int rowIndex, String status) {
    return new ShopServiceSnapshot(
        name, ShopServiceKind.INVENTORY, shopType, rowIndex, null, status);
  }

  public static ShopServiceSnapshot price(
      String name, int rowIndex, int serviceColumn, String status) {
    return new ShopServiceSnapshot(
        name, ShopServiceKind.PRICE, null, rowIndex, serviceColumn, status);
  }

  @Override
  public String toString() {
    return status == null || status.isBlank() ? name : "%s (%s)".formatted(name, status);
  }
}
