package com.ff1.editor.utils;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;

public final class CldcStackMapStripper {

  private static final String STACK_MAP_ATTRIBUTE = "StackMap";

  private CldcStackMapStripper() {}

  public static byte[] stripMethodStackMap(
      byte[] classBytes, String methodName, String methodDescriptor) {
    ClassFile classFile = ClassFile.of();
    ClassModel model = classFile.parse(classBytes);
    return classFile.transformClass(
        model,
        java.lang.classfile.ClassTransform.transformingMethodBodies(
            method ->
                methodName.equals(method.methodName().stringValue())
                    && methodDescriptor.equals(method.methodType().stringValue()),
            java.lang.classfile.CodeTransform.ofStateful(StackMapStripTransform::new)));
  }

  private static final class StackMapStripTransform implements java.lang.classfile.CodeTransform {

    @Override
    public void accept(CodeBuilder builder, CodeElement element) {
      if (element instanceof Attribute<?> attribute
          && STACK_MAP_ATTRIBUTE.equals(attribute.attributeName().stringValue())) {
        return;
      }
      builder.with(element);
    }
  }
}
