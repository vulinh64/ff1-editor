package com.ff1.editor.service.patcher;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.InvokeInstruction;
import java.util.ArrayList;
import java.util.List;

import com.ff1.editor.utils.CldcStackMapStripper;
import lombok.extern.slf4j.Slf4j;

/** Patches g.class so normal Run commands always escape while preserving no-run encounter gates. */
@Slf4j
public final class AlwaysSuccessfulRunClassPatcher {

  public static final String ENTRY_NAME = HeroLevelGrowthClassPatcher.ENTRY_NAME;

  private static final String RUN_CHECK_METHOD = "c";
  private static final String RUN_CHECK_DESCRIPTOR = "(I)Z";
  private static final String RANDOM_CLASS_NAME = "java/util/Random";
  private static final String RANDOM_NEXT_INT = "nextInt";
  private static final String RANDOM_NEXT_INT_DESCRIPTOR = "()I";
  private static final int ORIGINAL_RANDOM_CALLS = 2;
  private static final int ORIGINAL_RETURN_COUNT = 4;
  private static final int PATCHED_RETURN_COUNT = 3;

  public enum State {
    ORIGINAL,
    PATCHED,
    UNKNOWN
  }

  private AlwaysSuccessfulRunClassPatcher() {}

  public static State state(byte[] classBytes) {
    try {
      ClassModel model = ClassFile.of().parse(classBytes);
      List<Instruction> instructions = runCheckInstructions(model);
      if (instructions.isEmpty()) {
        log.info("Always-run class patch state unknown; run check method not found");
        return State.UNKNOWN;
      }
      int randomCalls = randomNextIntCalls(instructions);
      int returns = returns(instructions);
      if (randomCalls == ORIGINAL_RANDOM_CALLS && returns == ORIGINAL_RETURN_COUNT) {
        return State.ORIGINAL;
      }
      if (randomCalls == 0 && returns == PATCHED_RETURN_COUNT) {
        return State.PATCHED;
      }
      log.info(
          "Always-run class patch state unknown; randomCalls={}, returns={}", randomCalls, returns);
      return State.UNKNOWN;
    } catch (RuntimeException | LinkageError _) {
      return State.UNKNOWN;
    }
  }

  public static byte[] apply(byte[] classBytes) {
    State state = state(classBytes);
    log.info("Applying always-successful run class patch; current state={}", state);
    if (state == State.PATCHED) {
      return classBytes.clone();
    }
    if (state != State.ORIGINAL) {
      throw new IllegalStateException(
          "Unsupported g.class layout for always-successful run patch.");
    }

    ClassFile classFile = ClassFile.of();
    ClassModel model = classFile.parse(classBytes);
    PatchCounter counter = new PatchCounter();
    byte[] patched =
        classFile.transformClass(
            model,
            java.lang.classfile.ClassTransform.transformingMethodBodies(
                AlwaysSuccessfulRunClassPatcher::isRunCheckMethod,
                java.lang.classfile.CodeTransform.ofStateful(
                    () -> new AlwaysSuccessfulRunCodeTransform(counter))));
    patched =
        CldcStackMapStripper.stripMethodStackMap(patched, RUN_CHECK_METHOD, RUN_CHECK_DESCRIPTOR);
    State patchedState = state(patched);
    if (counter.count() != 1 || patchedState != State.PATCHED) {
      throw new IllegalStateException(
          "Expected one run-check tail in %s but patched %d; state=%s."
              .formatted(ENTRY_NAME, counter.count(), patchedState));
    }
    log.info("Always-successful run class patch applied");
    return patched;
  }

  private static List<Instruction> runCheckInstructions(ClassModel model) {
    for (MethodModel method : model.methods()) {
      if (isRunCheckMethod(method)) {
        return instructions(method);
      }
    }
    return List.of();
  }

  private static boolean isRunCheckMethod(MethodModel method) {
    return RUN_CHECK_METHOD.equals(method.methodName().stringValue())
        && RUN_CHECK_DESCRIPTOR.equals(method.methodType().stringValue());
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

  private static int randomNextIntCalls(List<Instruction> instructions) {
    int matches = 0;
    for (Instruction instruction : instructions) {
      if (isRandomNextInt(instruction)) {
        matches++;
      }
    }
    return matches;
  }

  private static int returns(List<Instruction> instructions) {
    int matches = 0;
    for (Instruction instruction : instructions) {
      if (instruction.opcode() == Opcode.IRETURN) {
        matches++;
      }
    }
    return matches;
  }

  private static boolean isRandomNextInt(Instruction instruction) {
    return instruction instanceof InvokeInstruction invoke
        && invoke.opcode() == Opcode.INVOKEVIRTUAL
        && RANDOM_CLASS_NAME.equals(invoke.owner().asInternalName())
        && RANDOM_NEXT_INT.equals(invoke.name().stringValue())
        && RANDOM_NEXT_INT_DESCRIPTOR.equals(invoke.type().stringValue());
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

  private static final class AlwaysSuccessfulRunCodeTransform
      implements java.lang.classfile.CodeTransform {
    private final PatchCounter counter;
    private int returns;
    private boolean emitPatchedTail;
    private boolean tailPatched;

    private AlwaysSuccessfulRunCodeTransform(PatchCounter counter) {
      this.counter = counter;
    }

    @Override
    public void accept(CodeBuilder builder, CodeElement element) {
      if (tailPatched) {
        return;
      }
      if (emitPatchedTail) {
        if (element instanceof Instruction) {
          builder.iconst_1().ireturn();
          tailPatched = true;
          counter.increment();
          return;
        }
        builder.with(element);
        return;
      }

      builder.with(element);
      if (element instanceof Instruction instruction && instruction.opcode() == Opcode.IRETURN) {
        returns++;
        if (returns == 2) {
          emitPatchedTail = true;
        }
      }
    }
  }
}
