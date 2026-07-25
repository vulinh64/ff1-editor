package com.ff1.editor.data;

import com.ff1.editor.utils.DataUtils;
import java.util.List;

public record ShopLocationSnapshot(int id, String name, List<ShopServiceSnapshot> services) {

  public ShopLocationSnapshot {
    services = DataUtils.emptyListIfNull(services);
  }

  @Override
  public String toString() {
    return "%d - %s".formatted(id, name);
  }
}
