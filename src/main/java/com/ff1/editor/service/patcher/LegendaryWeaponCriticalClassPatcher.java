package com.ff1.editor.service.patcher;

import com.ff1.editor.utils.CldcStackMapStripper;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.StoreInstruction;
import java.lang.constant.ClassDesc;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** Patches g.class so Excalibur and Masamune never miss and always crit. */
@Slf4j
public final class LegendaryWeaponCriticalClassPatcher {

  public static final String ENTRY_NAME = HeroLevelGrowthClassPatcher.ENTRY_NAME;

  private static final String PHYSICAL_DAMAGE_METHOD = "a";
  private static final String PHYSICAL_DAMAGE_DESCRIPTOR = "(ZZII)V";
  private static final int ATTACKER_IS_PARTY_LOCAL = 0;
  private static final int ATTACKER_INDEX_LOCAL = 2;
  private static final int BASE_HIT_COUNT_LOCAL = 5;
  private static final int CRITICAL_THRESHOLD_LOCAL = 7;
  private static final int HIT_CHANCE_LOCAL = 21;
  private static final int LOOP_HIT_COUNT_LOCAL = 22;
  private static final int EXCALIBUR_WEAPON_INDEX = 39;
  private static final int MASAMUNE_WEAPON_INDEX = 40;
  private static final int NO_MISS_HIT_CHANCE = 255;
  private static final int ALWAYS_CRIT_THRESHOLD = 200;
  private static final int PATCHED_WINDOW_SIZE = 20;
  private static final String STATE_CLASS_NAME = "j";
  private static final String STATE_FIELD = "a";
  private static final String STATE_DESCRIPTOR = "Lk;";
  private static final String PARTY_CLASS_NAME = "k";
  private static final String PARTY_FIELD = "a";
  private static final String PARTY_DESCRIPTOR = "[La;";
  private static final String HERO_CLASS_NAME = "a";
  private static final String EQUIPPED_WEAPON_FIELD = "h";
  private static final String EQUIPPED_WEAPON_DESCRIPTOR = "B";

  public enum State {
    ORIGINAL,
    PATCHED,
    UNKNOWN
  }

  private LegendaryWeaponCriticalClassPatcher() {}

  public static State state(byte[] classBytes) {
    try {
      ClassModel model = ClassFile.of().parse(classBytes);
      List<Instruction> instructions = physicalDamageInstructions(model);
      if (instructions.isEmpty()) {
        log.info("Legendary weapon critical patch state unknown; physical damage method not found");
        return State.UNKNOWN;
      }
      int hitCountCopySites = hitCountCopySites(instructions);
      int patchedSites = patchedLegendaryWeaponSites(instructions);
      if (hitCountCopySites == 1 && patchedSites == 0) {
        return State.ORIGINAL;
      }
      if (hitCountCopySites == 1 && patchedSites == 1) {
        return State.PATCHED;
      }
      log.info(
          "Legendary weapon critical patch state unknown; hitCountCopySites={}, patchedSites={}",
          hitCountCopySites,
          patchedSites);
      return State.UNKNOWN;
    } catch (RuntimeException | LinkageError _) {
      return State.UNKNOWN;
    }
  }

  public static byte[] apply(byte[] classBytes) {
    State state = state(classBytes);
    log.info("Applying legendary weapon critical class patch; current state={}", state);
    if (state == State.PATCHED) {
      return classBytes.clone();
    }
    if (state != State.ORIGINAL) {
      throw new IllegalStateException(
          "Unsupported g.class layout for legendary weapon critical patch.");
    }

    ClassFile classFile =
        ClassFile.of(
            ClassFile.StackMapsOption.DROP_STACK_MAPS,
            ClassFile.DebugElementsOption.DROP_DEBUG,
            ClassFile.LineNumbersOption.DROP_LINE_NUMBERS);
    ClassModel model = classFile.parse(classBytes);
    PatchCounter counter = new PatchCounter();
    byte[] patched =
        classFile.transformClass(
            model,
            java.lang.classfile.ClassTransform.transformingMethodBodies(
                LegendaryWeaponCriticalClassPatcher::isPhysicalDamageMethod,
                java.lang.classfile.CodeTransform.ofStateful(() -> new ApplyTransform(counter))));
    patched = stripStaleStackMap(patched);
    State patchedState = state(patched);
    if (counter.count() != 1 || patchedState != State.PATCHED) {
      throw new IllegalStateException(
          "Expected one legendary weapon critical site in %s but patched %d; state=%s."
              .formatted(ENTRY_NAME, counter.count(), patchedState));
    }
    log.info("Legendary weapon critical class patch applied");
    return patched;
  }

  public static byte[] remove(byte[] classBytes) {
    State state = state(classBytes);
    log.info("Removing legendary weapon critical class patch; current state={}", state);
    if (state == State.ORIGINAL) {
      return classBytes.clone();
    }
    if (state != State.PATCHED) {
      throw new IllegalStateException(
          "Unsupported g.class layout for legendary weapon critical patch removal.");
    }

    byte[] strippedClassBytes = stripStaleStackMap(classBytes);
    ClassFile classFile =
        ClassFile.of(
            ClassFile.StackMapsOption.DROP_STACK_MAPS,
            ClassFile.DebugElementsOption.DROP_DEBUG,
            ClassFile.LineNumbersOption.DROP_LINE_NUMBERS);
    ClassModel model = classFile.parse(strippedClassBytes);
    PatchCounter counter = new PatchCounter();
    byte[] restored =
        classFile.transformClass(
            model,
            java.lang.classfile.ClassTransform.transformingMethodBodies(
                LegendaryWeaponCriticalClassPatcher::isPhysicalDamageMethod,
                java.lang.classfile.CodeTransform.ofStateful(() -> new RemoveTransform(counter))));
    restored = stripStaleStackMap(restored);
    State restoredState = state(restored);
    if (counter.count() != 1 || restoredState != State.ORIGINAL) {
      throw new IllegalStateException(
          "Expected one legendary weapon critical removal site in %s but removed %d; state=%s."
              .formatted(ENTRY_NAME, counter.count(), restoredState));
    }
    log.info("Legendary weapon critical class patch removed");
    return restored;
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

  private static int hitCountCopySites(List<Instruction> instructions) {
    int matches = 0;
    for (int i = 0; i <= instructions.size() - 2; i++) {
      if (isIntLoad(instructions.get(i), BASE_HIT_COUNT_LOCAL)
          && isIntStore(instructions.get(i + 1), LOOP_HIT_COUNT_LOCAL)) {
        matches++;
      }
    }
    return matches;
  }

  private static int patchedLegendaryWeaponSites(List<Instruction> instructions) {
    int matches = 0;
    for (int i = 0; i <= instructions.size() - PATCHED_WINDOW_SIZE; i++) {
      if (isPatchedLegendaryWeaponWindow(instructions.subList(i, i + PATCHED_WINDOW_SIZE))) {
        matches++;
      }
    }
    return matches;
  }

  private static boolean isPatchedLegendaryWeaponWindow(List<Instruction> instructions) {
    return instructions.size() == PATCHED_WINDOW_SIZE
        && isIntLoad(instructions.get(0), ATTACKER_IS_PARTY_LOCAL)
        && instructions.get(1).opcode() == Opcode.IFEQ
        && isStateRead(instructions.get(2))
        && isPartyRead(instructions.get(3))
        && isIntLoad(instructions.get(4), ATTACKER_INDEX_LOCAL)
        && instructions.get(5).opcode() == Opcode.AALOAD
        && isEquippedWeaponRead(instructions.get(6))
        && isPush(instructions.get(7), EXCALIBUR_WEAPON_INDEX)
        && instructions.get(8).opcode() == Opcode.IF_ICMPEQ
        && isStateRead(instructions.get(9))
        && isPartyRead(instructions.get(10))
        && isIntLoad(instructions.get(11), ATTACKER_INDEX_LOCAL)
        && instructions.get(12).opcode() == Opcode.AALOAD
        && isEquippedWeaponRead(instructions.get(13))
        && isPush(instructions.get(14), MASAMUNE_WEAPON_INDEX)
        && instructions.get(15).opcode() == Opcode.IF_ICMPNE
        && isPush(instructions.get(16), NO_MISS_HIT_CHANCE)
        && isIntStore(instructions.get(17), HIT_CHANCE_LOCAL)
        && isPush(instructions.get(18), ALWAYS_CRIT_THRESHOLD)
        && isIntStore(instructions.get(19), CRITICAL_THRESHOLD_LOCAL);
  }

  private static boolean isIntLoad(Instruction instruction, int slot) {
    return instruction instanceof LoadInstruction load
        && load.opcode().kind() == Opcode.Kind.LOAD
        && load.slot() == slot;
  }

  private static boolean isIntStore(Instruction instruction, int slot) {
    return instruction instanceof StoreInstruction store
        && store.opcode().kind() == Opcode.Kind.STORE
        && store.slot() == slot;
  }

  private static boolean isStateRead(Instruction instruction) {
    return instruction instanceof FieldInstruction field
        && field.opcode() == Opcode.GETSTATIC
        && STATE_CLASS_NAME.equals(field.owner().asInternalName())
        && STATE_FIELD.equals(field.name().stringValue())
        && STATE_DESCRIPTOR.equals(field.type().stringValue());
  }

  private static boolean isPartyRead(Instruction instruction) {
    return instruction instanceof FieldInstruction field
        && field.opcode() == Opcode.GETFIELD
        && PARTY_CLASS_NAME.equals(field.owner().asInternalName())
        && PARTY_FIELD.equals(field.name().stringValue())
        && PARTY_DESCRIPTOR.equals(field.type().stringValue());
  }

  private static boolean isEquippedWeaponRead(Instruction instruction) {
    return instruction instanceof FieldInstruction field
        && field.opcode() == Opcode.GETFIELD
        && HERO_CLASS_NAME.equals(field.owner().asInternalName())
        && EQUIPPED_WEAPON_FIELD.equals(field.name().stringValue())
        && EQUIPPED_WEAPON_DESCRIPTOR.equals(field.type().stringValue());
  }

  private static boolean isPush(Instruction instruction, int value) {
    return instruction instanceof ConstantInstruction constant
        && constant.constantValue() instanceof Integer integer
        && integer == value;
  }

  private static void emitLegendaryWeaponPatch(CodeBuilder builder) {
    var notLegendary = builder.newLabel();
    var legendary = builder.newLabel();
    builder
        .iload(ATTACKER_IS_PARTY_LOCAL)
        .ifeq(notLegendary)
        .getstatic(
            ClassDesc.of(STATE_CLASS_NAME), STATE_FIELD, ClassDesc.ofDescriptor(STATE_DESCRIPTOR))
        .getfield(
            ClassDesc.of(PARTY_CLASS_NAME), PARTY_FIELD, ClassDesc.ofDescriptor(PARTY_DESCRIPTOR))
        .iload(ATTACKER_INDEX_LOCAL)
        .aaload()
        .getfield(
            ClassDesc.of(HERO_CLASS_NAME),
            EQUIPPED_WEAPON_FIELD,
            ClassDesc.ofDescriptor(EQUIPPED_WEAPON_DESCRIPTOR))
        .bipush(EXCALIBUR_WEAPON_INDEX)
        .if_icmpeq(legendary)
        .getstatic(
            ClassDesc.of(STATE_CLASS_NAME), STATE_FIELD, ClassDesc.ofDescriptor(STATE_DESCRIPTOR))
        .getfield(
            ClassDesc.of(PARTY_CLASS_NAME), PARTY_FIELD, ClassDesc.ofDescriptor(PARTY_DESCRIPTOR))
        .iload(ATTACKER_INDEX_LOCAL)
        .aaload()
        .getfield(
            ClassDesc.of(HERO_CLASS_NAME),
            EQUIPPED_WEAPON_FIELD,
            ClassDesc.ofDescriptor(EQUIPPED_WEAPON_DESCRIPTOR))
        .bipush(MASAMUNE_WEAPON_INDEX)
        .if_icmpne(notLegendary)
        .labelBinding(legendary)
        .sipush(NO_MISS_HIT_CHANCE)
        .istore(HIT_CHANCE_LOCAL)
        .sipush(ALWAYS_CRIT_THRESHOLD)
        .istore(CRITICAL_THRESHOLD_LOCAL)
        .labelBinding(notLegendary);
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

  private static final class ApplyTransform implements java.lang.classfile.CodeTransform {
    private final PatchCounter counter;
    private CodeElement held;
    private boolean patched;

    private ApplyTransform(PatchCounter counter) {
      this.counter = counter;
    }

    @Override
    public void accept(CodeBuilder builder, CodeElement element) {
      if (patched) {
        builder.with(element);
        return;
      }
      if (held == null) {
        if (element instanceof Instruction instruction
            && isIntLoad(instruction, BASE_HIT_COUNT_LOCAL)) {
          held = element;
        } else {
          builder.with(element);
        }
        return;
      }
      if (element instanceof Instruction instruction
          && isIntStore(instruction, LOOP_HIT_COUNT_LOCAL)) {
        emitLegendaryWeaponPatch(builder);
        builder.with(held);
        builder.with(element);
        held = null;
        patched = true;
        counter.increment();
        return;
      }
      builder.with(held);
      held = null;
      accept(builder, element);
    }

    @Override
    public void atEnd(CodeBuilder builder) {
      if (held != null) {
        builder.with(held);
      }
    }
  }

  private static final class RemoveTransform implements java.lang.classfile.CodeTransform {
    private final PatchCounter counter;
    private final Deque<CodeElement> pending = new ArrayDeque<>();
    private boolean removed;
    private boolean skippingInjectedLabels;

    private RemoveTransform(PatchCounter counter) {
      this.counter = counter;
    }

    @Override
    public void accept(CodeBuilder builder, CodeElement element) {
      if (removed) {
        if (skippingInjectedLabels && !(element instanceof Instruction)) {
          return;
        }
        skippingInjectedLabels = false;
        builder.with(element);
        return;
      }
      pending.addLast(element);
      while (!pending.isEmpty() && !(pending.peekFirst() instanceof Instruction)) {
        builder.with(pending.removeFirst());
      }
      if (matchesPending()) {
        removePatchedWindow();
        removed = true;
        skippingInjectedLabels = true;
        counter.increment();
        return;
      }
      while (instructionCount() > PATCHED_WINDOW_SIZE) {
        builder.with(pending.removeFirst());
      }
    }

    @Override
    public void atEnd(CodeBuilder builder) {
      while (!pending.isEmpty()) {
        builder.with(pending.removeFirst());
      }
    }

    private boolean matchesPending() {
      List<Instruction> instructions = pendingInstructions();
      if (instructions.size() < PATCHED_WINDOW_SIZE) {
        return false;
      }
      return isPatchedLegendaryWeaponWindow(instructions.subList(0, PATCHED_WINDOW_SIZE));
    }

    private void removePatchedWindow() {
      int instructionsRemoved = 0;
      while (!pending.isEmpty() && instructionsRemoved < PATCHED_WINDOW_SIZE) {
        CodeElement element = pending.removeFirst();
        if (element instanceof Instruction) {
          instructionsRemoved++;
        }
      }
    }

    private int instructionCount() {
      int count = 0;
      for (CodeElement element : pending) {
        if (element instanceof Instruction) {
          count++;
        }
      }
      return count;
    }

    private List<Instruction> pendingInstructions() {
      List<Instruction> instructions = new ArrayList<>(pending.size());
      for (CodeElement element : pending) {
        if (element instanceof Instruction instruction) {
          instructions.add(instruction);
        }
      }
      return instructions;
    }
  }
}
