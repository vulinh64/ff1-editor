package com.ff1.editor.data;

public record ShopGoodSnapshot(int id, String name, String category, Integer price) {

  @Override
  public String toString() {
    String label = name == null || name.isBlank() ? "Empty" : name;
    return "%d - %s".formatted(id, label);
  }
}
