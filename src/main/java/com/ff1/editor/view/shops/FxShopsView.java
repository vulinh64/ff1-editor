package com.ff1.editor.view.shops;

import static com.ff1.editor.view.ui.FxTableColumns.editableIntColumn;
import static com.ff1.editor.view.ui.FxTableColumns.intColumn;
import static com.ff1.editor.view.ui.FxTableColumns.textColumn;

import com.ff1.editor.data.EditorWorkspace;
import com.ff1.editor.data.ShopGoodSnapshot;
import com.ff1.editor.data.ShopInventoryEdit;
import com.ff1.editor.data.ShopLocationSnapshot;
import com.ff1.editor.data.ShopPriceEdit;
import com.ff1.editor.data.ShopServiceKind;
import com.ff1.editor.data.ShopServiceSnapshot;
import com.ff1.editor.service.ShopDiscoveryService;
import com.ff1.editor.view.FxEditorState;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

public final class FxShopsView extends BorderPane {

  private final FxEditorState state;
  private final ObservableList<ShopLocationSnapshot> locations =
      FXCollections.observableArrayList();
  private final ObservableList<ShopServiceSnapshot> services = FXCollections.observableArrayList();
  private final ObservableList<FxShopSlotRowViewModel> slots = FXCollections.observableArrayList();
  private final ObservableList<FxShopPriceRowViewModel> prices =
      FXCollections.observableArrayList();
  private final ObservableList<Integer> goodIds = FXCollections.observableArrayList();
  private Map<Integer, ShopGoodSnapshot> goods = Map.of();
  private final ComboBox<ShopLocationSnapshot> location = new ComboBox<>(locations);
  private final ComboBox<ShopServiceSnapshot> service = new ComboBox<>(services);
  private final TableView<FxShopSlotRowViewModel> slotTable = slotTable();
  private final TableView<FxShopPriceRowViewModel> priceTable = priceTable();
  private final Map<String, ShopInventoryEdit> inventoryEdits = new LinkedHashMap<>();
  private final Map<String, ShopPriceEdit> priceEdits = new LinkedHashMap<>();

  public FxShopsView(FxEditorState state) {
    this.state = state;
    getStyleClass().add("shops-view");
    setTop(filters());
    setCenter(slotTable);
    location.setMaxWidth(Double.MAX_VALUE);
    service.setMaxWidth(Double.MAX_VALUE);
    location.valueProperty().addListener((_, _, selected) -> selectLocation(selected));
    service.valueProperty().addListener((_, _, selected) -> selectService(selected));
    state.workspaceProperty().addListener((_, _, workspace) -> load(workspace));
    state.shopInventoryEditSupplier(this::shopInventoryEdits);
    state.shopPriceEditSupplier(this::shopPriceEdits);
  }

  private Node filters() {
    HBox controls = new HBox(8, new Label("Town/Location"), location, new Label("Shop"), service);
    controls.getStyleClass().add("filter-row");
    controls.setPadding(new Insets(8));
    HBox.setHgrow(location, Priority.ALWAYS);
    HBox.setHgrow(service, Priority.ALWAYS);
    return controls;
  }

  private void load(EditorWorkspace workspace) {
    stashCurrentEdits();
    inventoryEdits.clear();
    priceEdits.clear();
    slots.clear();
    prices.clear();
    services.clear();
    if (workspace == null) {
      locations.clear();
      return;
    }
    locations.setAll(new ShopDiscoveryService(workspace.workDir()).locations());
    location.getSelectionModel().selectFirst();
  }

  private void selectLocation(ShopLocationSnapshot selected) {
    stashCurrentEdits();
    slots.clear();
    prices.clear();
    services.setAll(selected == null ? List.of() : selected.services());
    service.getSelectionModel().selectFirst();
  }

  private void selectService(ShopServiceSnapshot selected) {
    stashCurrentEdits();
    slots.clear();
    prices.clear();
    EditorWorkspace workspace = state.workspace();
    if (workspace == null || selected == null) {
      return;
    }
    ShopDiscoveryService discovery = new ShopDiscoveryService(workspace.workDir());
    if (selected.kind() == ShopServiceKind.INVENTORY) {
      List<ShopGoodSnapshot> options = discovery.goodOptions(selected);
      goods =
          options.stream()
              .collect(
                  Collectors.toMap(ShopGoodSnapshot::id, Function.identity(), (_, newer) -> newer));
      goodIds.setAll(options.stream().map(ShopGoodSnapshot::id).toList());
      slots.setAll(
          discovery.slots(selected).stream()
              .map(slot -> new FxShopSlotRowViewModel(slot, goods))
              .toList());
      for (FxShopSlotRowViewModel row : slots) {
        ShopInventoryEdit edit = row.toEdit();
        ShopInventoryEdit pending =
            inventoryEdits.get(
                "%d:%d:%d".formatted(edit.shopType(), edit.rowIndex(), edit.slotIndex()));
        if (pending != null) {
          row.goodId(pending.goodId());
        }
      }
      setCenter(slotTable);
    } else {
      prices.setAll(new FxShopPriceRowViewModel(discovery.price(selected)));
      for (FxShopPriceRowViewModel row : prices) {
        ShopPriceEdit edit = row.toEdit();
        ShopPriceEdit pending =
            priceEdits.get("%d:%d".formatted(edit.rowIndex(), edit.serviceColumn()));
        if (pending != null) {
          row.value(pending.price());
        }
      }
      setCenter(priceTable);
    }
  }

  private List<ShopInventoryEdit> shopInventoryEdits() {
    stashCurrentEdits();
    return List.copyOf(inventoryEdits.values());
  }

  private List<ShopPriceEdit> shopPriceEdits() {
    stashCurrentEdits();
    return List.copyOf(priceEdits.values());
  }

  private void stashCurrentEdits() {
    for (FxShopSlotRowViewModel row : slots) {
      ShopInventoryEdit edit = row.toEdit();
      String key = "%d:%d:%d".formatted(edit.shopType(), edit.rowIndex(), edit.slotIndex());
      if (row.changed()) {
        inventoryEdits.put(key, edit);
      } else {
        inventoryEdits.remove(key);
      }
    }
    for (FxShopPriceRowViewModel row : prices) {
      ShopPriceEdit edit = row.toEdit();
      String key = "%d:%d".formatted(edit.rowIndex(), edit.serviceColumn());
      if (row.changed()) {
        priceEdits.put(key, edit);
      } else {
        priceEdits.remove(key);
      }
    }
  }

  private TableView<FxShopSlotRowViewModel> slotTable() {
    TableView<FxShopSlotRowViewModel> table = new TableView<>(slots);
    table.setEditable(true);
    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    table
        .getColumns()
        .setAll(
            List.of(
                intColumn("Slot", FxShopSlotRowViewModel::slotNumber, 64),
                goodColumn(),
                textColumn("Decoded Good", FxShopSlotRowViewModel::goodName, 220),
                textColumn("Category", FxShopSlotRowViewModel::category, 120),
                textColumn("Price", FxShopSlotRowViewModel::price, 92),
                textColumn("Source", FxShopSlotRowViewModel::source, 170)));
    return table;
  }

  private TableColumn<FxShopSlotRowViewModel, Integer> goodColumn() {
    TableColumn<FxShopSlotRowViewModel, Integer> column = new TableColumn<>("Equipment/Item");
    column.setCellValueFactory(cell -> cell.getValue().goodIdProperty().asObject());
    column.setCellFactory(
        ComboBoxTableCell.forTableColumn(
            new StringConverter<>() {
              @Override
              public String toString(Integer id) {
                if (id == null) {
                  return "";
                }
                ShopGoodSnapshot good = goods.get(id);
                return good == null ? "%d - <unknown>".formatted(id) : good.toString();
              }

              @Override
              public Integer fromString(String value) {
                return 0;
              }
            },
            goodIds));
    column.setOnEditCommit(
        event -> {
          event.getRowValue().goodId(event.getNewValue() == null ? 0 : event.getNewValue());
          slotTable.refresh();
        });
    column.setPrefWidth(220);
    return column;
  }

  private TableView<FxShopPriceRowViewModel> priceTable() {
    TableView<FxShopPriceRowViewModel> table = new TableView<>(prices);
    table.setEditable(true);
    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    table
        .getColumns()
        .setAll(
            List.of(
                textColumn("Service", FxShopPriceRowViewModel::service, 160),
                editableIntColumn("Price", FxShopPriceRowViewModel::valueProperty, 120, 0, 65535),
                textColumn("Source", FxShopPriceRowViewModel::source, 170)));
    return table;
  }
}
