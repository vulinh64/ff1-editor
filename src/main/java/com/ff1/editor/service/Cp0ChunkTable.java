package com.ff1.editor.service;

import static com.ff1.editor.utils.EditorSupport.readLittleEndianInt;

import java.util.Arrays;

public final class Cp0ChunkTable {

  private static final int CHUNK_COUNT = 30;

  private final byte[] data;
  private final int[] lengths;
  private final int[] offsets;

  public Cp0ChunkTable(byte[] data) {
    if (data == null || data.length < CHUNK_COUNT * Integer.BYTES) {
      throw new IllegalArgumentException("cp0 is too short for the chunk length table.");
    }
    this.data = data;
    this.lengths = new int[CHUNK_COUNT];
    this.offsets = new int[CHUNK_COUNT];
    int offset = CHUNK_COUNT * Integer.BYTES;
    for (int i = 0; i < CHUNK_COUNT; i++) {
      lengths[i] = readLittleEndianInt(data, i * Integer.BYTES);
      offsets[i] = offset;
      offset += lengths[i];
    }
    if (offset > data.length) {
      throw new IllegalArgumentException("cp0 chunk table points beyond the file length.");
    }
  }

  public int chunkOffset(int index) {
    checkIndex(index);
    return offsets[index];
  }

  public int chunkLength(int index) {
    checkIndex(index);
    return lengths[index];
  }

  public byte[] chunk(int index) {
    checkIndex(index);
    return Arrays.copyOfRange(data, offsets[index], offsets[index] + lengths[index]);
  }

  private static void checkIndex(int index) {
    if (index < 0 || index >= CHUNK_COUNT) {
      throw new IllegalArgumentException("cp0 chunk index must be 0..29.");
    }
  }
}
