package com.ff1.editor.data;

public enum ShopMappingStatus {
  CONFIRMED("confirmed"),
  WIKI_BACKED("wiki-backed"),
  LIKELY("likely"),
  UNCONFIRMED("unconfirmed");

  private final String displayName;

  ShopMappingStatus(String displayName) {
    this.displayName = displayName;
  }

  public String displayName() {
    return displayName;
  }
}
