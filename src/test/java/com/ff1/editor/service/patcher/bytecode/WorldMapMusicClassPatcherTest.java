package com.ff1.editor.service.patcher.bytecode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class WorldMapMusicClassPatcherTest {

  private static final Path STOCK_I_CLASS = Path.of("ff1-jar", "i.class");

  @Test
  void detectsStockIClassAsOriginal() throws IOException {
    assertEquals(PatcherState.ORIGINAL, WorldMapMusicClassPatcher.state(stockIClass()));
  }

  @Test
  void applyRemovesWorldMapOpenMusicStop() throws IOException {
    byte[] patched = WorldMapMusicClassPatcher.apply(stockIClass());

    assertEquals(PatcherState.PATCHED, WorldMapMusicClassPatcher.state(patched));
  }

  @Test
  void applyIsIdempotent() throws IOException {
    byte[] patched = WorldMapMusicClassPatcher.apply(stockIClass());

    assertArrayEquals(patched, WorldMapMusicClassPatcher.apply(patched));
  }

  @Test
  void remainsDetectableWithOtherIClassPatches() throws IOException {
    byte[] patched = WorldMapMusicClassPatcher.apply(stockIClass());
    patched = CottageReviveClassPatcher.apply(patched);
    patched = AirshipLandingClassPatcher.apply(patched);

    assertEquals(PatcherState.PATCHED, WorldMapMusicClassPatcher.state(patched));
    assertEquals(PatcherState.PATCHED, CottageReviveClassPatcher.state(patched));
    assertEquals(PatcherState.PATCHED, AirshipLandingClassPatcher.state(patched));
  }

  @Test
  void invalidClassIsUnknown() {
    assertEquals(PatcherState.UNKNOWN, WorldMapMusicClassPatcher.state(new byte[] {1, 2, 3}));
  }

  private static byte[] stockIClass() throws IOException {
    return Files.readAllBytes(STOCK_I_CLASS);
  }
}
