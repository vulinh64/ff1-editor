package com.ff1.editor.service.patcher.bytecode;

import com.ff1.editor.service.*;
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

/**
 * Patches g.class so player-cast Cure and Heal style effects scale with the acting hero
 * Intelligence stat.
 */
@Slf4j
public final class IntelligenceSpellHealingClassPatcher {

  private static final int RESULT_LOCAL = 10;
  private static final int ACTOR_LOCAL = 11;
  private static final int HEALING_KIND = 7;
  private static final int INT_DIVISOR = 200;
  private static final String SPELL_EFFECT_METHOD = "a";
  private static final String SPELL_EFFECT_DESCRIPTOR = "(BII)I";
  private static final String BATTLE_CLASS_NAME = "g";
  private static final String SAVE_CLASS_NAME = "j";
  private static final String SAVE_FIELD = "a";
  private static final String SAVE_FIELD_DESCRIPTOR = "Lk;";
  private static final String SPELL_DATA_FIELD_DESCRIPTOR = "[[C";
  private static final String SAVE_DATA_CLASS_NAME = "k";
  private static final String HERO_ARRAY_FIELD = "a";
  private static final String HERO_ARRAY_FIELD_DESCRIPTOR = "[La;";
  private static final String HERO_CLASS_NAME = "a";
  private static final String INTELLIGENCE_FIELD = "e";
  private static final String INTELLIGENCE_DESCRIPTOR = "B";

  private IntelligenceSpellHealingClassPatcher() {}

  public static PatcherState state(byte[] data) {
    try {
      ClassModel model = ClassFile.of().parse(data);
      int targetMethods = 0;
      int healingScaleGuards = 0;
      int intelligenceReads = 0;
      for (MethodModel method : model.methods()) {
        if (!isSpellEffectMethod(method)) {
          continue;
        }
        targetMethods++;
        healingScaleGuards += healingScaleGuards(method);
        intelligenceReads += intelligenceReads(method);
      }
      if (targetMethods == 1 && healingScaleGuards == 0 && intelligenceReads == 0) {
        return PatcherState.ORIGINAL;
      }
      if (targetMethods == 1 && healingScaleGuards > 0 && intelligenceReads > 0) {
        return PatcherState.PATCHED;
      }
      log.info(
          "INT spell-healing class patch state unknown; targetMethods={}, healingScaleGuards={}, intelligenceReads={}",
          targetMethods,
          healingScaleGuards,
          intelligenceReads);
      return PatcherState.UNKNOWN;
    } catch (RuntimeException | LinkageError _) {
      return PatcherState.UNKNOWN;
    }
  }

  public static byte[] apply(byte[] data) {
    PatcherState state = state(data);
    log.info("Applying INT spell-healing class patch; current state={}", state);
    if (state == PatcherState.PATCHED) {
      return data.clone();
    }
    if (state != PatcherState.ORIGINAL) {
      throw new IllegalStateException("Unsupported g.class layout for INT spell-healing patch.");
    }

    ClassFile classFile = ClassFile.of();
    ClassModel model = classFile.parse(data);
    PatchCounter counter = new PatchCounter();
    byte[] patched =
        classFile.transformClass(
            model,
            java.lang.classfile.ClassTransform.transformingMethodBodies(
                IntelligenceSpellHealingClassPatcher::isSpellEffectMethod,
                java.lang.classfile.CodeTransform.ofStateful(
                    () -> new IntelligenceHealingCodeTransform(counter))));
    PatcherState patchedState = state(patched);
    if (counter.count() == 0 || patchedState != PatcherState.PATCHED) {
      throw new IllegalStateException(
          "INT spell-healing patch did not produce the expected g.class bytecode; counter=%d, state=%s"
              .formatted(counter.count(), patchedState));
    }
    log.info("INT spell-healing class patch applied at {} return sites", counter.count());
    return patched;
  }

  private static boolean isSpellEffectMethod(MethodModel method) {
    return SPELL_EFFECT_METHOD.equals(method.methodName().stringValue())
        && SPELL_EFFECT_DESCRIPTOR.equals(method.methodType().stringValue());
  }

  private static int healingScaleGuards(MethodModel method) {
    int matches = 0;
    List<Instruction> instructions = instructions(method);
    for (int i = 0; i + 6 < instructions.size(); i++) {
      if (isHealingScaleGuard(instructions, i)) {
        matches++;
      }
    }
    return matches;
  }

  private static boolean isHealingScaleGuard(List<Instruction> instructions, int offset) {
    return isSpellDataRead(instructions.get(offset))
        && instructions.get(offset + 1).opcode() == Opcode.ILOAD_2
        && instructions.get(offset + 2).opcode() == Opcode.AALOAD
        && instructions.get(offset + 3).opcode() == Opcode.ICONST_4
        && instructions.get(offset + 4).opcode() == Opcode.CALOAD
        && isPush(instructions.get(offset + 5), HEALING_KIND)
        && instructions.get(offset + 6).opcode() == Opcode.IF_ICMPNE;
  }

  private static int intelligenceReads(MethodModel method) {
    int matches = 0;
    for (Instruction instruction : instructions(method)) {
      if (isIntelligenceRead(instruction)) {
        matches++;
      }
    }
    return matches;
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

  private static boolean isSpellDataRead(Instruction instruction) {
    return instruction instanceof FieldInstruction field
        && field.opcode() == Opcode.GETSTATIC
        && SAVE_CLASS_NAME.equals(field.owner().asInternalName())
        && SAVE_FIELD.equals(field.name().stringValue())
        && SPELL_DATA_FIELD_DESCRIPTOR.equals(field.type().stringValue());
  }

  private static boolean isIntelligenceRead(Instruction instruction) {
    return instruction instanceof FieldInstruction field
        && field.opcode() == Opcode.GETFIELD
        && HERO_CLASS_NAME.equals(field.owner().asInternalName())
        && INTELLIGENCE_FIELD.equals(field.name().stringValue())
        && INTELLIGENCE_DESCRIPTOR.equals(field.type().stringValue());
  }

  private static boolean isPush(Instruction instruction, int value) {
    return instruction instanceof ConstantInstruction constant
        && constant.constantValue() instanceof Integer integer
        && integer == value;
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

  private static final class IntelligenceHealingCodeTransform
      implements java.lang.classfile.CodeTransform {
    private final PatchCounter counter;

    private IntelligenceHealingCodeTransform(PatchCounter counter) {
      this.counter = counter;
    }

    @Override
    public void accept(CodeBuilder builder, CodeElement element) {
      if (element instanceof Instruction instruction && instruction.opcode() == Opcode.IRETURN) {
        emitScaledReturn(builder);
        counter.increment();
        return;
      }
      builder.with(element);
    }

    private static void emitScaledReturn(CodeBuilder builder) {
      Label returnOriginal = builder.newLabel();
      Label actorInRange = builder.newLabel();

      builder
          .istore(RESULT_LOCAL)
          .iload(0)
          .iconst_2()
          .iand()
          .ifne(returnOriginal)
          .iload(RESULT_LOCAL)
          .ifge(returnOriginal)
          .getstatic(
              ClassDesc.of(SAVE_CLASS_NAME),
              SAVE_FIELD,
              ClassDesc.ofDescriptor(SPELL_DATA_FIELD_DESCRIPTOR))
          .iload(2)
          .aaload()
          .iconst_4()
          .caload()
          .bipush(HEALING_KIND)
          .if_icmpne(returnOriginal)
          .getstatic(ClassDesc.of(BATTLE_CLASS_NAME), "C", ClassDesc.ofDescriptor("[I"))
          .getstatic(ClassDesc.of(BATTLE_CLASS_NAME), "Y", ConstantDescs.CD_int)
          .iaload()
          .istore(ACTOR_LOCAL)
          .iload(ACTOR_LOCAL)
          .iflt(returnOriginal)
          .iload(ACTOR_LOCAL)
          .iconst_4()
          .if_icmplt(actorInRange)
          .goto_(returnOriginal)
          .labelBinding(actorInRange)
          .iload(RESULT_LOCAL)
          .iload(RESULT_LOCAL)
          .ineg()
          .getstatic(
              ClassDesc.of(SAVE_CLASS_NAME),
              SAVE_FIELD,
              ClassDesc.ofDescriptor(SAVE_FIELD_DESCRIPTOR))
          .getfield(
              ClassDesc.of(SAVE_DATA_CLASS_NAME),
              HERO_ARRAY_FIELD,
              ClassDesc.ofDescriptor(HERO_ARRAY_FIELD_DESCRIPTOR))
          .iload(ACTOR_LOCAL)
          .aaload()
          .getfield(ClassDesc.of(HERO_CLASS_NAME), INTELLIGENCE_FIELD, ConstantDescs.CD_byte)
          .imul()
          .sipush(INT_DIVISOR)
          .idiv()
          .isub()
          .istore(RESULT_LOCAL)
          .labelBinding(returnOriginal)
          .iload(RESULT_LOCAL)
          .ireturn();
    }
  }
}
