package com.ff1.editor.service;

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

@Slf4j
public final class FifteenSpellChargeRecoveryClassPatcher {

  public static final String ENTRY_NAME = "i.class";

  private static final int EXPECTED_RECOVERY_CONSTANT_SITES = 1;
  private static final int ORIGINAL_RECOVERY_AMOUNT = 10;
  private static final int PATCHED_RECOVERY_AMOUNT = FifteenSpellChargeGrowthPatcher.MAX_CHARGES;
  private static final String RECOVERY_METHOD = "l";
  private static final String RECOVERY_DESCRIPTOR = "(I)V";

  public enum State {
    ORIGINAL,
    PATCHED,
    UNKNOWN
  }

  private FifteenSpellChargeRecoveryClassPatcher() {}

  public static State state(byte[] classBytes) {
    try {
      ClassModel model = ClassFile.of().parse(classBytes);
      int original = countRecoveryConstants(model, ORIGINAL_RECOVERY_AMOUNT);
      int patched = countRecoveryConstants(model, PATCHED_RECOVERY_AMOUNT);
      if (original == EXPECTED_RECOVERY_CONSTANT_SITES && patched == 0) {
        return State.ORIGINAL;
      }
      if (patched == EXPECTED_RECOVERY_CONSTANT_SITES && original == 0) {
        return State.PATCHED;
      }
      log.info(
          "15 spell-charge recovery patch state unknown; originalSites={}, patchedSites={}",
          original,
          patched);
      return State.UNKNOWN;
    } catch (RuntimeException | LinkageError _) {
      return State.UNKNOWN;
    }
  }

  public static byte[] apply(byte[] classBytes) {
    State state = state(classBytes);
    log.info("Applying 15 spell-charge recovery patch; current state={}", state);
    if (state == State.PATCHED) {
      return classBytes.clone();
    }
    if (state != State.ORIGINAL) {
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
    State patchedState = state(patched);
    if (counter.count() != EXPECTED_RECOVERY_CONSTANT_SITES || patchedState != State.PATCHED) {
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
