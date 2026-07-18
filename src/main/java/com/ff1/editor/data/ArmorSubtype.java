package com.ff1.editor.data;

public enum ArmorSubtype {
  BODY("Body"),
  SHIELD("Shield"),
  HELM("Helm"),
  GLOVES("Gloves");

  private final String displayName;

  ArmorSubtype(String displayName) {
    this.displayName = displayName;
  }

  public String displayName() {
    return displayName;
  }
}
