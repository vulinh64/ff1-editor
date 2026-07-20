package com.ff1.editor.service.patcher;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class WeaponAffinityDamageClassPatcherTest {

  private static final Path STOCK_G_CLASS = Path.of("ff1-jar", "g.class");

  @Test
  void stockGClassDetectsOriginal() throws IOException {
    assertEquals(
        WeaponAffinityDamageClassPatcher.State.ORIGINAL,
        WeaponAffinityDamageClassPatcher.state(stockGClass()));
  }

  @Test
  void patchedGClassDetectsPatched() throws IOException {
    byte[] patched = WeaponAffinityDamageClassPatcher.apply(stockGClass());

    assertEquals(
        WeaponAffinityDamageClassPatcher.State.PATCHED,
        WeaponAffinityDamageClassPatcher.state(patched));
  }

  @Test
  void applyIsIdempotent() throws IOException {
    byte[] patched = WeaponAffinityDamageClassPatcher.apply(stockGClass());

    assertArrayEquals(patched, WeaponAffinityDamageClassPatcher.apply(patched));
  }

  @Test
  void applyChangesStockClassBytes() throws IOException {
    byte[] original = stockGClass();
    byte[] patched = WeaponAffinityDamageClassPatcher.apply(original);

    assertNotEquals(0, original.length);
    assertNotEquals(java.util.Arrays.toString(original), java.util.Arrays.toString(patched));
  }

  @Test
  void canApplyAfterEnemyCriticalDefensePatch() throws IOException {
    byte[] criticalPatched = EnemyCriticalDefenseClassPatcher.apply(stockGClass());

    assertEquals(
        WeaponAffinityDamageClassPatcher.State.ORIGINAL,
        WeaponAffinityDamageClassPatcher.state(criticalPatched));
    assertEquals(
        WeaponAffinityDamageClassPatcher.State.PATCHED,
        WeaponAffinityDamageClassPatcher.state(
            WeaponAffinityDamageClassPatcher.apply(criticalPatched)));
  }

  @Test
  void enemyCriticalDefenseCanApplyAfterWeaponAffinityDamagePatch() throws IOException {
    byte[] affinityPatched = WeaponAffinityDamageClassPatcher.apply(stockGClass());

    assertEquals(
        EnemyCriticalDefenseClassPatcher.State.ORIGINAL,
        EnemyCriticalDefenseClassPatcher.state(affinityPatched));
    assertEquals(
        EnemyCriticalDefenseClassPatcher.State.PATCHED,
        EnemyCriticalDefenseClassPatcher.state(
            EnemyCriticalDefenseClassPatcher.apply(affinityPatched)));
  }

  @Test
  void unsupportedLayoutIsRejected() {
    byte[] invalidClass = new byte[] {0, 1, 2, 3};

    assertEquals(
        WeaponAffinityDamageClassPatcher.State.UNKNOWN,
        WeaponAffinityDamageClassPatcher.state(invalidClass));
    assertThrows(
        IllegalStateException.class, () -> WeaponAffinityDamageClassPatcher.apply(invalidClass));
  }

  private static byte[] stockGClass() throws IOException {
    return Files.readAllBytes(STOCK_G_CLASS);
  }
}
