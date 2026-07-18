package com.ff1.editor.view.items;

import static com.ff1.editor.view.ui.FxTableColumns.intColumn;
import static com.ff1.editor.view.ui.FxTableColumns.textColumn;

import com.ff1.editor.data.EditorWorkspace;
import com.ff1.editor.data.ItemCategory;
import com.ff1.editor.data.SkillSnapshot;
import com.ff1.editor.data.WeaponCastSpellEdit;
import com.ff1.editor.service.ItemEquipmentDiscoveryService;
import com.ff1.editor.service.SkillDiscoveryService;
import com.ff1.editor.view.FxEditorState;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

public final class FxItemsView extends BorderPane {

  public static final String PRICE_TITLE = "Price";
  public static final String SOURCE_TITLE = "Source";
  public static final String DESCRIPTION_TITLE = "Description";
  private final ObservableList<FxItemRowViewModel> items = FXCollections.observableArrayList();
  private final FilteredList<FxItemRowViewModel> weapons = new FilteredList<>(items);
  private final FilteredList<FxItemRowViewModel> armor = new FilteredList<>(items);
  private final FilteredList<FxItemRowViewModel> otherItems = new FilteredList<>(items);
  private final ObservableList<Integer> skillIds = FXCollections.observableArrayList();
  private final TextField search = new TextField();

  public FxItemsView(FxEditorState state) {
    getStyleClass().add("items-view");
    setTop(filters());
    setCenter(itemTabs());
    search.textProperty().addListener((_, _, _) -> refilter());
    state.workspaceProperty().addListener((_, _, workspace) -> load(workspace));
    state.weaponCastSpellEditSupplier(this::weaponCastSpellEdits);
    refilter();
  }

  private HBox filters() {
    search.setPromptText("Search items, equipment, spells, classes, descriptions, or offsets");
    HBox controls = new HBox(8, new Label("Search"), search);
    controls.getStyleClass().add("filter-row");
    controls.setPadding(new Insets(8));
    HBox.setHgrow(search, Priority.ALWAYS);
    return controls;
  }

  private void load(EditorWorkspace workspace) {
    List<FxItemRowViewModel> rows =
        workspace == null
            ? List.of()
            : new ItemEquipmentDiscoveryService(workspace.workDir())
                .discover().stream().map(FxItemRowViewModel::new).toList();
    List<Integer> skillOptions =
        workspace == null
            ? List.of()
            : new SkillDiscoveryService(workspace.workDir()).discover().stream()
                .map(SkillSnapshot::id)
                .filter(id -> id > 0)
                .toList();
    skillIds.setAll(0);
    skillIds.addAll(skillOptions);
    items.setAll(rows);
    refilter();
  }

  private List<WeaponCastSpellEdit> weaponCastSpellEdits() {
    return items.stream()
        .filter(FxItemRowViewModel::weaponCastChanged)
        .map(FxItemRowViewModel::toWeaponCastSpellEdit)
        .toList();
  }

  private void refilter() {
    weapons.setPredicate(
        item -> item.category() == ItemCategory.WEAPON && item.matches(search.getText()));
    armor.setPredicate(
        item -> item.category() == ItemCategory.ARMOR && item.matches(search.getText()));
    otherItems.setPredicate(
        item ->
            item.category() != ItemCategory.WEAPON
                && item.category() != ItemCategory.ARMOR
                && item.category() != ItemCategory.BLANK
                && item.matches(search.getText()));
  }

  private TabPane itemTabs() {
    TabPane tabs = new TabPane();
    Tab weaponsTab = new Tab("Weapons", weaponTable());
    weaponsTab.setClosable(false);
    Tab armorTab = new Tab("Armor", armorTable());
    armorTab.setClosable(false);
    Tab itemsTab = new Tab("Items", itemTable());
    itemsTab.setClosable(false);
    tabs.getTabs().addAll(weaponsTab, armorTab, itemsTab);
    return tabs;
  }

  private TableView<FxItemRowViewModel> weaponTable() {
    TableView<FxItemRowViewModel> table = baseTable(weapons);
    table.setEditable(true);
    table
        .getColumns()
        .setAll(
            List.of(
                intColumn("ID", FxItemRowViewModel::id, 56),
                textColumn("Weapon", FxItemRowViewModel::name, 150),
                intColumn(PRICE_TITLE, FxItemRowViewModel::price, 82),
                textColumn("Damage", FxItemRowViewModel::damage, 78),
                textColumn("Accuracy", FxItemRowViewModel::accuracy, 82),
                weaponCastSpellColumn(),
                textColumn("Classes", FxItemRowViewModel::allowedClasses, 420),
                textColumn("Mask", FxItemRowViewModel::equipMask, 82),
                textColumn("Special", FxItemRowViewModel::weaponSpecialBytes, 92),
                textColumn(DESCRIPTION_TITLE, FxItemRowViewModel::description, 360),
                textColumn(SOURCE_TITLE, FxItemRowViewModel::source, 170)));
    return table;
  }

  private TableColumn<FxItemRowViewModel, Integer> weaponCastSpellColumn() {
    TableColumn<FxItemRowViewModel, Integer> column = new TableColumn<>("Casts");
    column.setCellValueFactory(cell -> cell.getValue().castSpellIdProperty().asObject());
    column.setCellFactory(
        ComboBoxTableCell.forTableColumn(
            new StringConverter<>() {
              @Override
              public String toString(Integer id) {
                return id == null ? "" : FxItemRowViewModel.castSpellLabel(id);
              }

              @Override
              public Integer fromString(String value) {
                return 0;
              }
            },
            skillIds));
    column.setOnEditCommit(
        event ->
            event
                .getRowValue()
                .castSpellIdProperty()
                .set(event.getNewValue() == null ? 0 : event.getNewValue()));
    column.setPrefWidth(150);
    return column;
  }

  private TableView<FxItemRowViewModel> armorTable() {
    TableView<FxItemRowViewModel> table = baseTable(armor);
    table
        .getColumns()
        .setAll(
            List.of(
                intColumn("ID", FxItemRowViewModel::id, 56),
                textColumn("Type", FxItemRowViewModel::armorSubtype, 82),
                textColumn("Armor", FxItemRowViewModel::name, 150),
                intColumn(PRICE_TITLE, FxItemRowViewModel::price, 82),
                textColumn("Absorb", FxItemRowViewModel::absorb, 78),
                textColumn("Evasion Lower", FxItemRowViewModel::evasionPenalty, 112),
                textColumn("Casts", FxItemRowViewModel::castSpell, 122),
                textColumn("Resist", FxItemRowViewModel::resistanceMask, 82),
                textColumn("Classes", FxItemRowViewModel::allowedClasses, 420),
                textColumn("Mask", FxItemRowViewModel::equipMask, 82),
                textColumn(DESCRIPTION_TITLE, FxItemRowViewModel::description, 360),
                textColumn(SOURCE_TITLE, FxItemRowViewModel::source, 170)));
    return table;
  }

  private TableView<FxItemRowViewModel> itemTable() {
    TableView<FxItemRowViewModel> table = baseTable(otherItems);
    table
        .getColumns()
        .setAll(
            List.of(
                intColumn("ID", FxItemRowViewModel::id, 56),
                textColumn("Category", FxItemRowViewModel::categoryName, 104),
                textColumn("Item", FxItemRowViewModel::name, 160),
                intColumn(PRICE_TITLE, FxItemRowViewModel::price, 82),
                textColumn("Metadata", FxItemRowViewModel::metadataBytes, 100),
                textColumn(DESCRIPTION_TITLE, FxItemRowViewModel::description, 520),
                textColumn("Notes", FxItemRowViewModel::notes, 220),
                textColumn(SOURCE_TITLE, FxItemRowViewModel::source, 170)));
    return table;
  }

  private static TableView<FxItemRowViewModel> baseTable(FilteredList<FxItemRowViewModel> rows) {
    TableView<FxItemRowViewModel> table = new TableView<>();
    table.setItems(rows);
    table.setEditable(false);
    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    return table;
  }
}
