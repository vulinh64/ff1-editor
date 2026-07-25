package com.ff1.editor.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum SkillTarget implements LabeledValue {
  NONE("None"),
  ENEMY("Enemy"),
  SELF("Self"),
  PARTY("Party");

  private final String label;

  @Override
  public String toString() {
    return label;
  }
}
