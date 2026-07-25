package com.ff1.editor.service.patcher.bytecode;

import com.ff1.editor.utils.CldcStackMapStripper;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.Instruction;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.IncrementInstruction;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.StoreInstruction;
import java.lang.constant.ClassDesc;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Patches g.class so weapon affinity hits add half weapon damage and clamp hit chance to 255
 * instead of adding flat attack and hit bonuses.
 */
@Slf4j
public final class WeaponAffinityDamageClassPatcher {

  public static final String ENTRY_NAME = HeroLevelGrowthClassPatcher.ENTRY_NAME;

  private static final String PHYSICAL_DAMAGE_METHOD = "a";
  private static final String PHYSICAL_DAMAGE_DESCRIPTOR = "(ZZII)V";
  private static final int ATTACK_LOCAL = 4;
  private static final int WEAPON_INDEX_LOCAL = 7;
  private static final int CRITICAL_THRESHOLD_LOCAL = 7;
  private static final int HIT_CHANCE_LOCAL = 21;
  private static final int AFFINITY_MATCH_LOCAL = 32;
  private static final int WEAPON_DAMAGE_RUNTIME_FIELD = 3;
  private static final String ITEM_CLASS_NAME = "j";
  private static final String WEAPON_RECORDS_FIELD = "c";
  private static final String WEAPON_RECORDS_DESCRIPTOR = "[[S";
  private static final int ANY_BYTE = -1;
  private static final int[] DIRECT_WEAPON_DAMAGE_PATTERN = {
    0x15,
    ATTACK_LOCAL,
    0xb2,
    ANY_BYTE,
    ANY_BYTE,
    0x15,
    WEAPON_INDEX_LOCAL,
    0x32,
    0x06,
    0x35,
    0x05,
    0x6c,
    0x60,
    0x36,
    ATTACK_LOCAL
  };
  private static final int[] DIRECT_ACCURACY_CLAMP_PATTERN = {
    0x15, AFFINITY_MATCH_LOCAL, 0x99, ANY_BYTE, ANY_BYTE, 0x11, 0x00, 0xff, 0x36, HIT_CHANCE_LOCAL
  };

  private WeaponAffinityDamageClassPatcher() {}

  public static PatcherState state(byte[] classBytes) {
    try {
      ClassModel model = ClassFile.of().parse(classBytes);

      List<Instruction> instructions = physicalDamageInstructions(model);

      if (instructions.isEmpty()) {
        log.info("Weapon affinity damage patch state unknown; physical damage method not found");
        return PatcherState.UNKNOWN;
      }

      int originalSites = originalFlatAffinitySites(instructions);
      int damageSites = patchedWeaponDamageSites(instructions);
      int accuracySites = patchedAccuracyClampSites(instructions);
      int directDamageSites = directPatternSites(classBytes, DIRECT_WEAPON_DAMAGE_PATTERN);
      int directAccuracySites = directPatternSites(classBytes, DIRECT_ACCURACY_CLAMP_PATTERN);

      if (originalSites == 1 && damageSites == 0) {
        return PatcherState.ORIGINAL;
      }

      if (originalSites == 0
          && (damageSites == 1 || directDamageSites == 1)
          && (accuracySites >= 1 || directAccuracySites >= 1)) {
        return PatcherState.PATCHED;
      }

      log.info(
          "Weapon affinity damage patch state unknown; originalSites={}, damageSites={}, accuracySites={}, directDamageSites={}, directAccuracySites={}",
          originalSites,
          damageSites,
          accuracySites,
          directDamageSites,
          directAccuracySites);

      return PatcherState.UNKNOWN;
    } catch (RuntimeException | LinkageError e) {
      log.warn("+50% weakness affinity damage weapon patcher state error", e);

      return PatcherState.UNKNOWN;
    }
  }

  public static byte[] apply(byte[] classBytes) {
    PatcherState state = state(classBytes);

    log.info("Applying weapon affinity damage class patch; current state={}", state);

    if (state == PatcherState.PATCHED) {
      return classBytes.clone();
    }

    if (state != PatcherState.ORIGINAL) {
      throw new IllegalStateException(
          "Unsupported g.class layout for weapon affinity damage patch.");
    }

    ClassFile classFile = ClassFile.of();

    ClassModel model = classFile.parse(classBytes);

    ExtendedPatchCounter counter = new ExtendedPatchCounter();

    byte[] patched =
        classFile.transformClass(
            model,
            ClassTransform.transformingMethodBodies(
                WeaponAffinityDamageClassPatcher::isPhysicalDamageMethod,
                CodeTransform.ofStateful(() -> new WeaponAffinityDamageCodeTransform(counter))));

    patched = stripStaleStackMap(patched);

    PatcherState patchedState = state(patched);

    if (counter.flatBonusSites() != 1
        || counter.accuracySites() != 1
        || counter.weaponDamageSites() != 1
        || patchedState != PatcherState.PATCHED) {
      throw new IllegalStateException(
          "Expected one weapon affinity patch site in %s but patched flat=%d weaponDamage=%d accuracy=%d; state=%s."
              .formatted(
                  ENTRY_NAME,
                  counter.flatBonusSites(),
                  counter.weaponDamageSites(),
                  counter.accuracySites(),
                  patchedState));
    }

    log.info("Weapon affinity damage class patch applied");

    return patched;
  }

  private static byte[] stripStaleStackMap(byte[] classBytes) {
    return CldcStackMapStripper.stripMethodStackMap(
        classBytes, PHYSICAL_DAMAGE_METHOD, PHYSICAL_DAMAGE_DESCRIPTOR);
  }

  private static List<Instruction> physicalDamageInstructions(ClassModel model) {
    for (MethodModel method : model.methods()) {
      if (isPhysicalDamageMethod(method)) {
        return BytecodeInstructions.instructions(method);
      }
    }

    return Collections.emptyList();
  }

  private static boolean isPhysicalDamageMethod(MethodModel method) {
    return PHYSICAL_DAMAGE_METHOD.equals(method.methodName().stringValue())
        && PHYSICAL_DAMAGE_DESCRIPTOR.equals(method.methodType().stringValue());
  }

  private static int originalFlatAffinitySites(List<Instruction> instructions) {
    int matches = 0;

    for (int i = 0; i <= instructions.size() - 2; i++) {
      if (isOriginalFlatAffinityWindow(instructions.subList(i, i + 2))) {
        matches++;
      }
    }

    return matches;
  }

  private static int patchedAccuracyClampSites(List<Instruction> instructions) {
    int matches = 0;

    for (int i = 0; i <= instructions.size() - 2; i++) {
      if (isPush(instructions.get(i), 255)
          && isIntStore(instructions.get(i + 1), HIT_CHANCE_LOCAL)) {
        matches++;
      }
    }

    return matches;
  }

  private static int patchedWeaponDamageSites(List<Instruction> instructions) {
    int matches = 0;

    for (int i = 0; i <= instructions.size() - 10; i++) {
      if (isPatchedWeaponDamageWindow(instructions.subList(i, i + 10))) {
        matches++;
      }
    }

    return matches;
  }

  private static boolean isOriginalFlatAffinityWindow(List<Instruction> instructions) {
    return instructions.size() == 2
        && isIncrement(instructions.get(0), ATTACK_LOCAL, 4)
        && isIncrement(instructions.get(1), HIT_CHANCE_LOCAL, 40);
  }

  private static boolean isPatchedWeaponDamageWindow(List<Instruction> instructions) {
    return instructions.size() == 10
        && isIntLoad(instructions.get(0), ATTACK_LOCAL)
        && isWeaponRecordsRead(instructions.get(1))
        && isIntLoad(instructions.get(2), WEAPON_INDEX_LOCAL)
        && instructions.get(3).opcode() == Opcode.AALOAD
        && isPush(instructions.get(4), WEAPON_DAMAGE_RUNTIME_FIELD)
        && instructions.get(5).opcode() == Opcode.SALOAD
        && isPush(instructions.get(6), 2)
        && instructions.get(7).opcode() == Opcode.IDIV
        && instructions.get(8).opcode() == Opcode.IADD
        && isIntStore(instructions.get(9), ATTACK_LOCAL);
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

  private static boolean isIncrement(Instruction instruction, int slot, int constant) {
    return instruction instanceof IncrementInstruction increment
        && increment.slot() == slot
        && increment.constant() == constant;
  }

  private static boolean isWeaponRecordsRead(Instruction instruction) {
    return instruction instanceof FieldInstruction field
        && field.opcode() == Opcode.GETSTATIC
        && ITEM_CLASS_NAME.equals(field.owner().asInternalName())
        && WEAPON_RECORDS_FIELD.equals(field.name().stringValue())
        && WEAPON_RECORDS_DESCRIPTOR.equals(field.type().stringValue());
  }

  private static boolean isPush(Instruction instruction, int value) {
    return instruction instanceof ConstantInstruction constant
        && constant.constantValue() instanceof Integer integer
        && integer == value;
  }

  private static int directPatternSites(byte[] data, int[] pattern) {
    int matches = 0;
    for (int offset = 0; offset <= data.length - pattern.length; offset++) {
      if (matchesPattern(data, offset, pattern)) {
        matches++;
      }
    }
    return matches;
  }

  private static boolean matchesPattern(byte[] data, int offset, int[] pattern) {
    for (int i = 0; i < pattern.length; i++) {
      int expected = pattern[i];
      if (expected != ANY_BYTE && (data[offset + i] & 0xff) != expected) {
        return false;
      }
    }
    return true;
  }

  private static final class ExtendedPatchCounter {

    private int flatBonusSites;
    private int weaponDamageSites;
    private int accuracySites;

    void flatBonusPatched() {
      flatBonusSites++;
    }

    void accuracyPatched() {
      accuracySites++;
    }

    void weaponDamagePatched() {
      weaponDamageSites++;
    }

    int flatBonusSites() {
      return flatBonusSites;
    }

    int accuracySites() {
      return accuracySites;
    }

    int weaponDamageSites() {
      return weaponDamageSites;
    }
  }

  private static final class WeaponAffinityDamageCodeTransform implements CodeTransform {

    private final ExtendedPatchCounter counter;
    private final Deque<CodeElement> pending = new ArrayDeque<>();
    private boolean started;
    private boolean accuracyInjected;

    private WeaponAffinityDamageCodeTransform(ExtendedPatchCounter counter) {
      this.counter = counter;
    }

    private static boolean isCriticalClampStart(List<Instruction> instructions) {
      return instructions.size() == 3
          && isIntLoad(instructions.get(0), CRITICAL_THRESHOLD_LOCAL)
          && isIntLoad(instructions.get(1), HIT_CHANCE_LOCAL)
          && instructions.get(2) instanceof BranchInstruction branch
          && branch.opcode() == Opcode.IF_ICMPLT;
    }

    @Override
    public void accept(CodeBuilder builder, CodeElement element) {
      if (!started) {
        builder.iconst_0().istore(AFFINITY_MATCH_LOCAL);

        started = true;
      }

      pending.addLast(element);

      if (pending.size() < 4) {
        return;
      }

      if (pending.size() > 4) {
        flushFirst(builder);
      }

      patchPending(builder);
    }

    @Override
    public void atEnd(CodeBuilder builder) {
      while (!pending.isEmpty()) {
        builder.with(pending.removeFirst());
      }
    }

    private void patchPending(CodeBuilder builder) {
      List<Instruction> instructions = pendingInstructions();

      List<Instruction> trailingInstructions = trailingInstructions();

      if (isOriginalFlatAffinityWindow(trailingInstructions)) {
        while (pending.size() > 2) {
          builder.with(pending.removeFirst());
        }

        emitWeaponDamageBonus(builder);

        pending.removeFirst();
        pending.removeFirst();
        counter.flatBonusPatched();
        counter.weaponDamagePatched();

        return;
      }
      if (!accuracyInjected
          && instructions.size() >= 3
          && isCriticalClampStart(instructions.subList(0, 3))) {
        emitAccuracyClamp(builder);

        accuracyInjected = true;

        counter.accuracyPatched();
      }
    }

    private void flushFirst(CodeBuilder builder) {
      builder.with(pending.removeFirst());
    }

    private List<Instruction> pendingInstructions() {
      List<Instruction> instructions = new ArrayList<>(pending.size());

      for (CodeElement element : pending) {
        if (element instanceof Instruction instruction) {
          instructions.add(instruction);
        } else {
          return Collections.emptyList();
        }
      }

      return instructions;
    }

    private List<Instruction> trailingInstructions() {
      if (pending.size() < 2) {
        return List.of();
      }

      List<CodeElement> elements = new ArrayList<>(pending);

      List<Instruction> instructions = new ArrayList<>(2);

      for (int i = elements.size() - 2; i < elements.size(); i++) {
        if (elements.get(i) instanceof Instruction instruction) {
          instructions.add(instruction);
        } else {
          return Collections.emptyList();
        }
      }

      return instructions;
    }

    private static void emitAccuracyClamp(CodeBuilder builder) {
      var noAffinity = builder.newLabel();

      builder
          .iload(AFFINITY_MATCH_LOCAL)
          .ifeq(noAffinity)
          .sipush(255)
          .istore(HIT_CHANCE_LOCAL)
          .labelBinding(noAffinity);
    }

    private static void emitWeaponDamageBonus(CodeBuilder builder) {
      builder
          .iload(ATTACK_LOCAL)
          .getstatic(
              ClassDesc.of(ITEM_CLASS_NAME),
              WEAPON_RECORDS_FIELD,
              ClassDesc.ofDescriptor(WEAPON_RECORDS_DESCRIPTOR))
          .iload(WEAPON_INDEX_LOCAL)
          .aaload()
          .iconst_3()
          .saload()
          .iconst_2()
          .idiv()
          .iadd()
          .istore(ATTACK_LOCAL)
          .iconst_1()
          .istore(AFFINITY_MATCH_LOCAL);
    }
  }
}
