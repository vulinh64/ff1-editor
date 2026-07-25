package com.ff1.editor.service.patcher.bytecode;

import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.MethodModel;
import java.util.ArrayList;
import java.util.List;

final class BytecodeInstructions {

  private BytecodeInstructions() {}

  static List<Instruction> instructions(MethodModel method) {
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
}
