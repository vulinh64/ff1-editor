package com.ff1.editor.service.patcher.bytecode;

import com.ff1.editor.service.*;
import com.ff1.editor.service.patcher.data.FifteenSpellChargeGrowthPatcher;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.ConstantInstruction;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** Patches i.class so inns and cottages refill up to the fifteen-charge spell cap. */
@Slf4j
public final class FifteenSpellChargeRecoveryClassPatcher {

  public static final String ENTRY_NAME = "i.class";

  private static final int EXPECTED_RECOVERY_CONSTANT_SITES = 1;
  private static final int ORIGINAL_RECOVERY_AMOUNT = 10;
  private static final int PATCHED_RECOVERY_AMOUNT = FifteenSpellChargeGrowthPatcher.MAX_CHARGES;
  private static final String RECOVERY_METHOD = "l";
  private static final String RECOVERY_DESCRIPTOR = "(I)V";

  private FifteenSpellChargeRecoveryClassPatcher() {}

  public static PatcherState state(byte[] classBytes) {
    try {
      ClassModel model = ClassFile.of().parse(classBytes);
      int original = countRecoveryConstants(model, ORIGINAL_RECOVERY_AMOUNT);
      int patched = countRecoveryConstants(model, PATCHED_RECOVERY_AMOUNT);
      if (original == EXPECTED_RECOVERY_CONSTANT_SITES && patched == 0) {
        return PatcherState.ORIGINAL;
      }
      if (patched == EXPECTED_RECOVERY_CONSTANT_SITES && original == 0) {
        return PatcherState.PATCHED;
      }
      log.info(
          "15 spell-charge recovery patch state unknown; originalSites={}, patchedSites={}",
          original,
          patched);
      return PatcherState.UNKNOWN;
    } catch (RuntimeException | LinkageError _) {
      return PatcherState.UNKNOWN;
    }
  }

  public static byte[] apply(byte[] classBytes) {
    PatcherState state = state(classBytes);
    log.info("Applying 15 spell-charge recovery patch; current state={}", state);
    if (state == PatcherState.PATCHED) {
      return classBytes.clone();
    }
    if (state != PatcherState.ORIGINAL) {
      throw new IllegalStateException(
          "Unsupported i.class layout for 15 spell-charge recovery patch.");
    }

    ClassFile classFile = ClassFile.of();
    ClassModel model = classFile.parse(classBytes);
    PatchCounter counter = new PatchCounter();
    byte[] patched =
        classFile.transformClass(
            model,
            java.lang.classfile.ClassTransform.transformingMethodBodies(
                FifteenSpellChargeRecoveryClassPatcher::isRecoveryMethod,
                java.lang.classfile.CodeTransform.ofStateful(
                    () -> new FifteenChargeRecoveryCodeTransform(counter))));
    PatcherState patchedState = state(patched);
    if (counter.count() != EXPECTED_RECOVERY_CONSTANT_SITES
        || patchedState != PatcherState.PATCHED) {
      throw new IllegalStateException(
          "Expected %d spell-charge recovery constant in %s but patched %d; state=%s."
              .formatted(
                  EXPECTED_RECOVERY_CONSTANT_SITES, ENTRY_NAME, counter.count(), patchedState));
    }
    log.info("15 spell-charge recovery class patch applied at {} site", counter.count());
    return patched;
  }

  private static int countRecoveryConstants(ClassModel model, int value) {
    int matches = 0;
    for (MethodModel method : model.methods()) {
      if (!isRecoveryMethod(method)) {
        continue;
      }
      List<Instruction> instructions = instructions(method);
      for (int i = 0; i < instructions.size(); i++) {
        if (isRecoveryConstantSite(instructions, i, value)) {
          matches++;
        }
      }
    }
    return matches;
  }

  private static boolean isRecoveryMethod(MethodModel method) {
    return RECOVERY_METHOD.equals(method.methodName().stringValue())
        && RECOVERY_DESCRIPTOR.equals(method.methodType().stringValue());
  }

  private static List<Instruction> instructions(MethodModel method) {
    List<Instruction> instructions = new ArrayList<>();
    if (method.code().isEmpty()) {
      return instructions;
    }
    for (CodeElement element : method.code().orElseThrow()) {
      if (element instanceof Instruction instruction) {
        instructions.add(instruction);
      }
    }
    return instructions;
  }

  private static boolean isRecoveryConstantSite(
      List<Instruction> instructions, int offset, int value) {
    return offset + 1 < instructions.size()
        && isPush(instructions.get(offset), value)
        && instructions.get(offset + 1).opcode() == Opcode.ISTORE_2;
  }

  private static boolean isPush(Instruction instruction, int value) {
    return instruction instanceof ConstantInstruction constant
        && constant.constantValue() instanceof Integer integer
        && integer == value;
  }

  private static final class PatchCounter {
    private int count;

    void increment() {
      count++;
    }

    int count() {
      return count;
    }
  }

  private static final class FifteenChargeRecoveryCodeTransform
      implements java.lang.classfile.CodeTransform {
    private final PatchCounter counter;

    private FifteenChargeRecoveryCodeTransform(PatchCounter counter) {
      this.counter = counter;
    }

    @Override
    public void accept(CodeBuilder builder, CodeElement element) {
      if (element instanceof Instruction instruction
          && isPush(instruction, ORIGINAL_RECOVERY_AMOUNT)) {
        builder.bipush(PATCHED_RECOVERY_AMOUNT);
        counter.increment();
        return;
      }
      builder.with(element);
    }
  }
}
