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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Patches i.class so the airship can land on additional safe land terrain while preserving blocked
 * and water-like terrain checks.
 */
@Slf4j
public final class AirshipLandingClassPatcher {

  public static final String ENTRY_NAME = FifteenSpellChargeRecoveryClassPatcher.ENTRY_NAME;

  private static final String LANDING_METHOD = "L";
  private static final String LANDING_DESCRIPTOR = "()V";
  private static final int STOCK_LANDING_UPPER_BOUND = 14;
  private static final int PATCHED_LANDING_UPPER_BOUND = 33;
  private static final int SAFE_LANDING_LOWER_BOUND = 10;

  // Earlier local draft patch. Keep only so experimental jars can be upgraded
  // to the final Class-File API patch shape.
  private static final byte[] PERMISSIVE_LANDING_CHECK = {
    (byte) 0x59, // dup
    (byte) 0x3c, // istore_1
    (byte) 0x10, // bipush 55
    (byte) 0x37,
    (byte) 0x1b, // iload_1
    (byte) 0xa3, // if_icmpgt success; tile < 55
    (byte) 0x00,
    (byte) 0x0c,
    (byte) 0x1b, // iload_1
    (byte) 0x10, // bipush 65
    (byte) 0x41,
    (byte) 0xa3, // if_icmpgt success; tile > 65
    (byte) 0x00,
    (byte) 0x06,
    (byte) 0xa7, // goto fail; sea/river 55..65
    (byte) 0x00,
    (byte) 0x3d
  };

  private static final byte[] WALKABLE_THRESHOLD_LANDING_CHECK = {
    (byte) 0x59, // dup
    (byte) 0x3c, // istore_1
    (byte) 0x10, // bipush 55
    (byte) 0x37,
    (byte) 0x1b, // iload_1
    (byte) 0xa3, // if_icmpgt success; tile < 55
    (byte) 0x00,
    (byte) 0x0c,
    (byte) 0xa7, // goto fail; tile >= 55
    (byte) 0x00,
    (byte) 0x43,
    (byte) 0x00, // nop padding
    (byte) 0x00,
    (byte) 0x00,
    (byte) 0x00,
    (byte) 0x00,
    (byte) 0x00
  };

  private AirshipLandingClassPatcher() {}

  public static PatcherState state(byte[] classBytes) {
    try {
      ClassModel model = ClassFile.of().parse(classBytes);
      int original = countLandingUpperBoundSites(model, STOCK_LANDING_UPPER_BOUND);
      int patched = countLandingUpperBoundSites(model, PATCHED_LANDING_UPPER_BOUND);
      if (original == 1 && patched == 0) {
        return PatcherState.ORIGINAL;
      }
      if (patched == 1 && original == 0) {
        return PatcherState.PATCHED;
      }
      int permissive = count(classBytes, PERMISSIVE_LANDING_CHECK);
      int walkableThreshold = count(classBytes, WALKABLE_THRESHOLD_LANDING_CHECK);
      if ((permissive == 1 || walkableThreshold == 1) && patched == 0) {
        return PatcherState.ORIGINAL;
      }
      log.info(
          "Airship landing patch state unknown; originalSites={}, permissiveSites={}, walkableThresholdSites={}, patchedSites={}",
          original,
          permissive,
          walkableThreshold,
          patched);
      return PatcherState.UNKNOWN;
    } catch (RuntimeException | LinkageError _) {
      return PatcherState.UNKNOWN;
    }
  }

  public static byte[] apply(byte[] classBytes) {
    PatcherState state = state(classBytes);
    log.info("Applying airship landing patch; current state={}", state);
    if (state == PatcherState.PATCHED) {
      return classBytes.clone();
    }
    if (state != PatcherState.ORIGINAL) {
      throw new IllegalStateException("Unsupported i.class layout for airship landing patch.");
    }
    byte[] patched = applyClassFilePatch(classBytes);
    if (state(patched) != PatcherState.PATCHED) {
      patched = applyLegacyDraftUpgrade(classBytes);
      if (state(patched) != PatcherState.PATCHED) {
        throw new IllegalStateException(
            "Airship landing patch did not produce the expected bytecode.");
      }
    }
    log.info("Airship landing patch applied");
    return patched;
  }

  private static byte[] applyClassFilePatch(byte[] classBytes) {
    ClassFile classFile = ClassFile.of();
    ClassModel model = classFile.parse(classBytes);
    return classFile.transformClass(
        model,
        java.lang.classfile.ClassTransform.transformingMethodBodies(
            AirshipLandingClassPatcher::isLandingMethod,
            java.lang.classfile.CodeTransform.ofStateful(LandingCheckTransform::new)));
  }

  private static byte[] applyLegacyDraftUpgrade(byte[] classBytes) {
    byte[] upgraded = classBytes.clone();
    int offset = indexOf(upgraded, PERMISSIVE_LANDING_CHECK, 0);
    if (offset >= 0) {
      copyStockLandingCheck(upgraded, offset);
      return upgraded;
    }
    offset = indexOf(upgraded, WALKABLE_THRESHOLD_LANDING_CHECK, 0);
    if (offset >= 0) {
      copyStockLandingCheck(upgraded, offset);
    }
    return upgraded;
  }

  private static void copyStockLandingCheck(byte[] data, int offset) {
    byte[] replacement = {
      (byte) 0x59, // dup
      (byte) 0x3c, // istore_1
      (byte) 0x99, // ifeq success
      (byte) 0x00,
      (byte) 0x0f,
      (byte) 0x10, // bipush 10
      (byte) SAFE_LANDING_LOWER_BOUND,
      (byte) 0x1b, // iload_1
      (byte) 0xa3, // if_icmpgt fail
      (byte) 0x00,
      (byte) 0x43,
      (byte) 0x1b, // iload_1
      (byte) 0x10, // bipush upperBound
      (byte) AirshipLandingClassPatcher.PATCHED_LANDING_UPPER_BOUND,
      (byte) 0xa3, // if_icmpgt fail
      (byte) 0x00,
      (byte) 0x3d
    };
    System.arraycopy(replacement, 0, data, offset, replacement.length);
  }

  private static int countLandingUpperBoundSites(ClassModel model, int upperBound) {
    int matches = 0;
    for (MethodModel method : model.methods()) {
      if (!isLandingMethod(method)) {
        continue;
      }
      List<Instruction> instructions = instructions(method);
      for (int i = 0; i < instructions.size(); i++) {
        if (isLandingUpperBoundSite(instructions, i, upperBound)) {
          matches++;
        }
      }
    }
    return matches;
  }

  private static boolean isLandingMethod(MethodModel method) {
    return LANDING_METHOD.equals(method.methodName().stringValue())
        && LANDING_DESCRIPTOR.equals(method.methodType().stringValue());
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

  private static boolean isLandingUpperBoundSite(
      List<Instruction> instructions, int offset, int upperBound) {
    return offset >= 7
        && offset + 1 < instructions.size()
        && instructions.get(offset - 7).opcode() == Opcode.DUP
        && instructions.get(offset - 6).opcode() == Opcode.ISTORE_1
        && instructions.get(offset - 5).opcode() == Opcode.IFEQ
        && isPush(instructions.get(offset - 4), SAFE_LANDING_LOWER_BOUND)
        && instructions.get(offset - 3).opcode() == Opcode.ILOAD_1
        && instructions.get(offset - 2).opcode() == Opcode.IF_ICMPGT
        && instructions.get(offset - 1).opcode() == Opcode.ILOAD_1
        && isPush(instructions.get(offset), upperBound)
        && instructions.get(offset + 1).opcode() == Opcode.IF_ICMPGT;
  }

  private static boolean isPush(Instruction instruction, int value) {
    return instruction instanceof ConstantInstruction constant
        && constant.constantValue() instanceof Integer integer
        && integer == value;
  }

  private static final class LandingCheckTransform implements java.lang.classfile.CodeTransform {
    private final List<Instruction> recentInstructions = new ArrayList<>();

    @Override
    public void accept(CodeBuilder builder, CodeElement element) {
      if (element instanceof Instruction instruction
          && isPush(instruction, STOCK_LANDING_UPPER_BOUND)
          && isRecentLandingUpperBoundContext()) {
        builder.bipush(PATCHED_LANDING_UPPER_BOUND);
        remember(instruction);
        return;
      }
      builder.with(element);
      if (element instanceof Instruction instruction) {
        remember(instruction);
      } else {
        recentInstructions.clear();
      }
    }

    private boolean isRecentLandingUpperBoundContext() {
      int size = recentInstructions.size();
      return size >= 7
          && recentInstructions.get(size - 7).opcode() == Opcode.DUP
          && recentInstructions.get(size - 6).opcode() == Opcode.ISTORE_1
          && recentInstructions.get(size - 5).opcode() == Opcode.IFEQ
          && isPush(recentInstructions.get(size - 4), SAFE_LANDING_LOWER_BOUND)
          && recentInstructions.get(size - 3).opcode() == Opcode.ILOAD_1
          && recentInstructions.get(size - 2).opcode() == Opcode.IF_ICMPGT
          && recentInstructions.get(size - 1).opcode() == Opcode.ILOAD_1;
    }

    private void remember(Instruction instruction) {
      recentInstructions.add(instruction);
      if (recentInstructions.size() > 7) {
        recentInstructions.removeFirst();
      }
    }
  }

  private static int count(byte[] data, byte[] pattern) {
    int matches = 0;
    int offset = 0;
    while ((offset = indexOf(data, pattern, offset)) >= 0) {
      matches++;
      offset += pattern.length;
    }
    return matches;
  }

  private static int indexOf(byte[] data, byte[] pattern, int start) {
    if (pattern.length == 0 || data.length < pattern.length) {
      return -1;
    }
    int max = data.length - pattern.length;
    for (int i = Math.max(0, start); i <= max; i++) {
      if (Arrays.equals(data, i, i + pattern.length, pattern, 0, pattern.length)) {
        return i;
      }
    }
    return -1;
  }
}
