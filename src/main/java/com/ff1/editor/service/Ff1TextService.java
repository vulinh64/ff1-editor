package com.ff1.editor.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Ff1TextService {

  private final Path extractedDir;
  private char[] alphabet;

  public Ff1TextService(Path extractedDir) {
    this.extractedDir = extractedDir;
  }

  public byte[] readChunk(String prefix, int[] boundaries, int id) throws IOException {
    int group = groupFor(boundaries, id);
    int firstId = boundaries[group];
    int localIndex = id - firstId;
    byte[] data = Files.readAllBytes(extractedDir.resolve(prefix + group));
    int count = boundaries[group + 1] - boundaries[group];
    int start = count * 4;
    for (int i = 0; i < localIndex; i++) {
      start += readInt(data, i * 4);
    }
    int length = readInt(data, localIndex * 4);
    byte[] chunk = new byte[length];
    System.arraycopy(data, start, chunk, 0, length);
    return chunk;
  }

  public String decodeText(byte[] encoded) throws IOException {
    char[] table = alphabet();
    StringBuilder out = new StringBuilder();
    for (byte value : encoded) {
      int code = value & 0xff;
      if (code == 0xfe) {
        out.append("\\n");
      } else if (code < table.length) {
        out.append(table[code]);
      } else {
        out.append('<').append(code).append('>');
      }
    }
    return out.toString();
  }

  public void searchEncodedText(String text) throws IOException {
    byte[] needle = encodeText(text);
    log.info("Search `{}` as {}", text, HexFormat.of().formatHex(needle));
    boolean found = false;
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(extractedDir)) {
      for (Path file : stream) {
        if (!Files.isRegularFile(file)) {
          continue;
        }
        byte[] haystack = Files.readAllBytes(file);
        List<Integer> offsets = findAll(haystack, needle);
        for (int offset : offsets) {
          found = true;
          log.info("{} @ 0x{}", file.getFileName(), "%08x".formatted(offset));
        }
      }
    }
    if (!found) {
      log.info("No matches");
    }
  }

  public byte[] encodeText(String text) throws IOException {
    char[] table = alphabet();
    byte[] encoded = new byte[text.length()];
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      if (ch == '\n') {
        encoded[i] = (byte) 0xfe;
        continue;
      }
      int code = -1;
      for (int tableIndex = 0; tableIndex < table.length; tableIndex++) {
        if (table[tableIndex] == ch) {
          code = tableIndex;
          break;
        }
      }
      if (code < 0) {
        throw new IllegalArgumentException("Character is not in FF1 font table: " + ch);
      }
      encoded[i] = (byte) code;
    }
    return encoded;
  }

  private char[] alphabet() throws IOException {
    if (alphabet != null) {
      return alphabet;
    }
    byte[] fn0 = readChunk("fn", new int[] {0, 3}, 0);
    int count = readUnsignedShort(fn0, 0);
    char[] table = new char[count];
    int offset = 2;
    for (int i = 0; i < count; i++) {
      table[i] = (char) readUnsignedShort(fn0, offset);
      offset += 2;
    }
    alphabet = table;
    return alphabet;
  }

  private static int groupFor(int[] boundaries, int id) {
    for (int i = 0; i < boundaries.length - 1; i++) {
      if (id >= boundaries[i] && id < boundaries[i + 1]) {
        return i;
      }
    }
    throw new IllegalArgumentException("Text id " + id + " is outside boundaries");
  }

  private static int readInt(byte[] data, int offset) {
    return (data[offset] & 0xff)
        | ((data[offset + 1] & 0xff) << 8)
        | ((data[offset + 2] & 0xff) << 16)
        | ((data[offset + 3] & 0xff) << 24);
  }

  private static int readUnsignedShort(byte[] data, int offset) {
    return ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff);
  }

  private static List<Integer> findAll(byte[] haystack, byte[] needle) {
    List<Integer> offsets = new ArrayList<>();
    if (needle.length == 0 || haystack.length < needle.length) {
      return offsets;
    }
    for (int i = 0; i <= haystack.length - needle.length; i++) {
      boolean match = true;
      for (int j = 0; j < needle.length; j++) {
        if (haystack[i + j] != needle[j]) {
          match = false;
          break;
        }
      }
      if (match) {
        offsets.add(i);
      }
    }
    return offsets;
  }
}
