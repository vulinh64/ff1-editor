package com.ff1.editor.view.monsters;

import static com.ff1.editor.view.ui.FxTableColumns.editableIntColumn;
import static com.ff1.editor.view.ui.FxTableColumns.intColumn;
import static com.ff1.editor.view.ui.FxTableColumns.textColumn;

import com.ff1.editor.data.EditorWorkspace;
import com.ff1.editor.data.MaskOption;
import com.ff1.editor.data.MonsterArchetype;
import com.ff1.editor.data.MonsterElementAffinity;
import com.ff1.editor.data.MonsterStatsEdit;
import com.ff1.editor.service.MonsterDiscoveryService;
import com.ff1.editor.view.FxEditorState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class FxMonstersView extends BorderPane {

  private final ObservableList<FxMonsterRowViewModel> monsters =
      FXCollections.observableArrayList();
  private final FilteredList<FxMonsterRowViewModel> normalMonsters = new FilteredList<>(monsters);
  private final FilteredList<FxMonsterRowViewModel> bossMonsters = new FilteredList<>(monsters);
  private final TextField search = new TextField();

  public FxMonstersView(FxEditorState state) {
    getStyleClass().add("monsters-view");
    setTop(filters());
    setCenter(tabs());
    search.textProperty().addListener((_, _, _) -> refilter());
    state.workspaceProperty().addListener((_, _, workspace) -> load(workspace));
    state.monsterStatsEditSupplier(this::monsterStatsEdits);
    refilter();
  }

  private HBox filters() {
    search.setPromptText("Search monsters, types, weaknesses, resistances, encounters, or offsets");
    HBox controls = new HBox(8, new Label("Search"), search);
    controls.getStyleClass().add("filter-row");
    controls.setPadding(new Insets(8));
    HBox.setHgrow(search, Priority.ALWAYS);
    return controls;
  }

  private void load(EditorWorkspace workspace) {
    List<FxMonsterRowViewModel> rows =
        workspace == null
            ? List.of()
            : new MonsterDiscoveryService(workspace.workDir())
                .discover().stream().map(FxMonsterRowViewModel::new).toList();
    monsters.setAll(rows);
    refilter();
  }

  private void refilter() {
    normalMonsters.setPredicate(
        monster -> !monster.bossOrFixed() && monster.matches(search.getText()));
    bossMonsters.setPredicate(
        monster -> monster.bossOrFixed() && monster.matches(search.getText()));
  }

  private List<MonsterStatsEdit> monsterStatsEdits() {
    return monsters.stream()
        .filter(FxMonsterRowViewModel::changed)
        .map(FxMonsterRowViewModel::toEdit)
        .toList();
  }

  private TabPane tabs() {
    TabPane tabs = new TabPane();
    Tab normal = new Tab("Normal", table(normalMonsters, false));
    normal.setClosable(false);
    Tab bosses = new Tab("Bosses / Fixed", table(bossMonsters, true));
    bosses.setClosable(false);
    tabs.getTabs().addAll(normal, bosses);
    return tabs;
  }

  private static TableView<FxMonsterRowViewModel> table(
      FilteredList<FxMonsterRowViewModel> rows, boolean includeEncounterColumn) {
    TableView<FxMonsterRowViewModel> table = new TableView<>();
    table.setItems(rows);
    table.setEditable(true);
    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    List<TableColumn<FxMonsterRowViewModel, ?>> columns =
        new ArrayList<>(
            List.of(
                intColumn("ID", FxMonsterRowViewModel::id, 56),
                textColumn("Monster", FxMonsterRowViewModel::name, 150),
                editableIntColumn("EXP", FxMonsterRowViewModel::expProperty, 96, 0, 65535),
                editableIntColumn("Gil", FxMonsterRowViewModel::gilProperty, 96, 0, 65535),
                editableIntColumn("HP", FxMonsterRowViewModel::hpProperty, 82, 0, 999),
                editableIntColumn("Attack", FxMonsterRowViewModel::attackProperty, 96, 0, 255),
                editableIntColumn("Hits", FxMonsterRowViewModel::hitCountProperty, 82, 0, 255),
                editableIntColumn("Defense", FxMonsterRowViewModel::defenseProperty, 104, 0, 255),
                editableIntColumn("Evasion", FxMonsterRowViewModel::evasionProperty, 104, 0, 255),
                editableIntColumn(
                    "Magic Def", FxMonsterRowViewModel::magicDefenseProperty, 116, 0, 255),
                maskColumn(
                    "Archetypes",
                    MonsterArchetype.values(),
                    FxMonsterRowViewModel::archetypes,
                    FxMonsterRowViewModel::archetypeMaskValue,
                    FxMonsterRowViewModel::archetypeMaskValue,
                    _ -> 0,
                    3,
                    260),
                textColumn("Archetype Mask", FxMonsterRowViewModel::typeMask, 112),
                maskColumn(
                    "Weaknesses",
                    MonsterElementAffinity.values(),
                    FxMonsterRowViewModel::weaknesses,
                    FxMonsterRowViewModel::weaknessMaskValue,
                    FxMonsterRowViewModel::weaknessMaskValue,
                    FxMonsterRowViewModel::resistanceMaskValue,
                    0,
                    220),
                textColumn("Weak Mask", FxMonsterRowViewModel::weaknessMask, 88),
                maskColumn(
                    "Resists",
                    MonsterElementAffinity.values(),
                    FxMonsterRowViewModel::resistances,
                    FxMonsterRowViewModel::resistanceMaskValue,
                    FxMonsterRowViewModel::resistanceMaskValue,
                    FxMonsterRowViewModel::weaknessMaskValue,
                    0,
                    220),
                textColumn("Resist Mask", FxMonsterRowViewModel::resistanceMask, 96),
                textColumn("Raw 0..3", FxMonsterRowViewModel::rawStart, 98),
                textColumn("Source", FxMonsterRowViewModel::source, 170)));
    if (includeEncounterColumn) {
      columns.add(2, textColumn("No-Run Encounters", FxMonsterRowViewModel::bossEncounterIds, 150));
    }
    table.getColumns().setAll(columns);
    return table;
  }

  private static TableColumn<FxMonsterRowViewModel, FxMonsterRowViewModel> maskColumn(
      String title,
      MaskOption[] options,
      Function<FxMonsterRowViewModel, String> label,
      ToIntFunction<FxMonsterRowViewModel> mask,
      BiConsumer<FxMonsterRowViewModel, Integer> update,
      ToIntFunction<FxMonsterRowViewModel> opposingMask,
      int maxSelected,
      int width) {
    TableColumn<FxMonsterRowViewModel, FxMonsterRowViewModel> column = new TableColumn<>(title);
    column.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
    column.setCellFactory(
        _ -> new MaskCell(title, options, label, mask, update, opposingMask, maxSelected));
    column.setPrefWidth(width);
    return column;
  }

  private static final class MaskCell
      extends TableCell<FxMonsterRowViewModel, FxMonsterRowViewModel> {

    private final String title;
    private final MaskOption[] options;
    private final Function<FxMonsterRowViewModel, String> label;
    private final ToIntFunction<FxMonsterRowViewModel> mask;
    private final BiConsumer<FxMonsterRowViewModel, Integer> update;
    private final ToIntFunction<FxMonsterRowViewModel> opposingMask;
    private final int maxSelected;
    private final Button edit = new Button();

    private MaskCell(
        String title,
        MaskOption[] options,
        Function<FxMonsterRowViewModel, String> label,
        ToIntFunction<FxMonsterRowViewModel> mask,
        BiConsumer<FxMonsterRowViewModel, Integer> update,
        ToIntFunction<FxMonsterRowViewModel> opposingMask,
        int maxSelected) {
      this.title = title;
      this.options = options;
      this.label = label;
      this.mask = mask;
      this.update = update;
      this.opposingMask = opposingMask == null ? _ -> 0 : opposingMask;
      this.maxSelected = maxSelected;
      edit.setMaxWidth(Double.MAX_VALUE);
      edit.setOnAction(_ -> showEditor());
    }

    @Override
    protected void updateItem(FxMonsterRowViewModel row, boolean empty) {
      super.updateItem(row, empty);
      if (empty || row == null) {
        setGraphic(null);
        return;
      }
      String currentLabel = label.apply(row);
      edit.setText(currentLabel.isBlank() ? "<Click to Edit>" : currentLabel);
      setGraphic(edit);
    }

    private void showEditor() {
      FxMonsterRowViewModel row = getItem();
      if (row == null) {
        return;
      }
      Dialog<Integer> dialog = new Dialog<>();
      dialog.setTitle("Edit " + title);
      dialog.setHeaderText(row.name());
      DialogPane pane = dialog.getDialogPane();
      pane.getStylesheets()
          .add(
              Objects.requireNonNull(FxMonstersView.class.getResource("/editor.css"))
                  .toExternalForm());
      pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

      List<CheckBox> boxes = new ArrayList<>();
      VBox content = new VBox(8);
      content.setPadding(new Insets(8, 0, 0, 0));
      int currentMask = mask.applyAsInt(row);
      int blockedMask = opposingMask.applyAsInt(row);
      for (MaskOption option : options) {
        CheckBox checkbox = new CheckBox(option.label());
        checkbox.setSelected((currentMask & option.bit()) != 0);
        if ((blockedMask & option.bit()) != 0 && !checkbox.isSelected()) {
          checkbox.setDisable(true);
        }
        boxes.add(checkbox);
        content.getChildren().add(checkbox);
      }
      Button okButton = (Button) pane.lookupButton(ButtonType.OK);
      okButton
          .disableProperty()
          .bind(
              Bindings.createBooleanBinding(
                  () -> (selectedMask(boxes, options) & blockedMask) != 0,
                  boxes.stream()
                      .map(CheckBox::selectedProperty)
                      .toArray(javafx.beans.Observable[]::new)));
      if (maxSelected > 0) {
        for (CheckBox checkbox : boxes) {
          checkbox
              .disableProperty()
              .bind(
                  Bindings.createBooleanBinding(
                      () -> !checkbox.isSelected() && checkedCount(boxes) >= maxSelected,
                      boxes.stream()
                          .map(CheckBox::selectedProperty)
                          .toArray(javafx.beans.Observable[]::new)));
        }
      }
      pane.setContent(content);
      dialog.setResultConverter(
          button -> button == ButtonType.OK ? selectedMask(boxes, options) : null);
      dialog
          .showAndWait()
          .ifPresent(
              selectedMask -> {
                update.accept(row, selectedMask);
                updateItem(row, false);
              });
    }

    private static int checkedCount(List<CheckBox> boxes) {
      return (int) boxes.stream().filter(CheckBox::isSelected).count();
    }

    private static int selectedMask(List<CheckBox> boxes, MaskOption[] options) {
      int mask = 0;
      for (int i = 0; i < options.length; i++) {
        if (boxes.get(i).isSelected()) {
          mask |= options[i].bit();
        }
      }
      return mask;
    }
  }
}
