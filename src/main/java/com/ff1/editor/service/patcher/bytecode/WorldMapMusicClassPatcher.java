package com.ff1.editor.service.patcher.bytecode;

import static com.ff1.editor.utils.BytePatternSearch.count;
import static com.ff1.editor.utils.BytePatternSearch.indexOf;

import lombok.extern.slf4j.Slf4j;

/** Patches i.class so opening the world-map overlay does not stop field music. */
@Slf4j
public final class WorldMapMusicClassPatcher {

  public static final String ENTRY_NAME = FifteenSpellChargeRecoveryClassPatcher.ENTRY_NAME;

  private static final byte[] STOCK_WORLD_MAP_OPEN_MUSIC_STOP = {
    (byte) 0x03, // iconst_0
    (byte) 0xb8,
    (byte) 0x02,
    (byte) 0x47, // invokestatic j.e:(I)V
    (byte) 0x02, // iconst_m1
    (byte) 0x02, // iconst_m1
    (byte) 0xb8,
    (byte) 0x01,
    (byte) 0xa3, // invokestatic j.a:(II)V
    (byte) 0x2a, // aload_0
    (byte) 0x10,
    (byte) 0x0f, // bipush 15
    (byte) 0xb5,
    (byte) 0x01,
    (byte) 0x43 // putfield j.s:I
  };

  private static final byte[] PATCHED_WORLD_MAP_OPEN_MUSIC_CONTINUES = {
    (byte) 0x00, // nop
    (byte) 0x00, // nop
    (byte) 0x00, // nop
    (byte) 0x00, // nop
    (byte) 0x02, // iconst_m1
    (byte) 0x02, // iconst_m1
    (byte) 0xb8,
    (byte) 0x01,
    (byte) 0xa3, // invokestatic j.a:(II)V
    (byte) 0x2a, // aload_0
    (byte) 0x10,
    (byte) 0x0f, // bipush 15
    (byte) 0xb5,
    (byte) 0x01,
    (byte) 0x43 // putfield j.s:I
  };

  private WorldMapMusicClassPatcher() {}

  public static PatcherState state(byte[] classBytes) {
    int original = count(classBytes, STOCK_WORLD_MAP_OPEN_MUSIC_STOP);
    int patched = count(classBytes, PATCHED_WORLD_MAP_OPEN_MUSIC_CONTINUES);

    if (original == 1 && patched == 0) {
      return PatcherState.ORIGINAL;
    }

    if (patched == 1 && original == 0) {
      return PatcherState.PATCHED;
    }

    log.info(
        "World-map music patch state unknown; originalSites={}, patchedSites={}",
        original,
        patched);

    return PatcherState.UNKNOWN;
  }

  public static byte[] apply(byte[] classBytes) {
    PatcherState state = state(classBytes);

    log.info("Applying world-map music patch; current state={}", state);

    if (state == PatcherState.PATCHED) {
      return classBytes.clone();
    }

    if (state != PatcherState.ORIGINAL) {
      throw new IllegalStateException("Unsupported i.class layout for world-map music patch.");
    }

    byte[] patched = classBytes.clone();

    int offset = indexOf(patched, STOCK_WORLD_MAP_OPEN_MUSIC_STOP, 0);

    System.arraycopy(
        PATCHED_WORLD_MAP_OPEN_MUSIC_CONTINUES,
        0,
        patched,
        offset,
        PATCHED_WORLD_MAP_OPEN_MUSIC_CONTINUES.length);
    if (state(patched) != PatcherState.PATCHED) {
      throw new IllegalStateException(
          "World-map music patch did not produce the expected bytecode.");
    }

    log.info("World-map music patch applied");

    return patched;
  }
}
