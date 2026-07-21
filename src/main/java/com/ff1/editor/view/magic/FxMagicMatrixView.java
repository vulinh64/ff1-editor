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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public final class FxMagicMatrixView extends BorderPane {

  private final ObservableList<FxMagicMatrixRowViewModel> spells =
      FXCollections.observableArrayList();
  private final FilteredList<FxMagicMatrixRowViewModel> whiteMagic = new FilteredList<>(spells);
  private final FilteredList<FxMagicMatrixRowViewModel> blackMagic = new FilteredList<>(spells);
  private final TextField search = new TextField();

  public FxMagicMatrixView(FxEditorState state) {
    state.magicMatrixEditSupplier(this::magicMatrixEdits);
    getStyleClass().add("magic-matrix-view");
    setTop(filters());
    setCenter(spellTabs());
    search.textProperty().addListener((_, _, _) -> refilter());
    state.workspaceProperty().addListener((_, _, workspace) -> load(workspace));
    refilter();
  }

  private HBox filters() {
    search.setPromptText("Search spells, levels, schools, classes, masks, or source offsets");
    Button reset = new Button("Reset Permissions");
    reset.setOnAction(_ -> spells.forEach(FxMagicMatrixRowViewModel::reset));
    HBox controls = new HBox(8, new Label("Search"), search, reset);
    controls.getStyleClass().add("filter-row");
    controls.setPadding(new Insets(8));
    HBox.setHgrow(search, Priority.ALWAYS);
    return controls;
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
}
