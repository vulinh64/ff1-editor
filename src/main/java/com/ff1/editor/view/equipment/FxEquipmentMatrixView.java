package com.ff1.editor.view.equipment;

import static com.ff1.editor.view.ui.FxTableColumns.intColumn;
import static com.ff1.editor.view.ui.FxTableColumns.textColumn;

import com.ff1.editor.data.EditorWorkspace;
import com.ff1.editor.data.EquipmentPermissionEdit;
import com.ff1.editor.data.ItemCategory;
import com.ff1.editor.data.MagicClassBit;
import com.ff1.editor.service.EquipmentMatrixDiscoveryService;
import com.ff1.editor.view.FxEditorState;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public final class FxEquipmentMatrixView extends BorderPane {

  private final ObservableList<FxEquipmentMatrixRowViewModel> equipment =
      FXCollections.observableArrayList();
  private final FilteredList<FxEquipmentMatrixRowViewModel> weapons = new FilteredList<>(equipment);
  private final FilteredList<FxEquipmentMatrixRowViewModel> armor = new FilteredList<>(equipment);
  private final TextField search = new TextField();

  public FxEquipmentMatrixView(FxEditorState state) {
    state.equipmentPermissionEditSupplier(this::equipmentPermissionEdits);
    getStyleClass().add("equipment-matrix-view");
    setTop(filters());
    setCenter(equipmentTabs());
    search.textProperty().addListener((_, _, _) -> refilter());
    state.workspaceProperty().addListener((_, _, workspace) -> load(workspace));
    refilter();
  }

  private HBox filters() {
    search.setPromptText("Search equipment, classes, masks, types, or source offsets");
    Button reset = new Button("Reset Matrix");
    reset.setOnAction(_ -> equipment.forEach(FxEquipmentMatrixRowViewModel::reset));
    HBox controls = new HBox(8, new Label("Search"), search, reset);
    controls.getStyleClass().add("filter-row");
    controls.setPadding(new Insets(8));
    HBox.setHgrow(search, Priority.ALWAYS);
    return controls;
  }

  private void load(EditorWorkspace workspace) {
    List<FxEquipmentMatrixRowViewModel> rows =
        workspace == null
            ? List.of()
            : new EquipmentMatrixDiscoveryService(workspace.workDir())
                .discover().stream().map(FxEquipmentMatrixRowViewModel::new).toList();
    equipment.setAll(rows);
    refilter();
  }

  private void refilter() {
    String query = search.getText();
    weapons.setPredicate(item -> item.category() == ItemCategory.WEAPON && item.matches(query));
    armor.setPredicate(item -> item.category() == ItemCategory.ARMOR && item.matches(query));
  }

  private List<EquipmentPermissionEdit> equipmentPermissionEdits() {
    return equipment.stream()
        .filter(FxEquipmentMatrixRowViewModel::changed)
        .map(FxEquipmentMatrixRowViewModel::toEdit)
        .toList();
  }

  private TabPane equipmentTabs() {
    TabPane tabs = new TabPane();
    Tab weaponsTab = new Tab("Weapons", table(weapons, false));
    weaponsTab.setClosable(false);
    Tab armorTab = new Tab("Armor", table(armor, true));
    armorTab.setClosable(false);
    tabs.getTabs().addAll(weaponsTab, armorTab);
    return tabs;
  }

  private static TableView<FxEquipmentMatrixRowViewModel> table(
      FilteredList<FxEquipmentMatrixRowViewModel> rows, boolean showSubtype) {
    TableView<FxEquipmentMatrixRowViewModel> table = new TableView<>();
    table.setItems(rows);
    table.setEditable(true);
    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    table
        .getColumns()
        .addAll(
            List.of(
                intColumn("ID", FxEquipmentMatrixRowViewModel::itemId, 56),
                textColumn(
                    showSubtype ? "Armor" : "Weapon", FxEquipmentMatrixRowViewModel::name, 150)));
    if (showSubtype) {
      table.getColumns().add(textColumn("Type", FxEquipmentMatrixRowViewModel::subtype, 86));
    }
    for (MagicClassBit bit : MagicClassBit.values()) {
      table.getColumns().add(classColumn(bit));
    }
    table
        .getColumns()
        .addAll(
            List.of(
                textColumn("Mask", FxEquipmentMatrixRowViewModel::maskHex, 82),
                textColumn("Source", FxEquipmentMatrixRowViewModel::source, 170)));
    return table;
  }

  private static TableColumn<FxEquipmentMatrixRowViewModel, Boolean> classColumn(
      MagicClassBit bit) {
    TableColumn<FxEquipmentMatrixRowViewModel, Boolean> column =
        new TableColumn<>(bit.displayName());
    column.setCellValueFactory(cell -> cell.getValue().classBitProperty(bit));
    column.setCellFactory(CheckBoxTableCell.forTableColumn(column));
    column.setEditable(true);
    column.setPrefWidth(92);
    return column;
  }
}
