package com.ff1.editor.service.patcher.bytecode;

import com.ff1.editor.utils.CldcStackMapStripper;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.util.Collections;
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

  private CottageReviveClassPatcher() {}

  public static PatcherState state(byte[] classBytes) {
    try {
      ClassModel model = ClassFile.of().parse(classBytes);

      List<Instruction> instructions = recoveryInstructions(model);

      if (instructions.isEmpty()) {
        return PatcherState.UNKNOWN;
      }

      int statusWrites = statusWrites(instructions);

      return switch (statusWrites) {
        case 0 -> PatcherState.ORIGINAL;
        case 1 -> PatcherState.PATCHED;
        default -> {
          log.info("Cottage revive patch state unknown; statusWrites={}", statusWrites);

          yield PatcherState.UNKNOWN;
        }
      };
    } catch (RuntimeException | LinkageError e) {
      log.warn("Cottage revival patcher state error", e);

      return PatcherState.UNKNOWN;
    }
  }

  public static byte[] apply(byte[] classBytes) {
    PatcherState state = state(classBytes);

    log.info("Applying Cottage revive class patch; current state={}", state);

    if (state == PatcherState.PATCHED) {
      return classBytes.clone();
    }

    if (state != PatcherState.ORIGINAL) {
      throw new IllegalStateException("Unsupported i.class layout for Cottage revive patch.");
    }

    int recoveryCharges = recoveryCharges(classBytes);

    ClassFile classFile = ClassFile.of();

    ClassModel model = classFile.parse(classBytes);

    PatchSiteCounter counter = PatchSiteCounter.create();

    byte[] patched =
        classFile.transformClass(
            model,
            ClassTransform.transformingMethodBodies(
                CottageReviveClassPatcher::isRecoveryMethod,
                CodeTransform.ofStateful(
                    () -> new CottageReviveCodeTransform(counter, recoveryCharges))));

    patched =
        CldcStackMapStripper.stripMethodStackMap(patched, RECOVERY_METHOD, RECOVERY_DESCRIPTOR);

    PatcherState patchedState = state(patched);

    if (counter.count() != 1 || patchedState != PatcherState.PATCHED) {
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
        return BytecodeInstructions.instructions(method);
      }
    }

    return Collections.emptyList();
  }

  private static boolean isRecoveryMethod(MethodModel method) {
    return RECOVERY_METHOD.equals(method.methodName().stringValue())
        && RECOVERY_DESCRIPTOR.equals(method.methodType().stringValue());
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

  private static final class CottageReviveCodeTransform implements CodeTransform {

    private final PatchSiteCounter counter;
    private final int recoveryCharges;
    private boolean emitted;

    private CottageReviveCodeTransform(PatchSiteCounter counter, int recoveryCharges) {
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
      RecoveryLabels labels = RecoveryLabels.create(builder);

      builder.sipush(1000).istore(1).bipush(recoveryCharges).istore(2);
      builder.iload(0).iconst_1().if_icmpne(labels.notSleepingBag());
      builder.bipush(30).istore(1).iconst_0().istore(2).goto_(labels.recoveryLoop());
      builder.labelBinding(labels.notSleepingBag()).iload(0).iconst_2().if_icmpne(labels.notTent());
      builder.bipush(60).istore(1).iconst_0().istore(2).goto_(labels.recoveryLoop());
      builder.labelBinding(labels.notTent()).iload(0).iconst_3().if_icmpne(labels.recoveryLoop());
      builder.sipush(999).istore(1);

      builder.labelBinding(labels.recoveryLoop()).iconst_0().istore(3);

      Label heroLoop = builder.newLabel();

      builder.labelBinding(heroLoop).iload(3).iconst_4().if_icmpge(labels.done());

      loadHero(builder);

      builder
          .getfield(hero(), STATUS_FIELD, ConstantDescs.CD_byte)
          .iconst_1()
          .iand()
          .ifeq(labels.canRecover());
      builder.iload(0).bipush(COTTAGE_ITEM_KIND).if_icmpne(labels.nextHero());

      loadHero(builder);

      builder
          .dup()
          .getfield(hero(), STATUS_FIELD, ConstantDescs.CD_byte)
          .bipush(-2)
          .iand()
          .i2b()
          .putfield(hero(), STATUS_FIELD, ConstantDescs.CD_byte)
          .goto_(labels.canRecover());

      builder.labelBinding(labels.canRecover()).iconst_0().istore(4);
      builder.labelBinding(labels.chargeLoop()).iload(4).bipush(8).if_icmpge(labels.chargesDone());

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
          .if_icmple(labels.chargeWithinMax());

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

      builder.labelBinding(labels.chargeWithinMax()).iinc(4, 1).goto_(labels.chargeLoop());

      builder.labelBinding(labels.chargesDone());

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

      builder
          .getfield(hero(), MAX_HP_FIELD, ConstantDescs.CD_short)
          .if_icmple(labels.hpWithinMax());

      loadHero(builder);

      loadHero(builder);

      builder.getfield(hero(), MAX_HP_FIELD, ConstantDescs.CD_short);
      builder.putfield(hero(), CURRENT_HP_FIELD, ConstantDescs.CD_short);
      builder.labelBinding(labels.hpWithinMax());
      builder.labelBinding(labels.nextHero()).iinc(3, 1).goto_(heroLoop);
      builder.labelBinding(labels.done()).return_();
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

  private record RecoveryLabels(
      Label notSleepingBag,
      Label notTent,
      Label recoveryLoop,
      Label nextHero,
      Label canRecover,
      Label chargeLoop,
      Label chargesDone,
      Label chargeWithinMax,
      Label hpWithinMax,
      Label done) {

    private static RecoveryLabels create(CodeBuilder builder) {
      return new RecoveryLabels(
          builder.newLabel(),
          builder.newLabel(),
          builder.newLabel(),
          builder.newLabel(),
          builder.newLabel(),
          builder.newLabel(),
          builder.newLabel(),
          builder.newLabel(),
          builder.newLabel(),
          builder.newLabel());
    }
  }
}
