package com.ff1.editor.data;

public enum ShopInventoryType {
  WEAPON(0, 7),
  ARMOR(1, 8),
  ITEM(2, 6),
  BLACK_MAGIC(3, 10),
  WHITE_MAGIC(4, 9),
  SPECIAL(5, 11);

  private final int id;
  private final int chunkIndex;

  ShopInventoryType(int id, int chunkIndex) {
    this.id = id;
    this.chunkIndex = chunkIndex;
  }

  public int id() {
    return id;
  }

  public int chunkIndex() {
    return chunkIndex;
  }

  public static ShopInventoryType fromId(int id) {
    for (ShopInventoryType type : values()) {
      if (type.id == id) {
        return type;
      }
    }
    throw new IllegalArgumentException("Shop type must be 0..5.");
  }
}
