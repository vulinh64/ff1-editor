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
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** Patches i.class so Cottage clears KO before applying its normal field recovery. */
@Slf4j
public final class CottageReviveClassPatcher {

  public static final String ENTRY_NAME = FifteenSpellChargeRecoveryClassPatcher.ENTRY_NAME;

  private static final String RECOVERY_METHOD = "l";
  private static final String RECOVERY_DESCRIPTOR = "(I)V";
  private static final String SAVE_CLASS_NAME = "j";
  private static final String SAVE_FIELD = "a";
  private static final String SAVE_DESCRIPTOR = "Lk;";
  private static final String SAVE_DATA_CLASS_NAME = "k";
  private static final String HEROES_FIELD = "a";
  private static final String HEROES_DESCRIPTOR = "[La;";
  private static final String HERO_CLASS_NAME = "a";
  private static final String STATUS_FIELD = "a";
  private static final String STATUS_DESCRIPTOR = "B";
  private static final String CURRENT_HP_FIELD = "b";
  private static final String MAX_HP_FIELD = "c";
  private static final String CURRENT_CHARGES_FIELD = "b";
  private static final String MAX_CHARGES_FIELD = "c";
  private static final String CHARGES_DESCRIPTOR = "[B";
  private static final int STOCK_RECOVERY_CHARGES = 10;
  private static final int COTTAGE_ITEM_KIND = 3;

  public enum State {
    ORIGINAL,
    PATCHED,
    UNKNOWN
  }

  private CottageReviveClassPatcher() {}

  public static State state(byte[] classBytes) {
    try {
      ClassModel model = ClassFile.of().parse(classBytes);
      List<Instruction> instructions = recoveryInstructions(model);
      if (instructions.isEmpty()) {
        return State.UNKNOWN;
      }
      int statusWrites = statusWrites(instructions);
      if (statusWrites == 0) {
        return State.ORIGINAL;
      }
      if (statusWrites == 1) {
        return State.PATCHED;
      }
      log.info("Cottage revive patch state unknown; statusWrites={}", statusWrites);
      return State.UNKNOWN;
    } catch (RuntimeException | LinkageError _) {
      return State.UNKNOWN;
    }
  }

  public static byte[] apply(byte[] classBytes) {
    State state = state(classBytes);
    log.info("Applying Cottage revive class patch; current state={}", state);
    if (state == State.PATCHED) {
      return classBytes.clone();
    }
    if (state != State.ORIGINAL) {
      throw new IllegalStateException("Unsupported i.class layout for Cottage revive patch.");
    }

    int recoveryCharges = recoveryCharges(classBytes);
    ClassFile classFile = ClassFile.of();
    ClassModel model = classFile.parse(classBytes);
    PatchCounter counter = new PatchCounter();
    byte[] patched =
        classFile.transformClass(
            model,
            java.lang.classfile.ClassTransform.transformingMethodBodies(
                CottageReviveClassPatcher::isRecoveryMethod,
                java.lang.classfile.CodeTransform.ofStateful(
                    () -> new CottageReviveCodeTransform(counter, recoveryCharges))));
    patched =
        CldcStackMapStripper.stripMethodStackMap(patched, RECOVERY_METHOD, RECOVERY_DESCRIPTOR);
    State patchedState = state(patched);
    if (counter.count() != 1 || patchedState != State.PATCHED) {
      throw new IllegalStateException(
          "Expected one Cottage recovery method in %s but patched %d; state=%s."
              .formatted(ENTRY_NAME, counter.count(), patchedState));
    }
    log.info("Cottage revive class patch applied");
    return patched;
  }

  private static int recoveryCharges(byte[] classBytes) {
    ClassModel model = ClassFile.of().parse(classBytes);
    List<Instruction> instructions = recoveryInstructions(model);
    for (int i = 0; i + 1 < instructions.size(); i++) {
      if (isPush(instructions.get(i)) && instructions.get(i + 1).opcode() == Opcode.ISTORE_2) {
        return ((Integer) ((ConstantInstruction) instructions.get(i)).constantValue());
      }
    }
    return STOCK_RECOVERY_CHARGES;
  }

  private static List<Instruction> recoveryInstructions(ClassModel model) {
    for (MethodModel method : model.methods()) {
      if (isRecoveryMethod(method)) {
        return instructions(method);
      }
    }
    return List.of();
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

  private static int statusWrites(List<Instruction> instructions) {
    int matches = 0;
    for (Instruction instruction : instructions) {
      if (isStatusWrite(instruction)) {
        matches++;
      }
    }
    return matches;
  }

  private static boolean isStatusWrite(Instruction instruction) {
    return instruction instanceof FieldInstruction field
        && field.opcode() == Opcode.PUTFIELD
        && HERO_CLASS_NAME.equals(field.owner().asInternalName())
        && STATUS_FIELD.equals(field.name().stringValue())
        && STATUS_DESCRIPTOR.equals(field.type().stringValue());
  }

  private static boolean isPush(Instruction instruction) {
    return instruction instanceof ConstantInstruction constant
        && constant.constantValue() instanceof Integer;
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

  private static final class CottageReviveCodeTransform
      implements java.lang.classfile.CodeTransform {
    private final PatchCounter counter;
    private final int recoveryCharges;
    private boolean emitted;

    private CottageReviveCodeTransform(PatchCounter counter, int recoveryCharges) {
      this.counter = counter;
      this.recoveryCharges = recoveryCharges;
    }

    @Override
    public void accept(CodeBuilder builder, CodeElement element) {
      if (emitted) {
        return;
      }
      if (element instanceof Instruction) {
        emitReplacement(builder, recoveryCharges);
        emitted = true;
        counter.increment();
      } else {
        builder.with(element);
      }
    }

    private static void emitReplacement(CodeBuilder builder, int recoveryCharges) {
      Label notSleepingBag = builder.newLabel();
      Label notTent = builder.newLabel();
      Label recoveryLoop = builder.newLabel();
      Label nextHero = builder.newLabel();
      Label canRecover = builder.newLabel();
      Label chargeLoop = builder.newLabel();
      Label chargesDone = builder.newLabel();
      Label chargeWithinMax = builder.newLabel();
      Label hpWithinMax = builder.newLabel();
      Label done = builder.newLabel();

      builder.sipush(1000).istore(1).bipush(recoveryCharges).istore(2);
      builder.iload(0).iconst_1().if_icmpne(notSleepingBag);
      builder.bipush(30).istore(1).iconst_0().istore(2).goto_(recoveryLoop);
      builder.labelBinding(notSleepingBag).iload(0).iconst_2().if_icmpne(notTent);
      builder.bipush(60).istore(1).iconst_0().istore(2).goto_(recoveryLoop);
      builder.labelBinding(notTent).iload(0).iconst_3().if_icmpne(recoveryLoop);
      builder.sipush(999).istore(1);

      builder.labelBinding(recoveryLoop).iconst_0().istore(3);
      Label heroLoop = builder.newLabel();
      builder.labelBinding(heroLoop).iload(3).iconst_4().if_icmpge(done);
      loadHero(builder);
      builder
          .getfield(hero(), STATUS_FIELD, ConstantDescs.CD_byte)
          .iconst_1()
          .iand()
          .ifeq(canRecover);
      builder.iload(0).bipush(COTTAGE_ITEM_KIND).if_icmpne(nextHero);
      loadHero(builder);
      builder
          .dup()
          .getfield(hero(), STATUS_FIELD, ConstantDescs.CD_byte)
          .bipush(-2)
          .iand()
          .i2b()
          .putfield(hero(), STATUS_FIELD, ConstantDescs.CD_byte)
          .goto_(canRecover);

      builder.labelBinding(canRecover).iconst_0().istore(4);
      builder.labelBinding(chargeLoop).iload(4).bipush(8).if_icmpge(chargesDone);
      loadHero(builder);
      builder
          .getfield(hero(), CURRENT_CHARGES_FIELD, ClassDesc.ofDescriptor(CHARGES_DESCRIPTOR))
          .iload(4)
          .dup2()
          .baload()
          .iload(2)
          .iadd()
          .i2b()
          .bastore();
      loadHero(builder);
      builder
          .getfield(hero(), CURRENT_CHARGES_FIELD, ClassDesc.ofDescriptor(CHARGES_DESCRIPTOR))
          .iload(4)
          .baload();
      loadHero(builder);
      builder
          .getfield(hero(), MAX_CHARGES_FIELD, ClassDesc.ofDescriptor(CHARGES_DESCRIPTOR))
          .iload(4)
          .baload()
          .if_icmple(chargeWithinMax);
      loadHero(builder);
      builder
          .getfield(hero(), CURRENT_CHARGES_FIELD, ClassDesc.ofDescriptor(CHARGES_DESCRIPTOR))
          .iload(4);
      loadHero(builder);
      builder
          .getfield(hero(), MAX_CHARGES_FIELD, ClassDesc.ofDescriptor(CHARGES_DESCRIPTOR))
          .iload(4)
          .baload()
          .bastore();
      builder.labelBinding(chargeWithinMax).iinc(4, 1).goto_(chargeLoop);

      builder.labelBinding(chargesDone);
      loadHero(builder);
      builder
          .dup()
          .getfield(hero(), CURRENT_HP_FIELD, ConstantDescs.CD_short)
          .iload(1)
          .iadd()
          .i2s()
          .putfield(hero(), CURRENT_HP_FIELD, ConstantDescs.CD_short);
      loadHero(builder);
      builder.getfield(hero(), CURRENT_HP_FIELD, ConstantDescs.CD_short);
      loadHero(builder);
      builder.getfield(hero(), MAX_HP_FIELD, ConstantDescs.CD_short).if_icmple(hpWithinMax);
      loadHero(builder);
      loadHero(builder);
      builder.getfield(hero(), MAX_HP_FIELD, ConstantDescs.CD_short);
      builder.putfield(hero(), CURRENT_HP_FIELD, ConstantDescs.CD_short);
      builder.labelBinding(hpWithinMax);
      builder.labelBinding(nextHero).iinc(3, 1).goto_(heroLoop);
      builder.labelBinding(done).return_();
    }

    private static void loadHero(CodeBuilder builder) {
      builder
          .getstatic(
              ClassDesc.of(SAVE_CLASS_NAME), SAVE_FIELD, ClassDesc.ofDescriptor(SAVE_DESCRIPTOR))
          .getfield(
              ClassDesc.of(SAVE_DATA_CLASS_NAME),
              HEROES_FIELD,
              ClassDesc.ofDescriptor(HEROES_DESCRIPTOR))
          .iload(3)
          .aaload();
    }

    private static ClassDesc hero() {
      return ClassDesc.of(HERO_CLASS_NAME);
    }
  }
}
