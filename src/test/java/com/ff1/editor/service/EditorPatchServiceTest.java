package com.ff1.editor.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ff1.editor.data.BuildResult;
import com.ff1.editor.data.EditorWorkspace;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EditorPatchServiceTest {

  @TempDir private Path tempDir;

  @Test
  void buildPatchReplacesOnlyRequestedEntries() throws Exception {
    Path inputJar = tempDir.resolve("ff1.jar");
    Path outputJar = tempDir.resolve("ff1-patched.jar");
    writeJar(inputJar, Map.of("cp0", new byte[] {1, 2, 3}, "g.class", new byte[] {4, 5, 6}));
    EditorWorkspace workspace =
        EditorWorkspace.builder().inputJar(inputJar).outputJar(outputJar).build();

    BuildResult result =
        new EditorPatchService().buildPatch(workspace, Map.of("cp0", new byte[] {9, 8, 7}));

    assertEquals(outputJar.toAbsolutePath().normalize(), result.outputJar());
    assertEquals("replaced 1 jar entries", result.summary());
    assertArrayEquals(new byte[] {9, 8, 7}, readJarEntry(result.outputJar(), "cp0"));
    assertArrayEquals(new byte[] {4, 5, 6}, readJarEntry(result.outputJar(), "g.class"));
    assertArrayEquals(new byte[] {1, 2, 3}, readJarEntry(inputJar, "cp0"));
  }

  @Test
  void buildPatchUsesNumberedSuffixWhenOutputExists() throws Exception {
    Path inputJar = tempDir.resolve("ff1.jar");
    Path outputJar = tempDir.resolve("ff1-patched-0001.jar");
    Path suffixedOutputJar = tempDir.resolve("ff1-patched-0002.jar");
    writeJar(inputJar, Map.of("cp0", new byte[] {1}));
    Files.write(outputJar, new byte[] {0});
    EditorWorkspace workspace =
        EditorWorkspace.builder().inputJar(inputJar).outputJar(outputJar).build();

    BuildResult result =
        new EditorPatchService().buildPatch(workspace, Map.of("cp0", new byte[] {2}));

    assertEquals(suffixedOutputJar.toAbsolutePath().normalize(), result.outputJar());
    assertArrayEquals(new byte[] {2}, readJarEntry(result.outputJar(), "cp0"));
    assertArrayEquals(new byte[] {0}, Files.readAllBytes(outputJar));
  }

  @Test
  void buildPatchRejectsMissingReplacementEntryWithoutWritingOutputJar() throws Exception {
    Path inputJar = tempDir.resolve("ff1.jar");
    Path outputJar = tempDir.resolve("ff1-patched.jar");
    writeJar(inputJar, Map.of("cp0", new byte[] {1}));
    EditorWorkspace workspace =
        EditorWorkspace.builder().inputJar(inputJar).outputJar(outputJar).build();

    assertThrows(
        IllegalArgumentException.class,
        () -> new EditorPatchService().buildPatch(workspace, Map.of("missing", new byte[] {2})));
    assertFalse(Files.exists(outputJar));
  }

  @Test
  void nextAvailableOutputJarPreservesExplicitPathUntilItExists() throws Exception {
    Path outputJar = tempDir.resolve("ff1-patched.jar");

    assertEquals(
        outputJar.toAbsolutePath().normalize(),
        EditorPatchService.nextAvailableOutputJar(outputJar));
    Files.write(outputJar, new byte[] {0});
    assertEquals(
        tempDir.resolve("ff1-patched-0001.jar").toAbsolutePath().normalize(),
        EditorPatchService.nextAvailableOutputJar(outputJar));
  }

  private static void writeJar(Path path, Map<String, byte[]> entries) throws IOException {
    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(path))) {
      for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
        out.putNextEntry(new JarEntry(entry.getKey()));
        out.write(entry.getValue());
        out.closeEntry();
      }
    }
  }

  private static byte[] readJarEntry(Path path, String entryName) throws IOException {
    try (JarInputStream in = new JarInputStream(Files.newInputStream(path))) {
      JarEntry entry;
      while ((entry = in.getNextJarEntry()) != null) {
        if (entryName.equals(entry.getName())) {
          return in.readAllBytes();
        }
      }
    }
    throw new AssertionError("Missing jar entry " + entryName);
  }
}
