package com.ff1.editor.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum PatchState implements LabeledValue {
  ORIGINAL("available", "available"),
  PATCHED("already patched", "already patched"),
  UNKNOWN("unavailable for this class layout", "unsupported layout");

  private final String label;
  private final String tooltipLabel;

  public static PatchState from(Enum<?> state) {
    return valueOf(state.name());
  }
}
