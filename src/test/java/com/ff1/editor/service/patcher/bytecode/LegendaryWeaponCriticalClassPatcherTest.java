package com.ff1.editor.service.patcher.bytecode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LegendaryWeaponCriticalClassPatcherTest {

  private static final Path STOCK_G_CLASS = Path.of("ff1-jar", "g.class");

  @Test
  void stockGClassDetectsOriginal() throws IOException {
    assertEquals(PatcherState.ORIGINAL, LegendaryWeaponCriticalClassPatcher.state(stockGClass()));
  }

  @Test
  void patchedGClassDetectsPatched() throws IOException {
    byte[] patched = LegendaryWeaponCriticalClassPatcher.apply(stockGClass());

    assertEquals(PatcherState.PATCHED, LegendaryWeaponCriticalClassPatcher.state(patched));
  }

  @Test
  void applyIsIdempotent() throws IOException {
    byte[] patched = LegendaryWeaponCriticalClassPatcher.apply(stockGClass());

    assertArrayEquals(patched, LegendaryWeaponCriticalClassPatcher.apply(patched));
  }

  @Test
  void removeIsIdempotentOnOriginalClass() throws IOException {
    byte[] original = stockGClass();

    assertArrayEquals(original, LegendaryWeaponCriticalClassPatcher.remove(original));
  }

  @Test
  void removeRestoresOriginalPatchState() throws IOException {
    byte[] patched = LegendaryWeaponCriticalClassPatcher.apply(stockGClass());
    byte[] restored = LegendaryWeaponCriticalClassPatcher.remove(patched);

    assertEquals(PatcherState.ORIGINAL, LegendaryWeaponCriticalClassPatcher.state(restored));
  }

  @Test
  void applyChangesStockClassBytes() throws IOException {
    byte[] original = stockGClass();
    byte[] patched = LegendaryWeaponCriticalClassPatcher.apply(original);

    assertNotEquals(0, original.length);
    assertNotEquals(java.util.Arrays.toString(original), java.util.Arrays.toString(patched));
  }

  @Test
  void canApplyAndRemoveAfterEnemyCriticalDefensePatch() throws IOException {
    byte[] criticalPatched = EnemyCriticalDefenseClassPatcher.apply(stockGClass());

    assertEquals(PatcherState.ORIGINAL, LegendaryWeaponCriticalClassPatcher.state(criticalPatched));
    byte[] legendaryPatched = LegendaryWeaponCriticalClassPatcher.apply(criticalPatched);
    assertEquals(PatcherState.PATCHED, LegendaryWeaponCriticalClassPatcher.state(legendaryPatched));
    byte[] restored = LegendaryWeaponCriticalClassPatcher.remove(legendaryPatched);
    assertEquals(PatcherState.ORIGINAL, LegendaryWeaponCriticalClassPatcher.state(restored));
    assertEquals(PatcherState.PATCHED, EnemyCriticalDefenseClassPatcher.state(restored));
  }

  @Test
  void canApplyAndRemoveAfterWeaponAffinityDamagePatch() throws IOException {
    byte[] affinityPatched = WeaponAffinityDamageClassPatcher.apply(stockGClass());

    assertEquals(PatcherState.ORIGINAL, LegendaryWeaponCriticalClassPatcher.state(affinityPatched));
    byte[] legendaryPatched = LegendaryWeaponCriticalClassPatcher.apply(affinityPatched);
    assertEquals(PatcherState.PATCHED, LegendaryWeaponCriticalClassPatcher.state(legendaryPatched));
    byte[] restored = LegendaryWeaponCriticalClassPatcher.remove(legendaryPatched);
    assertEquals(PatcherState.ORIGINAL, LegendaryWeaponCriticalClassPatcher.state(restored));
    assertEquals(PatcherState.PATCHED, WeaponAffinityDamageClassPatcher.state(restored));
  }

  @Test
  void unsupportedLayoutIsRejected() {
    byte[] invalidClass = new byte[] {0, 1, 2, 3};

    assertEquals(PatcherState.UNKNOWN, LegendaryWeaponCriticalClassPatcher.state(invalidClass));
    assertThrows(
        IllegalStateException.class, () -> LegendaryWeaponCriticalClassPatcher.apply(invalidClass));
    assertThrows(
        IllegalStateException.class,
        () -> LegendaryWeaponCriticalClassPatcher.remove(invalidClass));
  }

  private static byte[] stockGClass() throws IOException {
    return Files.readAllBytes(STOCK_G_CLASS);
  }
}
