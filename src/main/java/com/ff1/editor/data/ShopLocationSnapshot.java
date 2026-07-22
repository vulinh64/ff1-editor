package com.ff1.editor.data;

import java.util.List;

public record ShopLocationSnapshot(int id, String name, List<ShopServiceSnapshot> services) {

  @Override
  public String toString() {
    return "%d - %s".formatted(id, name);
  }
}
