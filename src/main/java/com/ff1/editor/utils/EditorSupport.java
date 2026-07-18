package com.ff1.editor.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class EditorSupport {

  private static final String APP_DIR = ".ff1-editor";

  private EditorSupport() {}

  public static Path editorUserPath(String child) {
    return Path.of(System.getProperty("user.home"), APP_DIR, child);
  }

  public static byte[] readZipEntry(ZipInputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int read;
    while ((read = in.read(buffer)) >= 0) {
      out.write(buffer, 0, read);
    }
    return out.toByteArray();
  }

  public static void replaceJarEntries(
      Path inputJar, Path outputJar, Map<String, byte[]> replacements) throws IOException {
    Set<String> seen = new HashSet<>();
    try (ZipInputStream in = new ZipInputStream(Files.newInputStream(inputJar));
        ZipOutputStream out =
            new ZipOutputStream(
                Files.newOutputStream(
                    outputJar, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
      ZipEntry entry;
      while ((entry = in.getNextEntry()) != null) {
        ZipEntry copy = new ZipEntry(entry.getName());
        copy.setTime(entry.getTime());
        out.putNextEntry(copy);
        byte[] replacement = replacements.get(entry.getName());
        out.write(replacement == null ? readZipEntry(in) : replacement);
        if (replacement != null) {
          seen.add(entry.getName());
        }
        out.closeEntry();
        in.closeEntry();
      }
      for (String replacementName : replacements.keySet()) {
        if (!seen.contains(replacementName)) {
          throw new IllegalArgumentException(
              "Replacement entry does not exist in input jar: " + replacementName);
        }
      }
    }
  }

  public static int u8(byte value) {
    return value & 0xff;
  }

  public static int readLittleEndianInt(byte[] bytes, int offset) {
    return u8(bytes[offset])
        | (u8(bytes[offset + 1]) << 8)
        | (u8(bytes[offset + 2]) << 16)
        | (u8(bytes[offset + 3]) << 24);
  }

  public static int readBigEndianUnsignedShort(byte[] bytes, int offset) {
    return (u8(bytes[offset]) << 8) | u8(bytes[offset + 1]);
  }
}
