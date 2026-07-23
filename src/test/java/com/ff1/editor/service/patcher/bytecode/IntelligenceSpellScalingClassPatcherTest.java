package com.ff1.editor.service.patcher.bytecode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IntelligenceSpellScalingClassPatcherTest {

  private static final Path STOCK_G_CLASS = Path.of("ff1-jar", "g.class");

  @Test
  void stockGClassDetectsOriginalForBothPatches() throws IOException {
    byte[] original = stockGClass();

    assertEquals(PatcherState.ORIGINAL, IntelligenceSpellDamageClassPatcher.state(original));
    assertEquals(PatcherState.ORIGINAL, IntelligenceSpellHealingClassPatcher.state(original));
  }

  @Test
  void damagePatchedGClassStillDetectsOriginalForHealingPatch() throws IOException {
    byte[] damagePatched = IntelligenceSpellDamageClassPatcher.apply(stockGClass());

    assertEquals(PatcherState.PATCHED, IntelligenceSpellDamageClassPatcher.state(damagePatched));
    assertEquals(PatcherState.ORIGINAL, IntelligenceSpellHealingClassPatcher.state(damagePatched));
  }

  @Test
  void healingPatchedGClassStillDetectsOriginalForDamagePatch() throws IOException {
    byte[] healingPatched = IntelligenceSpellHealingClassPatcher.apply(stockGClass());

    assertEquals(PatcherState.PATCHED, IntelligenceSpellHealingClassPatcher.state(healingPatched));
    assertEquals(PatcherState.ORIGINAL, IntelligenceSpellDamageClassPatcher.state(healingPatched));
  }

  @Test
  void bothPatchesCanApplyInUiOrder() throws IOException {
    byte[] patched =
        IntelligenceSpellDamageClassPatcher.apply(
            IntelligenceSpellHealingClassPatcher.apply(stockGClass()));

    assertEquals(PatcherState.PATCHED, IntelligenceSpellDamageClassPatcher.state(patched));
    assertEquals(PatcherState.PATCHED, IntelligenceSpellHealingClassPatcher.state(patched));
  }

  @Test
  void bothPatchesCanApplyInReverseOrder() throws IOException {
    byte[] patched =
        IntelligenceSpellHealingClassPatcher.apply(
            IntelligenceSpellDamageClassPatcher.apply(stockGClass()));

    assertEquals(PatcherState.PATCHED, IntelligenceSpellDamageClassPatcher.state(patched));
    assertEquals(PatcherState.PATCHED, IntelligenceSpellHealingClassPatcher.state(patched));
  }

  @Test
  void applyIsIdempotentForBothPatches() throws IOException {
    byte[] damagePatched = IntelligenceSpellDamageClassPatcher.apply(stockGClass());
    byte[] healingPatched = IntelligenceSpellHealingClassPatcher.apply(stockGClass());

    assertArrayEquals(damagePatched, IntelligenceSpellDamageClassPatcher.apply(damagePatched));
    assertArrayEquals(healingPatched, IntelligenceSpellHealingClassPatcher.apply(healingPatched));
  }

  @Test
  void applyChangesStockClassBytesForBothPatches() throws IOException {
    byte[] original = stockGClass();
    byte[] damagePatched = IntelligenceSpellDamageClassPatcher.apply(original);
    byte[] healingPatched = IntelligenceSpellHealingClassPatcher.apply(original);

    assertNotEquals(0, original.length);
    assertNotEquals(java.util.Arrays.toString(original), java.util.Arrays.toString(damagePatched));
    assertNotEquals(java.util.Arrays.toString(original), java.util.Arrays.toString(healingPatched));
  }

  @Test
  void unsupportedLayoutIsRejectedByBothPatches() {
    byte[] invalidClass = new byte[] {0, 1, 2, 3};

    assertEquals(PatcherState.UNKNOWN, IntelligenceSpellDamageClassPatcher.state(invalidClass));
    assertEquals(PatcherState.UNKNOWN, IntelligenceSpellHealingClassPatcher.state(invalidClass));
    assertThrows(
        IllegalStateException.class, () -> IntelligenceSpellDamageClassPatcher.apply(invalidClass));
    assertThrows(
        IllegalStateException.class,
        () -> IntelligenceSpellHealingClassPatcher.apply(invalidClass));
  }

  private static byte[] stockGClass() throws IOException {
    return Files.readAllBytes(STOCK_G_CLASS);
  }
}
