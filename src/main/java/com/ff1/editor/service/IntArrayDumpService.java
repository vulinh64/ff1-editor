package com.ff1.editor.service;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.classfile.instruction.ArrayStoreInstruction;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.NewMultiArrayInstruction;
import java.lang.classfile.instruction.NewPrimitiveArrayInstruction;
import java.lang.classfile.instruction.NewReferenceArrayInstruction;
import java.lang.reflect.AccessFlag;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class IntArrayDumpService {

  private static final String CLASS_INITIALIZER = "<clinit>";
  private static final String CLASS_INITIALIZER_DESCRIPTOR = "()V";
  private static final String INT_ARRAY_DESCRIPTOR = "[I";
  private static final String INT_MATRIX_DESCRIPTOR = "[[I";

  public void dump(Path jarPath, String className) throws Exception {
    byte[] classBytes = readClassBytes(jarPath, className);
    ClassModel model = ClassFile.of().parse(classBytes);
    Map<FieldKey, Object> values = staticInitializerValues(model);
    model.fields().stream()
        .filter(field -> field.flags().has(AccessFlag.STATIC))
        .forEach(
            field -> {
              String name = field.fieldName().stringValue();
              String descriptor = field.fieldType().stringValue();
              Object value = values.get(new FieldKey(name, descriptor));
              if (value instanceof IntArrayValue(int[] val)) {
                log.info("{} = {}", name, Arrays.toString(val));
              } else if (value instanceof IntMatrixValue(int[][] val)) {
                log.info("{} = {}", name, Arrays.deepToString(val));
              } else if (INT_ARRAY_DESCRIPTOR.equals(descriptor)
                  || INT_MATRIX_DESCRIPTOR.equals(descriptor)
                  || descriptor.startsWith("[")) {
                log.info("{} = {} (not a literal <clinit> value)", name, descriptor);
              }
            });
  }

  private static byte[] readClassBytes(Path jarPath, String className) throws Exception {
    String entryName = className.replace('.', '/') + ".class";
    try (JarFile jar = new JarFile(jarPath.toFile())) {
      var entry = jar.getJarEntry(entryName);
      if (entry == null) {
        throw new IllegalArgumentException("Class entry not found in jar: " + entryName);
      }
      try (var input = jar.getInputStream(entry)) {
        return input.readAllBytes();
      }
    }
  }

  private static Map<FieldKey, Object> staticInitializerValues(ClassModel model) {
    for (MethodModel method : model.methods()) {
      if (CLASS_INITIALIZER.equals(method.methodName().stringValue())
          && CLASS_INITIALIZER_DESCRIPTOR.equals(method.methodType().stringValue())) {
        return interpretStaticInitializer(method);
      }
    }
    return Map.of();
  }

  private static Map<FieldKey, Object> interpretStaticInitializer(MethodModel method) {
    Map<FieldKey, Object> values = new LinkedHashMap<>();
    List<Object> stack = new ArrayList<>();
    for (CodeElement element : method.code().orElseThrow()) {
      if (element instanceof Instruction instruction) {
        accept(values, stack, instruction);
      }
    }
    return values;
  }

  private static void accept(
      Map<FieldKey, Object> values, List<Object> stack, Instruction instruction) {
    if (instruction instanceof ConstantInstruction constant) {
      stack.add(constant.constantValue());
      return;
    }
    if (instruction instanceof NewPrimitiveArrayInstruction newArray
        && newArray.typeKind() == TypeKind.INT) {
      stack.add(new IntArrayValue(popInt(stack)));
      return;
    }
    if (instruction instanceof NewReferenceArrayInstruction newArray
        && INT_ARRAY_DESCRIPTOR.equals(newArray.componentType().asInternalName())) {
      stack.add(new IntMatrixValue(popInt(stack)));
      return;
    }
    if (instruction instanceof NewMultiArrayInstruction newArray
        && INT_MATRIX_DESCRIPTOR.equals(newArray.arrayType().asInternalName())
        && newArray.dimensions() == 2) {
      int columns = popInt(stack);
      int rows = popInt(stack);
      stack.add(new IntMatrixValue(rows, columns));
      return;
    }
    if (instruction instanceof ArrayStoreInstruction arrayStore) {
      arrayStore(stack, arrayStore.typeKind());
      return;
    }
    if (instruction instanceof FieldInstruction field) {
      field(values, stack, field);
      return;
    }
    switch (instruction.opcode()) {
      case DUP -> {
        if (!stack.isEmpty()) {
          stack.add(stack.getLast());
        }
      }
      case ACONST_NULL -> stack.add(null);
      case POP -> pop(stack);
      default -> {
        // Ignore instructions that do not affect the literal array shapes this
        // developer dump command understands.
      }
    }
  }

  private static void arrayStore(List<Object> stack, TypeKind typeKind) {
    Object value = pop(stack);
    int index = popInt(stack);
    Object array = pop(stack);
    if (typeKind == TypeKind.INT
        && array instanceof IntArrayValue(int[] values)
        && index >= 0
        && index < values.length) {
      values[index] = intValue(value);
    } else if (typeKind == TypeKind.REFERENCE
        && array instanceof IntMatrixValue(int[][] intMatrix)
        && value instanceof IntArrayValue(int[] intArray)
        && index >= 0
        && index < intMatrix.length) {
      intMatrix[index] = intArray;
    }
  }

  private static void field(
      Map<FieldKey, Object> values, List<Object> stack, FieldInstruction field) {
    String name = field.name().stringValue();
    String descriptor = field.type().stringValue();
    FieldKey key = new FieldKey(name, descriptor);
    if (field.opcode() == Opcode.PUTSTATIC) {
      Object value = pop(stack);
      if ((INT_ARRAY_DESCRIPTOR.equals(descriptor) && value instanceof IntArrayValue)
          || (INT_MATRIX_DESCRIPTOR.equals(descriptor) && value instanceof IntMatrixValue)) {
        values.put(key, value);
      }
    } else if (field.opcode() == Opcode.GETSTATIC) {
      stack.add(values.getOrDefault(key, UnknownValue.INSTANCE));
    }
  }

  private static int popInt(List<Object> stack) {
    return intValue(pop(stack));
  }

  private static int intValue(Object value) {
    if (value instanceof Integer integer) {
      return integer;
    }
    return 0;
  }

  private static Object pop(List<Object> stack) {
    if (stack.isEmpty()) {
      return UnknownValue.INSTANCE;
    }
    return stack.removeLast();
  }

  private record IntArrayValue(int[] values) {
    private IntArrayValue(int length) {
      this(new int[length]);
    }
  }

  private record IntMatrixValue(int[][] values) {
    private IntMatrixValue(int rows) {
      this(new int[rows][]);
    }

    private IntMatrixValue(int rows, int columns) {
      this(new int[rows][columns]);
    }
  }

  private record FieldKey(String name, String descriptor) {}

  private enum UnknownValue {
    INSTANCE
  }
}
