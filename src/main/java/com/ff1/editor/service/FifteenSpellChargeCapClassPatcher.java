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
public final class FifteenSpellChargeCapClassPatcher {

  public static final String ENTRY_NAME = "g.class";

  private static final int EXPECTED_CAP_CONSTANT_SITES = 2;
  private static final int ORIGINAL_MAX_CHARGES = 9;
  private static final int PATCHED_MAX_CHARGES = FifteenSpellChargeGrowthPatcher.MAX_CHARGES;
  private static final String LEVEL_UP_METHOD = "F";
  private static final String LEVEL_UP_DESCRIPTOR = "()V";

  public enum State {
    ORIGINAL,
    PATCHED,
    UNKNOWN
  }

  private FifteenSpellChargeCapClassPatcher() {}

  public static State state(byte[] classBytes) {
    try {
      ClassModel model = ClassFile.of().parse(classBytes);
      int original = countCapConstants(model, ORIGINAL_MAX_CHARGES);
      int patched = countCapConstants(model, PATCHED_MAX_CHARGES);
      if (original == EXPECTED_CAP_CONSTANT_SITES && patched == 0) {
        return State.ORIGINAL;
      }
      if (patched == EXPECTED_CAP_CONSTANT_SITES && original == 0) {
        return State.PATCHED;
      }
      log.info(
          "15 spell-charge cap patch state unknown; originalSites={}, patchedSites={}",
          original,
          patched);
      return State.UNKNOWN;
    } catch (RuntimeException | LinkageError _) {
      return State.UNKNOWN;
    }
  }

  public static byte[] apply(byte[] classBytes) {
    State state = state(classBytes);
    log.info("Applying 15 spell-charge cap patch; current state={}", state);
    if (state == State.PATCHED) {
      return classBytes.clone();
    }
    if (state != State.ORIGINAL) {
      throw new IllegalStateException("Unsupported g.class layout for 15 spell-charge cap patch.");
    }

    ClassFile classFile = ClassFile.of();
    ClassModel model = classFile.parse(classBytes);
    PatchCounter counter = new PatchCounter();
    byte[] patched =
        classFile.transformClass(
            model,
            java.lang.classfile.ClassTransform.transformingMethodBodies(
                FifteenSpellChargeCapClassPatcher::isLevelUpMethod,
                java.lang.classfile.CodeTransform.ofStateful(
                    () -> new FifteenChargeCapCodeTransform(counter))));
    State patchedState = state(patched);
    if (counter.count() != EXPECTED_CAP_CONSTANT_SITES || patchedState != State.PATCHED) {
      throw new IllegalStateException(
          "Expected %d spell-charge cap constants in %s but patched %d; state=%s."
              .formatted(EXPECTED_CAP_CONSTANT_SITES, ENTRY_NAME, counter.count(), patchedState));
    }
    log.info("15 spell-charge cap class patch applied at {} sites", counter.count());
    return patched;
  }

  private static int countCapConstants(ClassModel model, int value) {
    int matches = 0;
    for (MethodModel method : model.methods()) {
      if (!isLevelUpMethod(method)) {
        continue;
      }
      List<Instruction> instructions = instructions(method);
      for (int i = 0; i < instructions.size(); i++) {
        if (isCapConstantSite(instructions, i, value)) {
          matches++;
        }
      }
    }
    return matches;
  }

  private static boolean isLevelUpMethod(MethodModel method) {
    return LEVEL_UP_METHOD.equals(method.methodName().stringValue())
        && LEVEL_UP_DESCRIPTOR.equals(method.methodType().stringValue());
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

  private static boolean isCapConstantSite(List<Instruction> instructions, int offset, int value) {
    return isPush(instructions.get(offset), value)
        && offset + 1 < instructions.size()
        && (instructions.get(offset + 1).opcode() == Opcode.IF_ICMPLE
            || instructions.get(offset + 1).opcode() == Opcode.BASTORE);
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

  private static final class FifteenChargeCapCodeTransform
      implements java.lang.classfile.CodeTransform {
    private final PatchCounter counter;
    private Instruction pendingInstruction;

    private FifteenChargeCapCodeTransform(PatchCounter counter) {
      this.counter = counter;
    }

    @Override
    public void accept(CodeBuilder builder, CodeElement element) {
      if (pendingInstruction != null) {
        if (element instanceof Instruction instruction
            && (instruction.opcode() == Opcode.IF_ICMPLE
                || instruction.opcode() == Opcode.BASTORE)) {
          builder.bipush(PATCHED_MAX_CHARGES);
          builder.with(element);
          counter.increment();
          pendingInstruction = null;
          return;
        }
        builder.with(pendingInstruction);
        pendingInstruction = null;
      }

      if (element instanceof Instruction instruction && isPush(instruction, ORIGINAL_MAX_CHARGES)) {
        pendingInstruction = instruction;
        return;
      }

      builder.with(element);
    }
  }
}
