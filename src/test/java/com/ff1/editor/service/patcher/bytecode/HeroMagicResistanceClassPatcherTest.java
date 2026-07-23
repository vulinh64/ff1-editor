package com.ff1.editor.service.patcher.bytecode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HeroMagicResistanceClassPatcherTest {

  private static final Path STOCK_G_CLASS = Path.of("ff1-jar", "g.class");

  @Test
  void stockGClassDetectsOriginal() throws IOException {
    assertEquals(PatcherState.ORIGINAL, HeroMagicResistanceClassPatcher.state(stockGClass()));
  }

  @Test
  void patchedGClassDetectsPatched() throws IOException {
    byte[] patched = HeroMagicResistanceClassPatcher.apply(stockGClass());

    assertEquals(PatcherState.PATCHED, HeroMagicResistanceClassPatcher.state(patched));
  }

  @Test
  void applyIsIdempotent() throws IOException {
    byte[] patched = HeroMagicResistanceClassPatcher.apply(stockGClass());

    assertArrayEquals(patched, HeroMagicResistanceClassPatcher.apply(patched));
  }

  @Test
  void applyChangesStockClassBytes() throws IOException {
    byte[] original = stockGClass();
    byte[] patched = HeroMagicResistanceClassPatcher.apply(original);

    assertNotEquals(0, original.length);
    assertNotEquals(java.util.Arrays.toString(original), java.util.Arrays.toString(patched));
  }

  @Test
  void unsupportedLayoutIsRejected() {
    byte[] invalidClass = new byte[] {0, 1, 2, 3};

    assertEquals(PatcherState.UNKNOWN, HeroMagicResistanceClassPatcher.state(invalidClass));
    assertThrows(
        IllegalStateException.class, () -> HeroMagicResistanceClassPatcher.apply(invalidClass));
  }

  private static byte[] stockGClass() throws IOException {
    return Files.readAllBytes(STOCK_G_CLASS);
  }
}
