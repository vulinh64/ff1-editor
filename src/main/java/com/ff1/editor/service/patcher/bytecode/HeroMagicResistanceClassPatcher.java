package com.ff1.editor.service.patcher.bytecode;

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
import java.lang.classfile.instruction.InvokeInstruction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Patches g.class so hero Intelligence plus Stamina reduces enemy-cast spell damage and normal
 * spell/effect success chance against party members.
 */
@Slf4j
public final class HeroMagicResistanceClassPatcher {

  public static final String ENTRY_NAME = HeroLevelGrowthClassPatcher.ENTRY_NAME;

  private static final String SPELL_EFFECT_METHOD = "a";
  private static final String SPELL_EFFECT_DESCRIPTOR = "(BII)I";
  private static final String BATTLE_CLASS_NAME = "g";
  private static final String SAVE_CLASS_NAME = "j";
  private static final String RANDOM_CLASS_NAME = "java/util/Random";
  private static final String RANDOM_NEXT_INT = "nextInt";
  private static final String RANDOM_NEXT_INT_DESCRIPTOR = "()I";
  private static final String SAVE_FIELD = "a";
  private static final String SAVE_FIELD_DESCRIPTOR = "Lk;";
  private static final String SPELL_DATA_DESCRIPTOR = "[[C";
  private static final String SAVE_DATA_CLASS_NAME = "k";
  private static final String HERO_ARRAY_FIELD = "a";
  private static final String HERO_ARRAY_DESCRIPTOR = "[La;";
  private static final String HERO_CLASS_NAME = "a";
  private static final String INTELLIGENCE_FIELD = "e";
  private static final String STAMINA_FIELD = "f";
  private static final int CHANCE_LOCAL = 3;
  private static final int RESULT_LOCAL = 10;
  private static final int RESIST_LOCAL = 11;
  private static final int MAX_RESIST_STAT = 200;
  private static final int CHANCE_DIVISOR = 1000;
  private static final int DAMAGE_NUMERATOR = 30;
  private static final int DAMAGE_DIVISOR = 20000;
  private static final int RANDOM_MODULUS = 201;
  private static final int DAMAGE_KIND = 1;
  private static final int STATUS_KIND = 3;
  private static final int SLEEP_STAGE_KIND = 4;
  private static final int MIND_BLAST_KIND = 5;
  private static final int RESISTANCE_CLEAR_KIND = 17;
  private static final int ORIGINAL_CHANCE_ROLLS = 6;

  private HeroMagicResistanceClassPatcher() {}

  public static PatcherState state(byte[] classBytes) {
    try {
      ClassModel model = ClassFile.of().parse(classBytes);
      int targetMethods = 0;
      int originalChanceRolls = 0;
      int chanceResistanceSites = 0;
      int damageResistanceSites = 0;
      for (MethodModel method : model.methods()) {
        if (!isSpellEffectMethod(method)) {
          continue;
        }
        targetMethods++;
        List<Instruction> instructions = instructions(method);
        originalChanceRolls += originalChanceRolls(instructions);
        chanceResistanceSites += pushCount(instructions, CHANCE_DIVISOR);
        damageResistanceSites += pushCount(instructions, DAMAGE_DIVISOR);
      }
      if (targetMethods == 1 && chanceResistanceSites == 0 && damageResistanceSites == 0) {
        return originalChanceRolls == ORIGINAL_CHANCE_ROLLS
            ? PatcherState.ORIGINAL
            : PatcherState.UNKNOWN;
      }
      if (targetMethods == 1 && chanceResistanceSites > 0 && damageResistanceSites > 0) {
        return PatcherState.PATCHED;
      }
      log.info(
          "Hero magic-resistance patch state unknown; targetMethods={}, originalChanceRolls={}, chanceResistanceSites={}, damageResistanceSites={}",
          targetMethods,
          originalChanceRolls,
          chanceResistanceSites,
          damageResistanceSites);
      return PatcherState.UNKNOWN;
    } catch (RuntimeException | LinkageError _) {
      return PatcherState.UNKNOWN;
    }
  }

  public static byte[] apply(byte[] classBytes) {
    PatcherState state = state(classBytes);
    log.info("Applying hero magic-resistance class patch; current state={}", state);
    if (state == PatcherState.PATCHED) {
      return classBytes.clone();
    }
    if (state != PatcherState.ORIGINAL) {
      throw new IllegalStateException(
          "Unsupported g.class layout for hero magic-resistance patch.");
    }

    ClassFile classFile = ClassFile.of();
    ClassModel model = classFile.parse(classBytes);
    PatchCounter counter = new PatchCounter();
    byte[] patched =
        classFile.transformClass(
            model,
            java.lang.classfile.ClassTransform.transformingMethodBodies(
                HeroMagicResistanceClassPatcher::isSpellEffectMethod,
                java.lang.classfile.CodeTransform.ofStateful(
                    () -> new HeroMagicResistanceCodeTransform(counter))));
    patched =
        CldcStackMapStripper.stripMethodStackMap(
            patched, SPELL_EFFECT_METHOD, SPELL_EFFECT_DESCRIPTOR);
    PatcherState patchedState = state(patched);
    if (counter.chanceSites() != ORIGINAL_CHANCE_ROLLS
        || counter.returnSites() == 0
        || patchedState != PatcherState.PATCHED) {
      throw new IllegalStateException(
          "Hero magic-resistance patch did not produce the expected g.class bytecode; chanceSites=%d, returnSites=%d, state=%s"
              .formatted(counter.chanceSites(), counter.returnSites(), patchedState));
    }
    log.info(
        "Hero magic-resistance class patch applied at {} chance sites and {} return sites",
        counter.chanceSites(),
        counter.returnSites());
    return patched;
  }

  private static boolean isSpellEffectMethod(MethodModel method) {
    return SPELL_EFFECT_METHOD.equals(method.methodName().stringValue())
        && SPELL_EFFECT_DESCRIPTOR.equals(method.methodType().stringValue());
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

  private static int originalChanceRolls(List<Instruction> instructions) {
    int matches = 0;
    for (int i = 0; i <= instructions.size() - 8; i++) {
      if (isChanceRollWindow(instructions.subList(i, i + 8))) {
        matches++;
      }
    }
    return matches;
  }

  private static boolean isChanceRollWindow(List<Instruction> instructions) {
    return instructions.size() == 8
        && instructions.get(0).opcode() == Opcode.ILOAD_3
        && isRandomRead(instructions.get(1))
        && isRandomNextInt(instructions.get(2))
        && instructions.get(3).opcode() == Opcode.ICONST_1
        && instructions.get(4).opcode() == Opcode.IUSHR
        && isPush(instructions.get(5), RANDOM_MODULUS)
        && instructions.get(6).opcode() == Opcode.IREM
        && instructions.get(7).opcode() == Opcode.IF_ICMPLT;
  }

  private static int pushCount(List<Instruction> instructions, int value) {
    int matches = 0;
    for (Instruction instruction : instructions) {
      if (isPush(instruction, value)) {
        matches++;
      }
    }
    return matches;
  }

  private static boolean isRandomRead(Instruction instruction) {
    return instruction instanceof FieldInstruction field
        && field.opcode() == Opcode.GETSTATIC
        && SAVE_CLASS_NAME.equals(field.owner().asInternalName())
        && SAVE_FIELD.equals(field.name().stringValue())
        && ("Ljava/util/Random;".equals(field.type().stringValue()));
  }

  private static boolean isRandomNextInt(Instruction instruction) {
    return instruction instanceof InvokeInstruction invoke
        && invoke.opcode() == Opcode.INVOKEVIRTUAL
        && RANDOM_CLASS_NAME.equals(invoke.owner().asInternalName())
        && RANDOM_NEXT_INT.equals(invoke.name().stringValue())
        && RANDOM_NEXT_INT_DESCRIPTOR.equals(invoke.type().stringValue());
  }

  private static boolean isPush(Instruction instruction, int value) {
    return instruction instanceof ConstantInstruction constant
        && constant.constantValue() instanceof Integer integer
        && integer == value;
  }

  private static final class PatchCounter {
    private int chanceSites;
    private int returnSites;

    void incrementChanceSites() {
      chanceSites++;
    }

    void incrementReturnSites() {
      returnSites++;
    }

    int chanceSites() {
      return chanceSites;
    }

    int returnSites() {
      return returnSites;
    }
  }

  private static final class HeroMagicResistanceCodeTransform
      implements java.lang.classfile.CodeTransform {
    private final PatchCounter counter;
    private final Deque<CodeElement> pending = new ArrayDeque<>();

    private HeroMagicResistanceCodeTransform(PatchCounter counter) {
      this.counter = counter;
    }

    @Override
    public void accept(CodeBuilder builder, CodeElement element) {
      pending.addLast(element);
      if (pending.size() < 8) {
        return;
      }
      if (pending.size() > 8) {
        CodeElement first = pending.removeFirst();
        if (first instanceof Instruction instruction && instruction.opcode() == Opcode.IRETURN) {
          emitResistedDamageReturn(builder);
          counter.incrementReturnSites();
          return;
        }
        builder.with(first);
      }
      if (isChanceRollWindow(pendingInstructions())) {
        emitResistedChance(builder);
        while (!pending.isEmpty()) {
          builder.with(pending.removeFirst());
        }
        counter.incrementChanceSites();
      }
    }

    @Override
    public void atEnd(CodeBuilder builder) {
      while (!pending.isEmpty()) {
        CodeElement first = pending.removeFirst();
        if (first instanceof Instruction instruction && instruction.opcode() == Opcode.IRETURN) {
          emitResistedDamageReturn(builder);
          counter.incrementReturnSites();
        } else {
          builder.with(first);
        }
      }
    }

    private List<Instruction> pendingInstructions() {
      List<Instruction> instructions = new ArrayList<>(8);
      for (CodeElement element : pending) {
        if (element instanceof Instruction instruction) {
          instructions.add(instruction);
        } else {
          return List.of();
        }
      }
      return instructions;
    }

    private static void emitResistedChance(CodeBuilder builder) {
      Label returnOriginal = builder.newLabel();
      Label actorInRange = builder.newLabel();
      Label effectKindMatches = builder.newLabel();
      Label resistUnderCap = builder.newLabel();

      builder
          .iload(0)
          .iconst_2()
          .iand()
          .ifne(returnOriginal)
          .getstatic(battle(), "C", java.lang.constant.ClassDesc.ofDescriptor("[I"))
          .getstatic(battle(), "Y", java.lang.constant.ConstantDescs.CD_int)
          .iaload()
          .iconst_4()
          .if_icmpge(actorInRange)
          .goto_(returnOriginal)
          .labelBinding(actorInRange)
          .iload(CHANCE_LOCAL)
          .ifle(returnOriginal);
      emitAffectedChanceKindGuard(builder, effectKindMatches, returnOriginal);
      builder
          .labelBinding(effectKindMatches)
          .getstatic(
              java.lang.constant.ClassDesc.of(SAVE_CLASS_NAME),
              SAVE_FIELD,
              java.lang.constant.ClassDesc.ofDescriptor(SAVE_FIELD_DESCRIPTOR))
          .getfield(
              java.lang.constant.ClassDesc.of(SAVE_DATA_CLASS_NAME),
              HERO_ARRAY_FIELD,
              java.lang.constant.ClassDesc.ofDescriptor(HERO_ARRAY_DESCRIPTOR))
          .iload(1)
          .aaload()
          .getfield(
              java.lang.constant.ClassDesc.of(HERO_CLASS_NAME),
              INTELLIGENCE_FIELD,
              java.lang.constant.ConstantDescs.CD_byte)
          .getstatic(
              java.lang.constant.ClassDesc.of(SAVE_CLASS_NAME),
              SAVE_FIELD,
              java.lang.constant.ClassDesc.ofDescriptor(SAVE_FIELD_DESCRIPTOR))
          .getfield(
              java.lang.constant.ClassDesc.of(SAVE_DATA_CLASS_NAME),
              HERO_ARRAY_FIELD,
              java.lang.constant.ClassDesc.ofDescriptor(HERO_ARRAY_DESCRIPTOR))
          .iload(1)
          .aaload()
          .getfield(
              java.lang.constant.ClassDesc.of(HERO_CLASS_NAME),
              STAMINA_FIELD,
              java.lang.constant.ConstantDescs.CD_byte)
          .iadd()
          .iconst_2()
          .iadd()
          .istore(RESIST_LOCAL)
          .iload(RESIST_LOCAL)
          .sipush(MAX_RESIST_STAT)
          .if_icmple(resistUnderCap)
          .sipush(MAX_RESIST_STAT)
          .istore(RESIST_LOCAL)
          .labelBinding(resistUnderCap)
          .iload(CHANCE_LOCAL)
          .iload(CHANCE_LOCAL)
          .iload(RESIST_LOCAL)
          .imul()
          .sipush(CHANCE_DIVISOR)
          .idiv()
          .isub()
          .istore(CHANCE_LOCAL)
          .labelBinding(returnOriginal);
    }

    private static void emitResistedDamageReturn(CodeBuilder builder) {
      Label returnOriginal = builder.newLabel();
      Label actorInRange = builder.newLabel();
      Label resistUnderCap = builder.newLabel();
      Label damageAtLeastOne = builder.newLabel();

      builder
          .istore(RESULT_LOCAL)
          .iload(0)
          .iconst_2()
          .iand()
          .ifne(returnOriginal)
          .iload(RESULT_LOCAL)
          .ifle(returnOriginal);
      emitKindGuard(builder, DAMAGE_KIND, actorInRange, returnOriginal);
      builder
          .labelBinding(actorInRange)
          .getstatic(battle(), "C", java.lang.constant.ClassDesc.ofDescriptor("[I"))
          .getstatic(battle(), "Y", java.lang.constant.ConstantDescs.CD_int)
          .iaload()
          .iconst_4()
          .if_icmplt(returnOriginal)
          .getstatic(
              java.lang.constant.ClassDesc.of(SAVE_CLASS_NAME),
              SAVE_FIELD,
              java.lang.constant.ClassDesc.ofDescriptor(SAVE_FIELD_DESCRIPTOR))
          .getfield(
              java.lang.constant.ClassDesc.of(SAVE_DATA_CLASS_NAME),
              HERO_ARRAY_FIELD,
              java.lang.constant.ClassDesc.ofDescriptor(HERO_ARRAY_DESCRIPTOR))
          .iload(1)
          .aaload()
          .getfield(
              java.lang.constant.ClassDesc.of(HERO_CLASS_NAME),
              INTELLIGENCE_FIELD,
              java.lang.constant.ConstantDescs.CD_byte)
          .getstatic(
              java.lang.constant.ClassDesc.of(SAVE_CLASS_NAME),
              SAVE_FIELD,
              java.lang.constant.ClassDesc.ofDescriptor(SAVE_FIELD_DESCRIPTOR))
          .getfield(
              java.lang.constant.ClassDesc.of(SAVE_DATA_CLASS_NAME),
              HERO_ARRAY_FIELD,
              java.lang.constant.ClassDesc.ofDescriptor(HERO_ARRAY_DESCRIPTOR))
          .iload(1)
          .aaload()
          .getfield(
              java.lang.constant.ClassDesc.of(HERO_CLASS_NAME),
              STAMINA_FIELD,
              java.lang.constant.ConstantDescs.CD_byte)
          .iadd()
          .iconst_2()
          .iadd()
          .istore(RESIST_LOCAL)
          .iload(RESIST_LOCAL)
          .sipush(MAX_RESIST_STAT)
          .if_icmple(resistUnderCap)
          .sipush(MAX_RESIST_STAT)
          .istore(RESIST_LOCAL)
          .labelBinding(resistUnderCap)
          .iload(RESULT_LOCAL)
          .iload(RESULT_LOCAL)
          .iload(RESIST_LOCAL)
          .imul()
          .bipush(DAMAGE_NUMERATOR)
          .imul()
          .sipush(DAMAGE_DIVISOR)
          .idiv()
          .isub()
          .istore(RESULT_LOCAL)
          .iload(RESULT_LOCAL)
          .iconst_1()
          .if_icmpge(damageAtLeastOne)
          .iconst_1()
          .ireturn()
          .labelBinding(damageAtLeastOne)
          .iload(RESULT_LOCAL)
          .ireturn()
          .labelBinding(returnOriginal)
          .iload(RESULT_LOCAL)
          .ireturn();
    }

    private static void emitAffectedChanceKindGuard(
        CodeBuilder builder, Label matched, Label unmatched) {
      emitKindGuard(builder, STATUS_KIND, matched, null);
      emitKindGuard(builder, SLEEP_STAGE_KIND, matched, null);
      emitKindGuard(builder, MIND_BLAST_KIND, matched, null);
      emitKindGuard(builder, RESISTANCE_CLEAR_KIND, matched, unmatched);
    }

    private static void emitKindGuard(
        CodeBuilder builder, int kind, Label matched, Label unmatched) {
      builder
          .getstatic(
              java.lang.constant.ClassDesc.of(SAVE_CLASS_NAME),
              SAVE_FIELD,
              java.lang.constant.ClassDesc.ofDescriptor(SPELL_DATA_DESCRIPTOR))
          .iload(2)
          .aaload()
          .iconst_4()
          .caload()
          .bipush(kind)
          .if_icmpeq(matched);
      if (unmatched != null) {
        builder.goto_(unmatched);
      }
    }

    private static java.lang.constant.ClassDesc battle() {
      return java.lang.constant.ClassDesc.of(BATTLE_CLASS_NAME);
    }
  }
}
