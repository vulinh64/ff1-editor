package com.ff1.editor.view.skills;

import static com.ff1.editor.view.ui.FxTableColumns.editableIntColumn;
import static com.ff1.editor.view.ui.FxTableColumns.intColumn;
import static com.ff1.editor.view.ui.FxTableColumns.textColumn;

import com.ff1.editor.data.EditorWorkspace;
import com.ff1.editor.data.SkillEffectEdit;
import com.ff1.editor.service.SkillDiscoveryService;
import com.ff1.editor.view.FxEditorState;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public final class FxSkillsView extends BorderPane {

  private final ObservableList<FxSkillRowViewModel> skills = FXCollections.observableArrayList();
  private final FilteredList<FxSkillRowViewModel> filtered = new FilteredList<>(skills);
  private final TextField search = new TextField();
  private final CheckBox invokedOnly = new CheckBox("Invoked only");
  private final CheckBox internalOnly = new CheckBox("Internal only");

  public FxSkillsView(FxEditorState state) {
    getStyleClass().add("skills-view");
    setTop(filters());
    setCenter(table());
    search.textProperty().addListener((_, _, _) -> refilter());
    invokedOnly.selectedProperty().addListener((_, _, _) -> refilter());
    internalOnly.selectedProperty().addListener((_, _, _) -> refilter());
    state.workspaceProperty().addListener((_, _, workspace) -> load(workspace));
    state.skillEffectEditSupplier(this::skillEffectEdits);
    refilter();
  }

  private HBox filters() {
    search.setPromptText("Search skill/effect records, spell names, invokers, masks, or offsets");
    HBox controls = new HBox(8, new Label("Search"), search, invokedOnly, internalOnly);
    controls.getStyleClass().add("filter-row");
    controls.setPadding(new Insets(8));
    HBox.setHgrow(search, Priority.ALWAYS);
    return controls;
  }

  private void load(EditorWorkspace workspace) {
    List<FxSkillRowViewModel> rows =
        workspace == null
            ? List.of()
            : new SkillDiscoveryService(workspace.workDir())
                .discover().stream().map(FxSkillRowViewModel::new).toList();
    skills.setAll(rows);
    refilter();
  }

  private List<SkillEffectEdit> skillEffectEdits() {
    return skills.stream()
        .filter(FxSkillRowViewModel::changed)
        .map(FxSkillRowViewModel::toEdit)
        .toList();
  }

  private void refilter() {
    filtered.setPredicate(
        skill ->
            skill.matches(search.getText())
                && (!invokedOnly.isSelected() || skill.invoked())
                && (!internalOnly.isSelected() || skill.internalOnly()));
  }

  private TableView<FxSkillRowViewModel> table() {
    TableView<FxSkillRowViewModel> table = new TableView<>();
    table.setItems(filtered);
    table.setEditable(true);
    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    table
        .getColumns()
        .setAll(
            List.of(
                intColumn("ID", FxSkillRowViewModel::id, 56),
                textColumn("Name", FxSkillRowViewModel::name, 140),
                textColumn("Learnable", FxSkillRowViewModel::learnableLabel, 110),
                intColumn("Effect ID", FxSkillRowViewModel::effectId, 82),
                textColumn("Kind", FxSkillRowViewModel::effectKindName, 150),
                editableIntColumn(
                    "Power/Status", FxSkillRowViewModel::powerOrStatusProperty, 126, 0, 255),
                editableIntColumn("Accuracy", FxSkillRowViewModel::accuracyProperty, 104, 0, 255),
                textColumn("Element/Status", FxSkillRowViewModel::elementOrStatusMask, 110),
                intColumn("Anim", FxSkillRowViewModel::animationId, 64),
                textColumn("Anim Flags", FxSkillRowViewModel::animationFlags, 92),
                intColumn("Raw0", FxSkillRowViewModel::raw0, 64),
                intColumn("Raw5", FxSkillRowViewModel::raw5, 64),
                textColumn("Mask", FxSkillRowViewModel::permissionMask, 82),
                intColumn("Price", FxSkillRowViewModel::price, 82),
                textColumn("Invokers", FxSkillRowViewModel::invokers, 380),
                textColumn("Source", FxSkillRowViewModel::source, 170)));
    return table;
  }
}
