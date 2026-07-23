package com.ff1.editor.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum SkillInvokerKind implements LabeledValue {
  LEARNABLE_SPELL("Learnable spell"),
  CONSUMABLE("Consumable");

  private final String label;

  public String itemLabel(String itemName) {
    return "%s: %s".formatted(label(), itemName);
  }
}
