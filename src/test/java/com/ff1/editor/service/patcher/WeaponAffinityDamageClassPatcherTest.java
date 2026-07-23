package com.ff1.editor.service.patcher;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.StoreInstruction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
  void patchedBonusReadsRuntimeWeaponDamageField() throws IOException {
    byte[] patched = WeaponAffinityDamageClassPatcher.apply(stockGClass());

    List<Instruction> instructions = physicalDamageInstructions(patched);
    assertEquals(1, weaponDamageBonusSites(instructions, 3));
    assertEquals(0, weaponDamageBonusSites(instructions, 1));
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

  private static List<Instruction> physicalDamageInstructions(byte[] classBytes) {
    var model = ClassFile.of().parse(classBytes);
    for (var method : model.methods()) {
      if ("a".equals(method.methodName().stringValue())
          && "(ZZII)V".equals(method.methodType().stringValue())) {
        List<Instruction> instructions = new ArrayList<>();
        for (CodeElement element : method.code().orElseThrow()) {
          if (element instanceof Instruction instruction) {
            instructions.add(instruction);
          }
        }
        return instructions;
      }
    }
    return List.of();
  }

  private static int weaponDamageBonusSites(List<Instruction> instructions, int runtimeField) {
    int matches = 0;
    for (int i = 0; i <= instructions.size() - 10; i++) {
      if (isLoad(instructions.get(i), 4)
          && isWeaponRecordsRead(instructions.get(i + 1))
          && isLoad(instructions.get(i + 2), 7)
          && instructions.get(i + 3).opcode() == Opcode.AALOAD
          && isPush(instructions.get(i + 4), runtimeField)
          && instructions.get(i + 5).opcode() == Opcode.SALOAD
          && isPush(instructions.get(i + 6), 2)
          && instructions.get(i + 7).opcode() == Opcode.IDIV
          && instructions.get(i + 8).opcode() == Opcode.IADD
          && isStore(instructions.get(i + 9))) {
        matches++;
      }
    }
    return matches;
  }

  private static boolean isWeaponRecordsRead(Instruction instruction) {
    return instruction instanceof FieldInstruction field
        && field.opcode() == Opcode.GETSTATIC
        && "j".equals(field.owner().asInternalName())
        && "c".equals(field.name().stringValue())
        && "[[S".equals(field.type().stringValue());
  }

  private static boolean isLoad(Instruction instruction, int slot) {
    return instruction instanceof LoadInstruction load
        && load.opcode().kind() == Opcode.Kind.LOAD
        && load.slot() == slot;
  }

  private static boolean isStore(Instruction instruction) {
    return instruction instanceof StoreInstruction store
        && store.opcode().kind() == Opcode.Kind.STORE
        && store.slot() == 4;
  }

  private static boolean isPush(Instruction instruction, int value) {
    return instruction instanceof ConstantInstruction constant
        && constant.constantValue() instanceof Integer integer
        && integer == value;
  }
}
