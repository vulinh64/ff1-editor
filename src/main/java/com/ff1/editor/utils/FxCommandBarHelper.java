package com.ff1.editor.utils;

import com.ff1.editor.data.PatchState;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

public final class FxCommandBarHelper {

  private FxCommandBarHelper() {}

  public static CheckBox dialogCheckBox(CheckBox source, PatchState patchState) {
    CheckBox checkbox = new CheckBox(source.getText());
    checkbox.setSelected(source.isSelected());
    checkbox.setDisable(source.isDisable());
    checkbox.setTooltip(new Tooltip(optionTooltip(source.getText(), patchState)));
    return checkbox;
  }

  public static HBox optionRow(CheckBox checkbox) {
    HBox row = new HBox(checkbox);
    row.setPickOnBounds(true);
    Tooltip tooltip = checkbox.getTooltip();
    if (tooltip != null) {
      Tooltip.install(row, new Tooltip(tooltip.getText()));
    }
    return row;
  }

  public static int selectedOriginal(CheckBox checkbox, PatchState patchState) {
    return checkbox.isSelected() && patchState == PatchState.ORIGINAL ? 1 : 0;
  }

  public static int changedToggle(CheckBox checkbox, PatchState patchState) {
    if (patchState == PatchState.UNKNOWN) {
      return 0;
    }
    boolean currentlyPatched = patchState == PatchState.PATCHED;
    return checkbox.isSelected() != currentlyPatched ? 1 : 0;
  }

  public static String patchStateLabel(PatchState state) {
    return switch (state) {
      case ORIGINAL -> "available";
      case PATCHED -> "already patched";
      case UNKNOWN -> "unavailable for this class layout";
    };
  }

  private static String optionTooltip(String text, PatchState state) {
    String description =
        switch (text) {
          case "Force strong level-ups" ->
              "Every level-up uses the strong-growth path: bonus HP, plus +1 to every body stat.";
          case "Universal spell-charge growth" -> "Every class gains spell charges while leveling.";
          case "15 max spell charges" -> "Raises the spell-charge cap from 9 to 15.";
          case "Damage-causing spells scale with INT" ->
              "Damage-causing player spells gain 1% damage for every 2 INT.";
          case "Healing spells scale with INT" ->
              "Non-full-heal player healing gains 1% effectiveness for every 2 INT.";
          case "INT+STA reduce enemy spell effects" ->
              "Hero INT + STA reduces enemy spell damage and some enemy spell success chances.";
          case "Always successful run" ->
              "The Run command always succeeds, except in forced or boss encounters.";
          case "Party action order" ->
              "Party actions resolve before enemies: magic, then items, then attacks, then Run. Within the same action group, lower party slots act first.";
          case "Enemy crits respect party defense" ->
              "Enemy critical-hit bonus damage is reduced by party defense, so heavy heroes take much less critical damage.";
          case "Weapon affinity damage bonus" ->
              "When a weapon matches an enemy weakness or archetype, it adds half its weapon damage to attack and clamps hit chance to 255.";
          case "Masamune and Excalibur always crit" ->
              "Masamune and Excalibur force no-miss hit chance and all-critical landed hit rolls. This toggle can apply or remove the patch.";
          case "Cottage revives KO" ->
              "Cottage revives KO party members and restores full HP and spell charges.";
          case "Airship lands on safe terrain" ->
              "The airship can land on walkable land terrain, while still rejecting water.";
          default -> text;
        };
    return "%s\nStatus: %s".formatted(description, optionTooltipState(state));
  }

  private static String optionTooltipState(PatchState state) {
    return switch (state) {
      case ORIGINAL -> "available";
      case PATCHED -> "already patched";
      case UNKNOWN -> "unsupported layout";
    };
  }
}
