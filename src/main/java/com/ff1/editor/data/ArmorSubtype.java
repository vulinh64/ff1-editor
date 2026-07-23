package com.ff1.editor.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ArmorSubtype implements LabeledValue {
  BODY("Body"),
  SHIELD("Shield"),
  HELM("Helm"),
  GLOVES("Gloves");

  private final String label;
}
