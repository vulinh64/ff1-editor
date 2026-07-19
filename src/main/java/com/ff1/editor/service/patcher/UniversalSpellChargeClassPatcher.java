package com.ff1.editor.service.patcher;

import com.ff1.editor.service.*;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** Patches g.class so all classes can receive spell-charge growth instead of mage classes only. */
@Slf4j
public final class UniversalSpellChargeClassPatcher {

  public static final String ENTRY_NAME = "g.class";

  private static final int EXPECTED_CLASS_GATE_SITES = 1;
  private static final int ORIGINAL_CLASS_GATE = 3;
  private static final int PATCHED_CLASS_GATE = 0;
  private static final String LEVEL_UP_METHOD = "F";
  private static final String LEVEL_UP_DESCRIPTOR = "()V";
  private static final String HERO_CLASS_NAME = "a";
  private static final String HERO_CLASS_ID_FIELD = "a";
  private static final String HERO_CLASS_ID_DESCRIPTOR = "S";

  public enum State {
    ORIGINAL,
    PATCHED,
    UNKNOWN
  }

  private UniversalSpellChargeClassPatcher() {}

  public static State state(byte[] classBytes) {
    try {
      ClassModel model = ClassFile.of().parse(classBytes);
      int original = countClassGateSites(model, ORIGINAL_CLASS_GATE);
      int patched = countClassGateSites(model, PATCHED_CLASS_GATE);
      if (original == EXPECTED_CLASS_GATE_SITES && patched == 0) {
        return State.ORIGINAL;
      }
      if (patched == EXPECTED_CLASS_GATE_SITES && original == 0) {
        return State.PATCHED;
      }
      log.info(
          "Universal spell-charge class patch state unknown; originalSites={}, patchedSites={}",
          original,
          patched);
      return State.UNKNOWN;
    } catch (RuntimeException | LinkageError _) {
      return State.UNKNOWN;
    }
  }

  public static byte[] apply(byte[] classBytes) {
    State state = state(classBytes);
    log.info("Applying universal spell-charge class patch; current state={}", state);
    if (state == State.PATCHED) {
      return classBytes.clone();
    }
    if (state != State.ORIGINAL) {
      throw new IllegalStateException(
          "Unsupported g.class layout for universal spell-charge patch.");
    }

    ClassFile classFile = ClassFile.of();
    ClassModel model = classFile.parse(classBytes);
    PatchCounter counter = new PatchCounter();
    byte[] patched =
        classFile.transformClass(
            model,
            java.lang.classfile.ClassTransform.transformingMethodBodies(
                UniversalSpellChargeClassPatcher::isLevelUpMethod,
                java.lang.classfile.CodeTransform.ofStateful(
                    () -> new UniversalSpellChargeCodeTransform(counter))));
    State patchedState = state(patched);
    if (counter.count() != EXPECTED_CLASS_GATE_SITES || patchedState != State.PATCHED) {
      throw new IllegalStateException(
          "Expected %d spell-charge class gate in %s but patched %d; state=%s."
              .formatted(EXPECTED_CLASS_GATE_SITES, ENTRY_NAME, counter.count(), patchedState));
    }
    log.info("Universal spell-charge class patch applied at {} site", counter.count());
    return patched;
  }

  private static int countClassGateSites(ClassModel model, int gate) {
    int matches = 0;
    for (MethodModel method : model.methods()) {
      if (!isLevelUpMethod(method)) {
        continue;
      }
      List<Instruction> instructions = instructions(method);
      for (int i = 0; i < instructions.size(); i++) {
        if (isClassGateSite(instructions, i, gate)) {
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

  private static boolean isClassGateSite(List<Instruction> instructions, int offset, int gate) {
    return offset >= 1
        && offset + 1 < instructions.size()
        && isHeroClassIdRead(instructions.get(offset - 1))
        && isPush(instructions.get(offset), gate)
        && instructions.get(offset + 1).opcode() == Opcode.IF_ICMPLT;
  }

  private static boolean isHeroClassIdRead(Instruction instruction) {
    return instruction instanceof FieldInstruction field
        && field.opcode() == Opcode.GETFIELD
        && HERO_CLASS_NAME.equals(field.owner().asInternalName())
        && HERO_CLASS_ID_FIELD.equals(field.name().stringValue())
        && HERO_CLASS_ID_DESCRIPTOR.equals(field.type().stringValue());
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

  private static final class UniversalSpellChargeCodeTransform
      implements java.lang.classfile.CodeTransform {
    private final PatchCounter counter;
    private Instruction previousInstruction;

    private UniversalSpellChargeCodeTransform(PatchCounter counter) {
      this.counter = counter;
    }

    @Override
    public void accept(CodeBuilder builder, CodeElement element) {
      if (element instanceof Instruction instruction
          && isHeroClassIdRead(previousInstruction)
          && isPush(instruction, ORIGINAL_CLASS_GATE)) {
        builder.iconst_0();
        counter.increment();
        previousInstruction = null;
        return;
      }

      builder.with(element);
      previousInstruction = element instanceof Instruction instruction ? instruction : null;
    }
  }
}
