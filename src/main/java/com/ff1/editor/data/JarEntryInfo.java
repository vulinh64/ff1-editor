package com.ff1.editor.data;

import lombok.Builder;
import lombok.With;

@Builder
@With
public record JarEntryInfo(String name, long size, long compressedSize, long crc)
    implements Comparable<JarEntryInfo> {

  public boolean isClass() {
    return name.endsWith(".class");
  }

  public boolean isLikelyBinaryData() {
    return !isClass()
        && !name.startsWith("META-INF/")
        && !name.endsWith(".png")
        && !name.contains(".");
  }

  @Override
  public int compareTo(JarEntryInfo other) {
    return name.compareTo(other.name);
  }
}
