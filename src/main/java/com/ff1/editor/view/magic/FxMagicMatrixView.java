package com.ff1.editor.view.magic;

import static com.ff1.editor.view.ui.FxTableColumns.intColumn;
import static com.ff1.editor.view.ui.FxTableColumns.maskColumn;
import static com.ff1.editor.view.ui.FxTableColumns.textColumn;

import com.ff1.editor.data.EditorWorkspace;
import com.ff1.editor.data.MagicClassBit;
import com.ff1.editor.data.MagicMatrixEdit;
import com.ff1.editor.data.SpellSchool;
import com.ff1.editor.service.MagicMatrixDiscoveryService;
import com.ff1.editor.view.FxEditorState;
import java.util.List;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public final class FxMagicMatrixView extends BorderPane {

  private final ObservableList<FxMagicMatrixRowViewModel> spells =
      FXCollections.observableArrayList(row -> new Observable[] {row.permissionMaskProperty()});
  private final FilteredList<FxMagicMatrixRowViewModel> whiteMagic = new FilteredList<>(spells);
  private final FilteredList<FxMagicMatrixRowViewModel> blackMagic = new FilteredList<>(spells);
  private final TextField search = new TextField();
  private final TabPane normalTabs = spellTabs();
  private final TableView<FxMagicMatrixRowViewModel> whiteMatrix = matrixTable();
  private final TableView<FxMagicMatrixRowViewModel> blackMatrix = matrixTable();
  private final TabPane matrixTabs = matrixTabs();
  private boolean syncingTabs;

  public FxMagicMatrixView(FxEditorState state) {
    state.magicMatrixEditSupplier(this::magicMatrixEdits);
    getStyleClass().add("magic-matrix-view");
    syncMagicSchoolTabs();
    setTop(filters());
    setCenter(normalTabs);
    search.textProperty().addListener((_, _, _) -> refilter());
    state.workspaceProperty().addListener((_, _, workspace) -> load(workspace));
    refilter();
  }

  private HBox filters() {
    search.setPromptText("Search spells, levels, schools, classes, masks, or source offsets");
    Button reset = new Button("Reset Permissions");
    reset.setOnAction(_ -> spells.forEach(FxMagicMatrixRowViewModel::reset));
    ToggleGroup viewMode = new ToggleGroup();
    RadioButton normal = new RadioButton("Normal");
    normal.setToggleGroup(viewMode);
    normal.setSelected(true);
    RadioButton matrix = new RadioButton("Matrix");
    matrix.setToggleGroup(viewMode);
    normal.setOnAction(_ -> showTabs(normalTabs, matrixTabs));
    matrix.setOnAction(_ -> showTabs(matrixTabs, normalTabs));
    HBox controls =
        new HBox(8, new Label("View"), normal, matrix, new Label("Search"), search, reset);
    controls.getStyleClass().add("filter-row");
    controls.setPadding(new Insets(8));
    HBox.setHgrow(search, Priority.ALWAYS);
    return controls;
  }

  private void showTabs(TabPane target, TabPane source) {
    target.getSelectionModel().select(source.getSelectionModel().getSelectedIndex());
    setCenter(target);
  }

  private void syncMagicSchoolTabs() {
    normalTabs
        .getSelectionModel()
        .selectedIndexProperty()
        .addListener((_, _, index) -> syncSelectedMagicSchool(matrixTabs, index.intValue()));
    matrixTabs
        .getSelectionModel()
        .selectedIndexProperty()
        .addListener((_, _, index) -> syncSelectedMagicSchool(normalTabs, index.intValue()));
  }

  private void syncSelectedMagicSchool(TabPane target, int index) {
    if (syncingTabs || index < 0 || index >= target.getTabs().size()) {
      return;
    }
    syncingTabs = true;
    target.getSelectionModel().select(index);
    syncingTabs = false;
  }

  private void load(EditorWorkspace workspace) {
    List<FxMagicMatrixRowViewModel> rows =
        workspace == null
            ? List.of()
            : new MagicMatrixDiscoveryService(workspace.workDir())
                .discover().stream().map(FxMagicMatrixRowViewModel::new).toList();
    spells.setAll(rows);
    refilter();
  }

  private void refilter() {
    String query = search.getText();
    whiteMagic.setPredicate(spell -> spell.school() == SpellSchool.WHITE && spell.matches(query));
    blackMagic.setPredicate(spell -> spell.school() == SpellSchool.BLACK && spell.matches(query));
    rebuildMatrixColumns(whiteMatrix, List.copyOf(whiteMagic));
    rebuildMatrixColumns(blackMatrix, List.copyOf(blackMagic));
  }

  private List<MagicMatrixEdit> magicMatrixEdits() {
    return spells.stream()
        .filter(FxMagicMatrixRowViewModel::changed)
        .map(FxMagicMatrixRowViewModel::toEdit)
        .toList();
  }

  private TabPane spellTabs() {
    TabPane tabs = new TabPane();
    Tab white = new Tab("White Magic", table(whiteMagic));
    white.setClosable(false);
    Tab black = new Tab("Black Magic", table(blackMagic));
    black.setClosable(false);
    tabs.getTabs().addAll(white, black);
    return tabs;
  }

  private TabPane matrixTabs() {
    TabPane tabs = new TabPane();
    Tab white = new Tab("White Magic Matrix", whiteMatrix);
    white.setClosable(false);
    Tab black = new Tab("Black Magic Matrix", blackMatrix);
    black.setClosable(false);
    tabs.getTabs().addAll(white, black);
    return tabs;
  }

  private static TableView<FxMagicMatrixRowViewModel> table(
      FilteredList<FxMagicMatrixRowViewModel> rows) {
    TableView<FxMagicMatrixRowViewModel> table = new TableView<>();
    table.setItems(rows);
    table.setEditable(true);
    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    table
        .getColumns()
        .addAll(
            List.of(
                intColumn("ID", FxMagicMatrixRowViewModel::spellId, 56),
                textColumn("Spell", FxMagicMatrixRowViewModel::name, 132),
                intColumn("LV", FxMagicMatrixRowViewModel::level, 56),
                intColumn("Slot", FxMagicMatrixRowViewModel::slot, 56),
                maskColumn(
                    "Classes",
                    MagicClassBit.values(),
                    FxMagicMatrixRowViewModel::allowedClasses,
                    FxMagicMatrixRowViewModel::permissionMaskValue,
                    FxMagicMatrixRowViewModel::permissionMaskValue,
                    FxMagicMatrixRowViewModel::name,
                    420),
                textColumn("Description", FxMagicMatrixRowViewModel::description, 360)));
    table
        .getColumns()
        .addAll(
            List.of(
                textColumn("Mask", FxMagicMatrixRowViewModel::maskHex, 82),
                textColumn("Source", FxMagicMatrixRowViewModel::source, 170)));
    return table;
  }

  private static TableView<FxMagicMatrixRowViewModel> matrixTable() {
    TableView<FxMagicMatrixRowViewModel> table = new TableView<>();
    table.getStyleClass().add("permission-matrix-table");
    table.setEditable(true);
    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    return table;
  }

  private static void rebuildMatrixColumns(
      TableView<FxMagicMatrixRowViewModel> table, List<FxMagicMatrixRowViewModel> spells) {
    table.setItems(FXCollections.observableArrayList(spells));
    table
        .getColumns()
        .setAll(
            List.of(
                intColumn("ID", FxMagicMatrixRowViewModel::spellId, 56),
                textColumn("Spell", FxMagicMatrixRowViewModel::name, 132),
                intColumn("LV", FxMagicMatrixRowViewModel::level, 56),
                intColumn("Slot", FxMagicMatrixRowViewModel::slot, 56)));
    for (MagicClassBit bit : MagicClassBit.values()) {
      table.getColumns().add(classMatrixColumn(bit));
    }
  }

  private static TableColumn<FxMagicMatrixRowViewModel, Boolean> classMatrixColumn(
      MagicClassBit bit) {
    TableColumn<FxMagicMatrixRowViewModel, Boolean> column = new TableColumn<>(bit.label());
    column.setCellValueFactory(cell -> cell.getValue().classBitProperty(bit));
    column.setCellFactory(CheckBoxTableCell.forTableColumn(column));
    column.setEditable(true);
    column.setPrefWidth(92);
    return column;
  }
}
