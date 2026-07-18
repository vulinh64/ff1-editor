package com.ff1.editor.data;

public enum PatchState {
  ORIGINAL,
  PATCHED,
  UNKNOWN;

  public static PatchState from(Enum<?> state) {
    return valueOf(state.name());
  }
}
