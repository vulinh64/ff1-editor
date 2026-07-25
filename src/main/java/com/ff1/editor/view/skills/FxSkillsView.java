package com.ff1.editor.view.skills;

import static com.ff1.editor.view.ui.FxTableColumns.editableIntColumn;
import static com.ff1.editor.view.ui.FxTableColumns.intColumn;
import static com.ff1.editor.view.ui.FxTableColumns.textColumn;

import com.ff1.editor.data.EditorWorkspace;
import com.ff1.editor.data.SkillEffectEdit;
import com.ff1.editor.data.SkillTarget;
import com.ff1.editor.data.SkillTargetScope;
import com.ff1.editor.service.SkillDiscoveryService;
import com.ff1.editor.view.FxEditorState;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
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
                intColumn("Target ID", FxSkillRowViewModel::targetMode, 82),
                targetColumn(table),
                affectsColumn(table),
                intColumn("Kind ID", FxSkillRowViewModel::effectKind, 78),
                textColumn("Kind Name", FxSkillRowViewModel::effectKindName, 150),
                editableIntColumn("Price", FxSkillRowViewModel::priceProperty, 92, 0, 65535),
                editableIntColumn(
                    "Power/Status", FxSkillRowViewModel::powerOrStatusProperty, 126, 0, 255),
                editableIntColumn("Accuracy", FxSkillRowViewModel::accuracyProperty, 104, 0, 255),
                textColumn("Element/Status", FxSkillRowViewModel::elementOrStatusMask, 110),
                intColumn("Anim", FxSkillRowViewModel::animationId, 64),
                textColumn("Anim Flags", FxSkillRowViewModel::animationFlags, 92),
                intColumn("Raw0", FxSkillRowViewModel::raw0, 64),
                intColumn("Raw5", FxSkillRowViewModel::raw5, 64),
                textColumn("Mask", FxSkillRowViewModel::permissionMask, 82),
                textColumn("Invokers", FxSkillRowViewModel::invokers, 380),
                textColumn("Source", FxSkillRowViewModel::source, 170)));
    return table;
  }

  private TableColumn<FxSkillRowViewModel, SkillTarget> targetColumn(
      TableView<FxSkillRowViewModel> table) {
    TableColumn<FxSkillRowViewModel, SkillTarget> column = new TableColumn<>("Target");
    column.setCellValueFactory(cell -> cell.getValue().targetProperty());
    column.setCellFactory(_ -> new TargetCell(table));
    column.setPrefWidth(90);
    return column;
  }

  private TableColumn<FxSkillRowViewModel, SkillTargetScope> affectsColumn(
      TableView<FxSkillRowViewModel> table) {
    TableColumn<FxSkillRowViewModel, SkillTargetScope> column = new TableColumn<>("Affects");
    column.setCellValueFactory(cell -> cell.getValue().targetScopeProperty());
    column.setCellFactory(_ -> new AffectsCell(table));
    column.setPrefWidth(116);
    return column;
  }

  private static final class TargetCell extends TableCell<FxSkillRowViewModel, SkillTarget> {

    private final TableView<FxSkillRowViewModel> table;
    private final ComboBox<SkillTarget> editor = new ComboBox<>();

    private TargetCell(TableView<FxSkillRowViewModel> table) {
      this.table = table;
      editor.getItems().setAll(SkillTarget.SELF, SkillTarget.PARTY);
      editor.setMaxWidth(Double.MAX_VALUE);
      editor.setOnAction(_ -> commitSelection());
    }

    @Override
    protected void updateItem(SkillTarget target, boolean empty) {
      super.updateItem(target, empty);
      FxSkillRowViewModel row = row();
      if (empty || row == null) {
        setText(null);
        setGraphic(null);
        return;
      }
      if (!row.targetEditable()) {
        setText(target == null ? "" : target.label());
        setGraphic(null);
        setContentDisplay(ContentDisplay.TEXT_ONLY);
        return;
      }
      editor.setValue(target);
      setText(null);
      setGraphic(editor);
      setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    private void commitSelection() {
      FxSkillRowViewModel row = row();
      if (row == null) {
        return;
      }
      row.target(editor.getValue());
      table.refresh();
    }

    private FxSkillRowViewModel row() {
      return getTableRow() == null ? null : getTableRow().getItem();
    }
  }

  private static final class AffectsCell extends TableCell<FxSkillRowViewModel, SkillTargetScope> {

    private final TableView<FxSkillRowViewModel> table;
    private final ComboBox<SkillTargetScope> editor = new ComboBox<>();

    private AffectsCell(TableView<FxSkillRowViewModel> table) {
      this.table = table;
      editor.getItems().setAll(SkillTargetScope.SINGLE, SkillTargetScope.OMNI);
      editor.setMaxWidth(Double.MAX_VALUE);
      editor.setOnAction(_ -> commitSelection());
    }

    @Override
    protected void updateItem(SkillTargetScope scope, boolean empty) {
      super.updateItem(scope, empty);
      FxSkillRowViewModel row = row();
      if (empty || row == null) {
        setText(null);
        setGraphic(null);
        return;
      }
      if (!row.affectsEditable()) {
        setText(scope == null ? "" : scope.label());
        setGraphic(null);
        setContentDisplay(ContentDisplay.TEXT_ONLY);
        return;
      }
      editor.setValue(scope);
      setText(null);
      setGraphic(editor);
      setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    private void commitSelection() {
      FxSkillRowViewModel row = row();
      if (row == null) {
        return;
      }
      row.targetScope(editor.getValue());
      table.refresh();
    }

    private FxSkillRowViewModel row() {
      return getTableRow() == null ? null : getTableRow().getItem();
    }
  }
}
