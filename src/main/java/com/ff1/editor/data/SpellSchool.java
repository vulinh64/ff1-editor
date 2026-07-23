package com.ff1.editor.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum SpellSchool implements LabeledValue {
  WHITE("White"),
  BLACK("Black");

  private final String label;
}
