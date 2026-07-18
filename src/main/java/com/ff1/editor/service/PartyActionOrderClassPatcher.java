package com.ff1.editor.service;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

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

  public enum State {
    ORIGINAL,
    PATCHED,
    UNKNOWN
  }

  private PartyActionOrderClassPatcher() {}

  public static State state(byte[] classBytes) {
    try {
      ClassModel model = ClassFile.of().parse(classBytes);
      List<Instruction> instructions = orderInstructions(model);
      if (instructions.isEmpty()) {
        log.info("Party action-order patch state unknown; order method not found");
        return State.UNKNOWN;
      }
      int randomCalls = randomNextIntCalls(instructions);
      int commandReads = commandReads(instructions);
      if (randomCalls == ORIGINAL_RANDOM_CALLS && commandReads == 0) {
        return State.ORIGINAL;
      }
      if (commandReads > 0) {
        return State.PATCHED;
      }
      log.info(
          "Party action-order patch state unknown; randomCalls={}, commandReads={}",
          randomCalls,
          commandReads);
      return State.UNKNOWN;
    } catch (RuntimeException | LinkageError _) {
      return State.UNKNOWN;
    }
  }

  public static byte[] apply(byte[] classBytes) {
    State state = state(classBytes);
    log.info("Applying party action-order class patch; current state={}", state);
    if (state == State.PATCHED) {
      return classBytes.clone();
    }
    if (state != State.ORIGINAL) {
      throw new IllegalStateException("Unsupported g.class layout for party action-order patch.");
    }

    ClassFile classFile = ClassFile.of();
    ClassModel model = classFile.parse(classBytes);
    PatchCounter counter = new PatchCounter();
    byte[] patched =
        classFile.transformClass(
            model,
            java.lang.classfile.ClassTransform.transformingMethodBodies(
                PartyActionOrderClassPatcher::isOrderMethod,
                java.lang.classfile.CodeTransform.ofStateful(
                    () -> new PartyActionOrderCodeTransform(counter))));
    patched = stripStaleStackMap(patched);
    State patchedState = state(patched);
    if (counter.count() != 1 || patchedState != State.PATCHED) {
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
        return instructions(method);
      }
    }
    return List.of();
  }

  private static boolean isOrderMethod(MethodModel method) {
    return ORDER_METHOD.equals(method.methodName().stringValue())
        && ORDER_DESCRIPTOR.equals(method.methodType().stringValue());
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

  private static final class PatchCounter {
    private int count;

    void increment() {
      count++;
    }

    int count() {
      return count;
    }
  }

  private static final class PartyActionOrderCodeTransform
      implements java.lang.classfile.CodeTransform {
    private final PatchCounter counter;
    private boolean emitted;

    private PartyActionOrderCodeTransform(PatchCounter counter) {
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
      Label customOrder = builder.newLabel();
      Label startOriginalFill = builder.newLabel();
      Label originalFillLoop = builder.newLabel();
      Label originalShuffleStart = builder.newLabel();
      Label originalShuffleLoop = builder.newLabel();
      Label originalReturn = builder.newLabel();
      Label commandTwo = builder.newLabel();
      Label commandZero = builder.newLabel();
      Label commandThree = builder.newLabel();
      Label commandOtherPass = builder.newLabel();
      Label commandOther = builder.newLabel();
      Label heroesDone = builder.newLabel();
      Label commandLoop = builder.newLabel();
      Label heroLoop = builder.newLabel();
      Label skipHero = builder.newLabel();
      Label addHero = builder.newLabel();
      Label nextCommand = builder.newLabel();
      Label enemyFillLoop = builder.newLabel();
      Label enemyShuffleStart = builder.newLabel();
      Label enemyShuffleLoop = builder.newLabel();

      builder.iconst_0().putstatic(battle(), QUEUE_INDEX_FIELD, ConstantDescs.CD_int);
      builder.getstatic(battle(), "b", ConstantDescs.CD_boolean).ifne(startOriginalFill);
      builder.goto_(customOrder);

      builder.labelBinding(startOriginalFill).iconst_0().istore(0);
      builder
          .labelBinding(originalFillLoop)
          .iload(0)
          .getstatic(battle(), QUEUE_FIELD, ClassDesc.ofDescriptor(QUEUE_DESCRIPTOR))
          .arraylength()
          .if_icmpge(originalShuffleStart)
          .getstatic(battle(), QUEUE_FIELD, ClassDesc.ofDescriptor(QUEUE_DESCRIPTOR))
          .iload(0)
          .iload(0)
          .iastore()
          .iinc(0, 1)
          .goto_(originalFillLoop);
      builder
          .labelBinding(originalShuffleStart)
          .iconst_0()
          .istore(0)
          .labelBinding(originalShuffleLoop)
          .iload(0)
          .bipush(17)
          .if_icmpge(originalReturn);
      emitRandomQueueIndex(builder, 2, 0);
      emitRandomQueueIndex(builder, 3, 0);
      emitQueueSwap(builder);
      builder.iinc(0, 1).goto_(originalShuffleLoop).labelBinding(originalReturn).return_();

      builder.labelBinding(customOrder).iconst_0().istore(1).iconst_1().istore(0);
      builder
          .labelBinding(commandLoop)
          .iload(0)
          .iconst_m1()
          .if_icmpeq(heroesDone)
          .iconst_0()
          .istore(2);
      builder
          .labelBinding(heroLoop)
          .iload(2)
          .iconst_4()
          .if_icmpge(nextCommand)
          .getstatic(battle(), COMMAND_FIELD, ClassDesc.ofDescriptor(COMMAND_DESCRIPTOR))
          .iload(2)
          .aaload()
          .iconst_0()
          .iaload()
          .istore(3)
          .iload(0)
          .iconst_4()
          .if_icmpeq(commandOther)
          .iload(3)
          .iload(0)
          .if_icmpeq(addHero)
          .goto_(skipHero);
      builder
          .labelBinding(commandOther)
          .iload(3)
          .iconst_1()
          .if_icmpeq(skipHero)
          .iload(3)
          .iconst_2()
          .if_icmpeq(skipHero)
          .iload(3)
          .iconst_0()
          .if_icmpeq(skipHero)
          .iload(3)
          .iconst_3()
          .if_icmpeq(skipHero);
      builder
          .labelBinding(addHero)
          .getstatic(battle(), QUEUE_FIELD, ClassDesc.ofDescriptor(QUEUE_DESCRIPTOR))
          .iload(1)
          .iinc(1, 1)
          .iload(2)
          .iastore();
      builder.labelBinding(skipHero).iinc(2, 1).goto_(heroLoop);
      builder
          .labelBinding(nextCommand)
          .iload(0)
          .iconst_1()
          .if_icmpeq(commandTwo)
          .iload(0)
          .iconst_2()
          .if_icmpeq(commandZero)
          .iload(0)
          .iconst_0()
          .if_icmpeq(commandThree)
          .iload(0)
          .iconst_3()
          .if_icmpeq(commandOtherPass)
          .iconst_m1()
          .istore(0)
          .goto_(commandLoop);
      builder.labelBinding(commandTwo).iconst_2().istore(0).goto_(commandLoop);
      builder.labelBinding(commandZero).iconst_0().istore(0).goto_(commandLoop);
      builder.labelBinding(commandThree).iconst_3().istore(0).goto_(commandLoop);
      builder.labelBinding(commandOtherPass).iconst_4().istore(0).goto_(commandLoop);

      builder
          .labelBinding(heroesDone)
          .iconst_4()
          .istore(2)
          .labelBinding(enemyFillLoop)
          .iload(2)
          .getstatic(battle(), QUEUE_FIELD, ClassDesc.ofDescriptor(QUEUE_DESCRIPTOR))
          .arraylength()
          .if_icmpge(enemyShuffleStart)
          .getstatic(battle(), QUEUE_FIELD, ClassDesc.ofDescriptor(QUEUE_DESCRIPTOR))
          .iload(1)
          .iinc(1, 1)
          .iload(2)
          .iastore()
          .iinc(2, 1)
          .goto_(enemyFillLoop);
      builder
          .labelBinding(enemyShuffleStart)
          .iconst_0()
          .istore(0)
          .labelBinding(enemyShuffleLoop)
          .iload(0)
          .bipush(17)
          .if_icmpge(originalReturn);
      emitRandomQueueIndex(builder, 2, 4);
      emitRandomQueueIndex(builder, 3, 4);
      emitQueueSwap(builder);
      builder.iinc(0, 1).goto_(enemyShuffleLoop);
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
}
