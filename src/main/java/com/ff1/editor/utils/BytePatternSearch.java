package com.ff1.editor.utils;

import java.util.Arrays;

public final class BytePatternSearch {

  private BytePatternSearch() {}

  public static int count(byte[] data, byte[] pattern) {
    int matches = 0;
    int offset = 0;
    while ((offset = indexOf(data, pattern, offset)) >= 0) {
      matches++;
      offset += pattern.length;
    }
    return matches;
  }

  public static int indexOf(byte[] data, byte[] pattern, int start) {
    if (pattern.length == 0 || data.length < pattern.length) {
      return -1;
    }
    int max = data.length - pattern.length;
    for (int i = Math.max(0, start); i <= max; i++) {
      if (Arrays.equals(data, i, i + pattern.length, pattern, 0, pattern.length)) {
        return i;
      }
    }
    return -1;
  }
}
