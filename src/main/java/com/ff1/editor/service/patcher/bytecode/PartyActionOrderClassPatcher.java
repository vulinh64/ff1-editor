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
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Patches g.class battle queue creation so party commands resolve before enemy actions in normal
 * and preemptive turns.
 */
@Slf4j
public final class PartyActionOrderClassPatcher {

  public static final String ENTRY_NAME = HeroLevelGrowthClassPatcher.ENTRY_NAME;

  private static final String ORDER_METHOD = "G";
  private static final String ORDER_DESCRIPTOR = "()V";
  private static final String BATTLE_CLASS_NAME = "g";
  private static final String RANDOM_CLASS_NAME = "java/util/Random";
  private static final String RANDOM_NEXT_INT = "nextInt";
  private static final String RANDOM_NEXT_INT_DESCRIPTOR = "()I";
  private static final String SAVE_CLASS_NAME = "j";
  private static final String RANDOM_FIELD = "a";
  private static final String RANDOM_DESCRIPTOR = "Ljava/util/Random;";
  private static final String QUEUE_FIELD = "C";
  private static final String QUEUE_DESCRIPTOR = "[I";
  private static final String QUEUE_INDEX_FIELD = "Y";
  private static final String COMMAND_FIELD = "e";
  private static final String COMMAND_DESCRIPTOR = "[[I";
  private static final int ORIGINAL_RANDOM_CALLS = 2;

  private PartyActionOrderClassPatcher() {}

  public static PatcherState state(byte[] classBytes) {
    try {
      ClassModel model = ClassFile.of().parse(classBytes);

      List<Instruction> instructions = orderInstructions(model);

      if (instructions.isEmpty()) {
        log.info("Party action-order patch state unknown; order method not found");

        return PatcherState.UNKNOWN;
      }

      int randomCalls = randomNextIntCalls(instructions);
      int commandReads = commandReads(instructions);

      if (randomCalls == ORIGINAL_RANDOM_CALLS && commandReads == 0) {
        return PatcherState.ORIGINAL;
      }

      if (commandReads > 0) {
        return PatcherState.PATCHED;
      }

      log.info(
          "Party action-order patch state unknown; randomCalls={}, commandReads={}",
          randomCalls,
          commandReads);

      return PatcherState.UNKNOWN;
    } catch (RuntimeException | LinkageError e) {
      log.warn("Party battle order patcher state error", e);

      return PatcherState.UNKNOWN;
    }
  }

  public static byte[] apply(byte[] classBytes) {
    PatcherState state = state(classBytes);

    log.info("Applying party action-order class patch; current state={}", state);

    if (state == PatcherState.PATCHED) {
      return classBytes.clone();
    }

    if (state != PatcherState.ORIGINAL) {
      throw new IllegalStateException("Unsupported g.class layout for party action-order patch.");
    }

    ClassFile classFile = ClassFile.of();

    ClassModel model = classFile.parse(classBytes);

    PatchSiteCounter counter = PatchSiteCounter.create();

    byte[] patched =
        classFile.transformClass(
            model,
            ClassTransform.transformingMethodBodies(
                PartyActionOrderClassPatcher::isOrderMethod,
                CodeTransform.ofStateful(() -> new PartyActionOrderCodeTransform(counter))));

    patched = stripStaleStackMap(patched);

    PatcherState patchedState = state(patched);

    if (counter.count() != 1 || patchedState != PatcherState.PATCHED) {
      throw new IllegalStateException(
          "Expected one action-order method in %s but patched %d; state=%s."
              .formatted(ENTRY_NAME, counter.count(), patchedState));
    }

    log.info("Party action-order class patch applied");

    return patched;
  }

  private static byte[] stripStaleStackMap(byte[] classBytes) {
    return CldcStackMapStripper.stripMethodStackMap(classBytes, ORDER_METHOD, ORDER_DESCRIPTOR);
  }

  private static List<Instruction> orderInstructions(ClassModel model) {
    for (MethodModel method : model.methods()) {
      if (isOrderMethod(method)) {
        return BytecodeInstructions.instructions(method);
      }
    }

    return Collections.emptyList();
  }

  private static boolean isOrderMethod(MethodModel method) {
    return ORDER_METHOD.equals(method.methodName().stringValue())
        && ORDER_DESCRIPTOR.equals(method.methodType().stringValue());
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

  private static int commandReads(List<Instruction> instructions) {
    int matches = 0;

    for (Instruction instruction : instructions) {
      if (isCommandRead(instruction)) {
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

  private static boolean isCommandRead(Instruction instruction) {
    return instruction instanceof FieldInstruction field
        && field.opcode() == Opcode.GETSTATIC
        && BATTLE_CLASS_NAME.equals(field.owner().asInternalName())
        && COMMAND_FIELD.equals(field.name().stringValue())
        && COMMAND_DESCRIPTOR.equals(field.type().stringValue());
  }

  private static final class PartyActionOrderCodeTransform implements CodeTransform {

    private final PatchSiteCounter counter;
    private boolean emitted;

    private PartyActionOrderCodeTransform(PatchSiteCounter counter) {
      this.counter = counter;
    }

    @Override
    public void accept(CodeBuilder builder, CodeElement element) {
      if (emitted) {
        return;
      }

      if (element instanceof Instruction) {
        emitReplacement(builder);

        emitted = true;

        counter.increment();
      } else {
        builder.with(element);
      }
    }

    private static void emitReplacement(CodeBuilder builder) {
      PartyOrderLabels labels = PartyOrderLabels.create(builder);

      builder.iconst_0().putstatic(battle(), QUEUE_INDEX_FIELD, ConstantDescs.CD_int);

      builder.getstatic(battle(), "b", ConstantDescs.CD_boolean).ifne(labels.startOriginalFill());

      builder.goto_(labels.customOrder());

      builder.labelBinding(labels.startOriginalFill()).iconst_0().istore(0);

      builder
          .labelBinding(labels.originalFillLoop())
          .iload(0)
          .getstatic(battle(), QUEUE_FIELD, ClassDesc.ofDescriptor(QUEUE_DESCRIPTOR))
          .arraylength()
          .if_icmpge(labels.originalShuffleStart())
          .getstatic(battle(), QUEUE_FIELD, ClassDesc.ofDescriptor(QUEUE_DESCRIPTOR))
          .iload(0)
          .iload(0)
          .iastore()
          .iinc(0, 1)
          .goto_(labels.originalFillLoop());

      builder
          .labelBinding(labels.originalShuffleStart())
          .iconst_0()
          .istore(0)
          .labelBinding(labels.originalShuffleLoop())
          .iload(0)
          .bipush(17)
          .if_icmpge(labels.originalReturn());

      emitRandomQueueIndex(builder, 2, 0);

      emitRandomQueueIndex(builder, 3, 0);

      emitQueueSwap(builder);

      builder
          .iinc(0, 1)
          .goto_(labels.originalShuffleLoop())
          .labelBinding(labels.originalReturn())
          .return_();

      builder.labelBinding(labels.customOrder()).iconst_0().istore(1).iconst_1().istore(0);

      builder
          .labelBinding(labels.commandLoop())
          .iload(0)
          .iconst_m1()
          .if_icmpeq(labels.heroesDone())
          .iconst_0()
          .istore(2);

      builder
          .labelBinding(labels.heroLoop())
          .iload(2)
          .iconst_4()
          .if_icmpge(labels.nextCommand())
          .getstatic(battle(), COMMAND_FIELD, ClassDesc.ofDescriptor(COMMAND_DESCRIPTOR))
          .iload(2)
          .aaload()
          .iconst_0()
          .iaload()
          .istore(3)
          .iload(0)
          .iconst_4()
          .if_icmpeq(labels.commandOther())
          .iload(3)
          .iload(0)
          .if_icmpeq(labels.addHero())
          .goto_(labels.skipHero());

      builder
          .labelBinding(labels.commandOther())
          .iload(3)
          .iconst_1()
          .if_icmpeq(labels.skipHero())
          .iload(3)
          .iconst_2()
          .if_icmpeq(labels.skipHero())
          .iload(3)
          .iconst_0()
          .if_icmpeq(labels.skipHero())
          .iload(3)
          .iconst_3()
          .if_icmpeq(labels.skipHero());
      builder
          .labelBinding(labels.addHero())
          .getstatic(battle(), QUEUE_FIELD, ClassDesc.ofDescriptor(QUEUE_DESCRIPTOR))
          .iload(1)
          .iinc(1, 1)
          .iload(2)
          .iastore();

      builder.labelBinding(labels.skipHero()).iinc(2, 1).goto_(labels.heroLoop());

      builder
          .labelBinding(labels.nextCommand())
          .iload(0)
          .iconst_1()
          .if_icmpeq(labels.commandTwo())
          .iload(0)
          .iconst_2()
          .if_icmpeq(labels.commandZero())
          .iload(0)
          .iconst_0()
          .if_icmpeq(labels.commandThree())
          .iload(0)
          .iconst_3()
          .if_icmpeq(labels.commandOtherPass())
          .iconst_m1()
          .istore(0)
          .goto_(labels.commandLoop());

      builder.labelBinding(labels.commandTwo()).iconst_2().istore(0).goto_(labels.commandLoop());

      builder.labelBinding(labels.commandZero()).iconst_0().istore(0).goto_(labels.commandLoop());

      builder.labelBinding(labels.commandThree()).iconst_3().istore(0).goto_(labels.commandLoop());

      builder
          .labelBinding(labels.commandOtherPass())
          .iconst_4()
          .istore(0)
          .goto_(labels.commandLoop());

      builder
          .labelBinding(labels.heroesDone())
          .iconst_4()
          .istore(2)
          .labelBinding(labels.enemyFillLoop())
          .iload(2)
          .getstatic(battle(), QUEUE_FIELD, ClassDesc.ofDescriptor(QUEUE_DESCRIPTOR))
          .arraylength()
          .if_icmpge(labels.enemyShuffleStart())
          .getstatic(battle(), QUEUE_FIELD, ClassDesc.ofDescriptor(QUEUE_DESCRIPTOR))
          .iload(1)
          .iinc(1, 1)
          .iload(2)
          .iastore()
          .iinc(2, 1)
          .goto_(labels.enemyFillLoop());

      builder
          .labelBinding(labels.enemyShuffleStart())
          .iconst_0()
          .istore(0)
          .labelBinding(labels.enemyShuffleLoop())
          .iload(0)
          .bipush(17)
          .if_icmpge(labels.originalReturn());

      emitRandomQueueIndex(builder, 2, 4);

      emitRandomQueueIndex(builder, 3, 4);

      emitQueueSwap(builder);

      builder.iinc(0, 1).goto_(labels.enemyShuffleLoop());
    }

    private static void emitRandomQueueIndex(CodeBuilder builder, int targetLocal, int base) {
      builder
          .getstatic(
              ClassDesc.of(SAVE_CLASS_NAME),
              RANDOM_FIELD,
              ClassDesc.ofDescriptor(RANDOM_DESCRIPTOR))
          .invokevirtual(
              ClassDesc.ofDescriptor("L" + RANDOM_CLASS_NAME + ";"),
              RANDOM_NEXT_INT,
              java.lang.constant.MethodTypeDesc.of(ConstantDescs.CD_int))
          .iconst_1()
          .iushr()
          .getstatic(battle(), QUEUE_FIELD, ClassDesc.ofDescriptor(QUEUE_DESCRIPTOR))
          .arraylength();

      if (base > 0) {
        builder.iconst_4().isub();
      }

      builder.irem();

      if (base > 0) {
        builder.iconst_4().iadd();
      }

      builder.istore(targetLocal);
    }

    private static void emitQueueSwap(CodeBuilder builder) {
      builder
          .getstatic(battle(), QUEUE_FIELD, ClassDesc.ofDescriptor(QUEUE_DESCRIPTOR))
          .iload(2)
          .iaload()
          .istore(1)
          .getstatic(battle(), QUEUE_FIELD, ClassDesc.ofDescriptor(QUEUE_DESCRIPTOR))
          .iload(2)
          .getstatic(battle(), QUEUE_FIELD, ClassDesc.ofDescriptor(QUEUE_DESCRIPTOR))
          .iload(3)
          .iaload()
          .iastore()
          .getstatic(battle(), QUEUE_FIELD, ClassDesc.ofDescriptor(QUEUE_DESCRIPTOR))
          .iload(3)
          .iload(1)
          .iastore();
    }

    private static ClassDesc battle() {
      return ClassDesc.of(BATTLE_CLASS_NAME);
    }
  }

  private record PartyOrderLabels(
      Label customOrder,
      Label startOriginalFill,
      Label originalFillLoop,
      Label originalShuffleStart,
      Label originalShuffleLoop,
      Label originalReturn,
      Label commandTwo,
      Label commandZero,
      Label commandThree,
      Label commandOtherPass,
      Label commandOther,
      Label heroesDone,
      Label commandLoop,
      Label heroLoop,
      Label skipHero,
      Label addHero,
      Label nextCommand,
      Label enemyFillLoop,
      Label enemyShuffleStart,
      Label enemyShuffleLoop) {

    private static PartyOrderLabels create(CodeBuilder builder) {
      return new PartyOrderLabels(
          builder.newLabel(),
          builder.newLabel(),
          builder.newLabel(),
          builder.newLabel(),
          builder.newLabel(),
          builder.newLabel(),
          builder.newLabel(),
          builder.newLabel(),
          builder.newLabel(),
          builder.newLabel(),
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
