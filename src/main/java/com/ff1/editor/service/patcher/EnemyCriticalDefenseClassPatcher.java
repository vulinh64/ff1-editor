package com.ff1.editor.service.patcher;

import com.ff1.editor.utils.CldcStackMapStripper;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.IncrementInstruction;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.StoreInstruction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Patches g.class so enemy critical hits double defense-reduced damage instead of adding raw
 * pre-defense damage.
 */
@Slf4j
public final class EnemyCriticalDefenseClassPatcher {

  public static final String ENTRY_NAME = HeroLevelGrowthClassPatcher.ENTRY_NAME;

  private static final String PHYSICAL_DAMAGE_METHOD = "a";
  private static final String PHYSICAL_DAMAGE_DESCRIPTOR = "(ZZII)V";
  private static final int DAMAGE_LOCAL = 26;
  private static final int ATTACK_ROLL_LOCAL = 27;
  private static final int CRIT_COUNT_LOCAL = 23;

  public enum State {
    ORIGINAL,
    PATCHED,
    UNKNOWN
  }

  private EnemyCriticalDefenseClassPatcher() {}

  public static State state(byte[] classBytes) {
    try {
      ClassModel model = ClassFile.of().parse(classBytes);
      List<Instruction> instructions = physicalDamageInstructions(model);
      if (instructions.isEmpty()) {
        log.info("Enemy critical-defense patch state unknown; physical damage method not found");
        return State.UNKNOWN;
      }
      int originalSites = originalCritBonusSites(instructions);
      int patchedSites = patchedCritBonusSites(instructions);
      if (originalSites == 1 && patchedSites == 0) {
        return State.ORIGINAL;
      }
      if (originalSites == 1 && patchedSites == 1) {
        return State.PATCHED;
      }
      log.info(
          "Enemy critical-defense patch state unknown; originalSites={}, patchedSites={}",
          originalSites,
          patchedSites);
      return State.UNKNOWN;
    } catch (RuntimeException | LinkageError _) {
      return State.UNKNOWN;
    }
  }

  public static byte[] apply(byte[] classBytes) {
    State state = state(classBytes);
    log.info("Applying enemy critical-defense class patch; current state={}", state);
    if (state == State.PATCHED) {
      return classBytes.clone();
    }
    if (state != State.ORIGINAL) {
      throw new IllegalStateException(
          "Unsupported g.class layout for enemy critical-defense patch.");
    }

    ClassFile classFile = ClassFile.of();
    ClassModel model = classFile.parse(classBytes);
    PatchCounter counter = new PatchCounter();
    byte[] patched =
        classFile.transformClass(
            model,
            java.lang.classfile.ClassTransform.transformingMethodBodies(
                EnemyCriticalDefenseClassPatcher::isPhysicalDamageMethod,
                java.lang.classfile.CodeTransform.ofStateful(
                    () -> new EnemyCriticalDefenseCodeTransform(counter))));
    patched = stripStaleStackMap(patched);
    State patchedState = state(patched);
    if (counter.count() != 1 || patchedState != State.PATCHED) {
      throw new IllegalStateException(
          "Expected one enemy critical-defense site in %s but patched %d; state=%s."
              .formatted(ENTRY_NAME, counter.count(), patchedState));
    }
    log.info("Enemy critical-defense class patch applied");
    return patched;
  }

  private static byte[] stripStaleStackMap(byte[] classBytes) {
    return CldcStackMapStripper.stripMethodStackMap(
        classBytes, PHYSICAL_DAMAGE_METHOD, PHYSICAL_DAMAGE_DESCRIPTOR);
  }

  private static List<Instruction> physicalDamageInstructions(ClassModel model) {
    for (MethodModel method : model.methods()) {
      if (isPhysicalDamageMethod(method)) {
        return instructions(method);
      }
    }
    return List.of();
  }

  private static boolean isPhysicalDamageMethod(MethodModel method) {
    return PHYSICAL_DAMAGE_METHOD.equals(method.methodName().stringValue())
        && PHYSICAL_DAMAGE_DESCRIPTOR.equals(method.methodType().stringValue());
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

  private static int originalCritBonusSites(List<Instruction> instructions) {
    int matches = 0;
    for (int i = 0; i <= instructions.size() - 5; i++) {
      if (isOriginalCritBonusWindow(instructions.subList(i, i + 5))) {
        matches++;
      }
    }
    return matches;
  }

  private static int patchedCritBonusSites(List<Instruction> instructions) {
    int matches = 0;
    for (int i = 0; i <= instructions.size() - 4; i++) {
      if (isPatchedDefenseCritBonusWindow(instructions.subList(i, i + 4))) {
        matches++;
      }
    }
    return matches;
  }

  private static boolean isOriginalCritBonusWindow(List<Instruction> instructions) {
    return instructions.size() == 5
        && isIntLoad(instructions.get(0), DAMAGE_LOCAL)
        && isIntLoad(instructions.get(1), ATTACK_ROLL_LOCAL)
        && instructions.get(2).opcode() == Opcode.IADD
        && isIntStore(instructions.get(3))
        && isIncrement(instructions.get(4));
  }

  private static boolean isPatchedDefenseCritBonusWindow(List<Instruction> instructions) {
    return instructions.size() == 4
        && isIntLoad(instructions.get(0), DAMAGE_LOCAL)
        && isIntLoad(instructions.get(1), DAMAGE_LOCAL)
        && instructions.get(2).opcode() == Opcode.IADD
        && isIntStore(instructions.get(3));
  }

  private static boolean isIntLoad(Instruction instruction, int slot) {
    return instruction instanceof LoadInstruction load
        && load.opcode().kind() == Opcode.Kind.LOAD
        && load.slot() == slot;
  }

  private static boolean isIntStore(Instruction instruction) {
    return instruction instanceof StoreInstruction store
        && store.opcode().kind() == Opcode.Kind.STORE
        && store.slot() == EnemyCriticalDefenseClassPatcher.DAMAGE_LOCAL;
  }

  private static boolean isIncrement(Instruction instruction) {
    return instruction instanceof IncrementInstruction increment
        && increment.slot() == EnemyCriticalDefenseClassPatcher.CRIT_COUNT_LOCAL
        && increment.constant() == 1;
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

  private static final class EnemyCriticalDefenseCodeTransform
      implements java.lang.classfile.CodeTransform {
    private final PatchCounter counter;
    private final Deque<CodeElement> pending = new ArrayDeque<>();
    private boolean patched;

    private EnemyCriticalDefenseCodeTransform(PatchCounter counter) {
      this.counter = counter;
    }

    @Override
    public void accept(CodeBuilder builder, CodeElement element) {
      if (patched) {
        builder.with(element);
        return;
      }

      pending.addLast(element);
      if (pending.size() < 5) {
        return;
      }
      if (pending.size() > 5) {
        builder.with(pending.removeFirst());
      }
      if (isOriginalCritBonusWindow(pendingInstructions())) {
        emitEnemyDefenseCritBonus(builder);
        pending.clear();
        patched = true;
        counter.increment();
      }
    }

    @Override
    public void atEnd(CodeBuilder builder) {
      while (!pending.isEmpty()) {
        builder.with(pending.removeFirst());
      }
    }

    private List<Instruction> pendingInstructions() {
      List<Instruction> instructions = new ArrayList<>(5);
      for (CodeElement element : pending) {
        if (element instanceof Instruction instruction) {
          instructions.add(instruction);
        } else {
          return List.of();
        }
      }
      return instructions;
    }

    private static void emitEnemyDefenseCritBonus(CodeBuilder builder) {
      Label originalBonus = builder.newLabel();
      Label afterBonus = builder.newLabel();
      builder
          .iload(0)
          .ifne(originalBonus)
          .iload(1)
          .ifeq(originalBonus)
          .iload(DAMAGE_LOCAL)
          .iload(DAMAGE_LOCAL)
          .iadd()
          .istore(DAMAGE_LOCAL)
          .goto_(afterBonus)
          .labelBinding(originalBonus)
          .iload(DAMAGE_LOCAL)
          .iload(ATTACK_ROLL_LOCAL)
          .iadd()
          .istore(DAMAGE_LOCAL)
          .labelBinding(afterBonus)
          .iinc(CRIT_COUNT_LOCAL, 1);
    }
  }
}
