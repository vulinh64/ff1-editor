package com.ff1.editor.data;

public enum SpellSchool {
  WHITE("White"),
  BLACK("Black");

  private final String displayName;

  SpellSchool(String displayName) {
    this.displayName = displayName;
  }

  public String displayName() {
    return displayName;
  }
}
