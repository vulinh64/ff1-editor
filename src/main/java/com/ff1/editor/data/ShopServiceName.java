package com.ff1.editor.data;

public enum ShopServiceName {
  WEAPON_SHOP("Weapon Shop"),
  ARMOR_SHOP("Armor Shop"),
  ITEM_SHOP("Item Shop"),
  LEVEL_1_WHITE_MAGIC_SHOP("Lvl. 1 White Magic Shop"),
  LEVEL_1_BLACK_MAGIC_SHOP("Lvl. 1 Black Magic Shop"),
  LEVEL_2_WHITE_MAGIC_SHOP("Lvl. 2 White Magic Shop"),
  LEVEL_2_BLACK_MAGIC_SHOP("Lvl. 2 Black Magic Shop"),
  LEVEL_3_WHITE_MAGIC_SHOP("Lvl. 3 White Magic Shop"),
  LEVEL_3_BLACK_MAGIC_SHOP("Lvl. 3 Black Magic Shop"),
  LEVEL_4_WHITE_MAGIC_SHOP("Lvl. 4 White Magic Shop"),
  LEVEL_4_BLACK_MAGIC_SHOP("Lvl. 4 Black Magic Shop"),
  LEVEL_5_WHITE_MAGIC_SHOP("Lvl. 5 White Magic Shop"),
  LEVEL_5_BLACK_MAGIC_SHOP("Lvl. 5 Black Magic Shop"),
  LEVEL_6_WHITE_MAGIC_SHOP("Lvl. 6 White Magic Shop"),
  LEVEL_6_BLACK_MAGIC_SHOP("Lvl. 6 Black Magic Shop"),
  LEVEL_7_WHITE_MAGIC_SHOP("Lvl. 7 White Magic Shop"),
  LEVEL_7_BLACK_MAGIC_SHOP("Lvl. 7 Black Magic Shop"),
  LEVEL_8_WHITE_MAGIC_SHOP("Lvl. 8 White Magic Shop"),
  LEVEL_8_BLACK_MAGIC_SHOP("Lvl. 8 Black Magic Shop"),
  HIDDEN_WHITE_MAGIC_SHOP("Hidden White Magic Shop"),
  HIDDEN_BLACK_MAGIC_SHOP("Hidden Black Magic Shop"),
  SPECIAL_SHOP("Special Shop"),
  EVOLVED_SHOP("Evolved Shop"),
  INN("Inn"),
  ITEM_SHOP_ROW_3("Item Shop Row 3"),
  ITEM_SHOP_ROW_4("Item Shop Row 4"),
  ITEM_SHOP_ROW_5("Item Shop Row 5"),
  ITEM_SHOP_ROW_6("Item Shop Row 6"),
  INN_ROW_1("Inn Row 1"),
  INN_ROW_2("Inn Row 2"),
  INN_ROW_3("Inn Row 3"),
  INN_ROW_4("Inn Row 4"),
  INN_ROW_5("Inn Row 5"),
  INN_ROW_6("Inn Row 6");

  private final String displayName;

  ShopServiceName(String displayName) {
    this.displayName = displayName;
  }

  public String displayName() {
    return displayName;
  }
}
