package com.ff1.editor.view.heroes;

import static com.ff1.editor.view.ui.FxTableColumns.editableIntColumn;
import static com.ff1.editor.view.ui.FxTableColumns.intColumn;
import static com.ff1.editor.view.ui.FxTableColumns.textColumn;

import com.ff1.editor.data.EditorWorkspace;
import com.ff1.editor.data.HeroClassStatsEdit;
import com.ff1.editor.service.HeroClassDiscoveryService;
import com.ff1.editor.view.FxEditorState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public final class FxHeroesView extends BorderPane {

  private final ObservableList<FxHeroClassViewModel> heroes = FXCollections.observableArrayList();
  private final FilteredList<FxHeroClassViewModel> filtered = new FilteredList<>(heroes);
  private final TextField search = new TextField();

  public FxHeroesView(FxEditorState state) {
    state.heroStatsEditSupplier(this::heroStatsEdits);
    getStyleClass().add("heroes-view");
    TableView<FxHeroClassViewModel> table = table();
    table.setItems(filtered);
    table.setEditable(true);
    setTop(filters());
    setCenter(table);
    search.textProperty().addListener((_, _, _) -> refilter());
    state.workspaceProperty().addListener((_, _, workspace) -> load(workspace));
    refilter();
  }

  private HBox filters() {
    search.setPromptText("Search heroes, tiers, notes, or source offsets");
    Button reset = new Button("Reset Stats");
    reset.setOnAction(_ -> heroes.forEach(FxHeroClassViewModel::resetStats));
    HBox controls = new HBox(8, new Label("Search"), search, reset);
    controls.getStyleClass().add("filter-row");
    controls.setPadding(new Insets(8));
    HBox.setHgrow(search, Priority.ALWAYS);
    return controls;
  }

  private void load(EditorWorkspace workspace) {
    List<FxHeroClassViewModel> rows =
        workspace == null
            ? List.of()
            : new HeroClassDiscoveryService(workspace.workDir())
                .discover().stream().map(FxHeroClassViewModel::new).toList();
    rows = mirrorUpgradedRows(rows);
    heroes.setAll(rows);
    refilter();
  }

  private static List<FxHeroClassViewModel> mirrorUpgradedRows(
      List<FxHeroClassViewModel> discoveredRows) {
    Map<Integer, FxHeroClassViewModel> rowsById = new HashMap<>();
    for (FxHeroClassViewModel row : discoveredRows) {
      rowsById.put(row.id(), row);
    }
    return discoveredRows.stream()
        .map(
            row ->
                row.baseClass()
                    ? row
                    : row.mirrorStatsFrom(rowsById.get(row.hero().upgradeFromId())))
        .toList();
  }

  private void refilter() {
    filtered.setPredicate(hero -> hero.matches(search.getText()));
  }

  private List<HeroClassStatsEdit> heroStatsEdits() {
    return heroes.stream()
        .filter(FxHeroClassViewModel::baseClass)
        .filter(FxHeroClassViewModel::changed)
        .map(FxHeroClassViewModel::toEdit)
        .toList();
  }

  private static TableView<FxHeroClassViewModel> table() {
    TableView<FxHeroClassViewModel> table = new TableView<>();
    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    table
        .getColumns()
        .setAll(
            List.of(
                intColumn("ID", FxHeroClassViewModel::id, 56),
                textColumn("Class", FxHeroClassViewModel::name, 140),
                textColumn("Tier", FxHeroClassViewModel::tier, 90),
                textColumn("Upgrade From", FxHeroClassViewModel::upgradeFrom, 110),
                editableIntColumn(
                    "HP",
                    FxHeroClassViewModel::hpProperty,
                    88,
                    0,
                    127,
                    FxHeroClassViewModel::baseClass),
                editableIntColumn(
                    "STR",
                    FxHeroClassViewModel::strengthProperty,
                    88,
                    0,
                    99,
                    FxHeroClassViewModel::baseClass),
                editableIntColumn(
                    "AGL",
                    FxHeroClassViewModel::agilityProperty,
                    88,
                    0,
                    99,
                    FxHeroClassViewModel::baseClass),
                editableIntColumn(
                    "INT",
                    FxHeroClassViewModel::intelligenceProperty,
                    88,
                    0,
                    99,
                    FxHeroClassViewModel::baseClass),
                editableIntColumn(
                    "STA",
                    FxHeroClassViewModel::staminaProperty,
                    88,
                    0,
                    99,
                    FxHeroClassViewModel::baseClass),
                editableIntColumn(
                    "LCK",
                    FxHeroClassViewModel::luckProperty,
                    88,
                    0,
                    99,
                    FxHeroClassViewModel::baseClass),
                textColumn("Source", FxHeroClassViewModel::source, 170),
                textColumn("Notes", FxHeroClassViewModel::notes, 420)));
    return table;
  }
}
