package com.ff1.editor.service.patcher.bytecode;

import com.ff1.editor.service.*;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** Patches g.class level-up rolls so every HP and body-stat growth check succeeds. */
@Slf4j
public final class HeroLevelGrowthClassPatcher {

  public static final String ENTRY_NAME = "g.class";

  private static final int EXPECTED_LEVEL_UP_RANDOM_ROLLS = 6;
  private static final int ORIGINAL_RANDOM_MODULUS = 8;
  private static final int PATCHED_RANDOM_MODULUS = 1;
  private static final String LEVEL_UP_METHOD = "F";
  private static final String LEVEL_UP_DESCRIPTOR = "()V";
  private static final String RANDOM_CLASS_NAME = "java/util/Random";
  private static final String RANDOM_NEXT_INT = "nextInt";
  private static final String RANDOM_NEXT_INT_DESCRIPTOR = "()I";

  private HeroLevelGrowthClassPatcher() {}

  public static PatcherState state(byte[] classBytes) {
    try {
      ClassModel model = ClassFile.of().parse(classBytes);
      int original = countRandomModulusSites(model, ORIGINAL_RANDOM_MODULUS);
      int patched = countRandomModulusSites(model, PATCHED_RANDOM_MODULUS);
      if (original == EXPECTED_LEVEL_UP_RANDOM_ROLLS && patched == 0) {
        return PatcherState.ORIGINAL;
      }
      if (patched == EXPECTED_LEVEL_UP_RANDOM_ROLLS && original == 0) {
        return PatcherState.PATCHED;
      }
      log.info(
          "Strong level-up class patch state unknown; originalSites={}, patchedSites={}",
          original,
          patched);
      return PatcherState.UNKNOWN;
    } catch (RuntimeException | LinkageError _) {
      return PatcherState.UNKNOWN;
    }
  }

  public static byte[] apply(byte[] classBytes) {
    PatcherState state = state(classBytes);
    log.info("Applying strong level-up class patch; current state={}", state);
    if (state == PatcherState.PATCHED) {
      return classBytes.clone();
    }
    if (state != PatcherState.ORIGINAL) {
      throw new IllegalStateException("Unsupported g.class layout for strong level-up patch.");
    }

    ClassFile classFile = ClassFile.of();
    ClassModel model = classFile.parse(classBytes);
    PatchCounter counter = new PatchCounter();
    byte[] patched =
        classFile.transformClass(
            model,
            java.lang.classfile.ClassTransform.transformingMethodBodies(
                HeroLevelGrowthClassPatcher::isLevelUpMethod,
                java.lang.classfile.CodeTransform.ofStateful(
                    () -> new StrongLevelUpCodeTransform(counter))));
    PatcherState patchedState = state(patched);
    if (counter.count() != EXPECTED_LEVEL_UP_RANDOM_ROLLS || patchedState != PatcherState.PATCHED) {
      throw new IllegalStateException(
          "Expected %d level-up random rolls in %s but patched %d; state=%s."
              .formatted(
                  EXPECTED_LEVEL_UP_RANDOM_ROLLS, ENTRY_NAME, counter.count(), patchedState));
    }
    log.info("Strong level-up class patch applied at {} sites", counter.count());
    return patched;
  }

  private static int countRandomModulusSites(ClassModel model, int modulus) {
    int matches = 0;
    for (MethodModel method : model.methods()) {
      if (!isLevelUpMethod(method)) {
        continue;
      }
      List<Instruction> instructions = instructions(method);
      for (int i = 0; i < instructions.size(); i++) {
        if (isRandomModulusSite(instructions, i, modulus)) {
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

  private static boolean isRandomModulusSite(
      List<Instruction> instructions, int offset, int modulus) {
    return offset >= 3
        && offset + 2 < instructions.size()
        && isRandomNextInt(instructions.get(offset - 3))
        && instructions.get(offset - 2).opcode() == Opcode.ICONST_1
        && instructions.get(offset - 1).opcode() == Opcode.IUSHR
        && isPush(instructions.get(offset), modulus)
        && instructions.get(offset + 1).opcode() == Opcode.IREM
        && instructions.get(offset + 2).opcode() == Opcode.IFNE;
  }

  private static boolean isRandomNextInt(Instruction instruction) {
    return instruction instanceof InvokeInstruction invoke
        && invoke.opcode() == Opcode.INVOKEVIRTUAL
        && RANDOM_CLASS_NAME.equals(invoke.owner().asInternalName())
        && RANDOM_NEXT_INT.equals(invoke.name().stringValue())
        && RANDOM_NEXT_INT_DESCRIPTOR.equals(invoke.type().stringValue());
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

  private static final class StrongLevelUpCodeTransform
      implements java.lang.classfile.CodeTransform {
    private final PatchCounter counter;
    private final List<Instruction> recentInstructions = new ArrayList<>();

    private StrongLevelUpCodeTransform(PatchCounter counter) {
      this.counter = counter;
    }

    @Override
    public void accept(CodeBuilder builder, CodeElement element) {
      if (element instanceof Instruction instruction
          && isPush(instruction, ORIGINAL_RANDOM_MODULUS)
          && hasRandomShiftPrefix()) {
        builder.iconst_1();
        counter.increment();
        recentInstructions.clear();
        return;
      }

      builder.with(element);
      if (element instanceof Instruction instruction) {
        remember(instruction);
      } else {
        recentInstructions.clear();
      }
    }

    private boolean hasRandomShiftPrefix() {
      return recentInstructions.size() == 3
          && isRandomNextInt(recentInstructions.get(0))
          && recentInstructions.get(1).opcode() == Opcode.ICONST_1
          && recentInstructions.get(2).opcode() == Opcode.IUSHR;
    }

    private void remember(Instruction instruction) {
      recentInstructions.add(instruction);
      if (recentInstructions.size() > 3) {
        recentInstructions.removeFirst();
      }
    }
  }
}
