package com.ff1.editor.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataUtils {

  public static <T> List<T> emptyListIfNull(List<T> list) {
    return list != null ? List.copyOf(list) : Collections.emptyList();
  }

  public static <K, V> Map<K, V> emptyMapIfNull(Map<K, V> map) {
    return map != null ? Map.copyOf(map) : Collections.emptyMap();
  }
}
