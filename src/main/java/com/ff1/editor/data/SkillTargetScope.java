package com.ff1.editor.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum SkillTargetScope implements LabeledValue {
  NONE("None"),
  SELF("Self"),
  SINGLE("Single target"),
  OMNI("Omni");

  private final String label;

  @Override
  public String toString() {
    return label;
  }
}
