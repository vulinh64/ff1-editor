package com.ff1.editor.view.ui;

import com.ff1.editor.data.MaskOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;

public final class FxTableColumns {

  private FxTableColumns() {}

  public static <T> TableColumn<T, Number> intColumn(
      String title, ToIntFunction<T> value, int width) {
    TableColumn<T, Number> column = new TableColumn<>(title);
    column.setCellValueFactory(
        cell -> new SimpleIntegerProperty(value.applyAsInt(cell.getValue())));
    column.setPrefWidth(width);
    return column;
  }

  public static <T> TableColumn<T, Integer> editableIntColumn(
      String title, Function<T, IntegerProperty> property, int width, int min, int max) {
    return editableIntColumn(title, property, width, min, max, _ -> true);
  }

  public static <T> TableColumn<T, Integer> editableIntColumn(
      String title,
      Function<T, IntegerProperty> property,
      int width,
      int min,
      int max,
      Predicate<T> editable) {
    TableColumn<T, Integer> column = new TableColumn<>("%s (%d..%d)".formatted(title, min, max));
    column.setCellValueFactory(cell -> property.apply(cell.getValue()).asObject());
    column.setCellFactory(_ -> new CommittingIntegerCell<>(property, min, max, editable));
    column.setPrefWidth(width);
    return column;
  }

  public static <T> TableColumn<T, String> textColumn(
      String title, Function<T, ?> value, int width) {
    TableColumn<T, String> column = new TableColumn<>(title);
    column.setCellValueFactory(
        cell -> new SimpleStringProperty(String.valueOf(value.apply(cell.getValue()))));
    column.setPrefWidth(width);
    return column;
  }

  public static <T> TableColumn<T, T> maskColumn(
      String title,
      MaskOption[] options,
      Function<T, String> label,
      ToIntFunction<T> mask,
      BiConsumer<T, Integer> update,
      Function<T, String> dialogHeader,
      int width) {
    TableColumn<T, T> column = new TableColumn<>(title);
    column.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
    column.setCellFactory(_ -> new MaskCell<>(title, options, label, mask, update, dialogHeader));
    column.setPrefWidth(width);
    return column;
  }

  private static final class CommittingIntegerCell<T> extends TableCell<T, Integer> {

    private final Function<T, IntegerProperty> property;
    private final int min;
    private final int max;
    private final Predicate<T> editable;
    private TextField editor;

    private CommittingIntegerCell(
        Function<T, IntegerProperty> property, int min, int max, Predicate<T> editable) {
      this.property = property;
      this.min = min;
      this.max = max;
      this.editable = editable == null ? _ -> true : editable;
    }

    @Override
    public void startEdit() {
      T row = getTableRow() == null ? null : getTableRow().getItem();
      if (row == null || !editable.test(row)) {
        return;
      }
      super.startEdit();
      if (editor == null) {
        editor = new TextField();
        editor.setOnAction(_ -> commitEditorValue());
        editor.setOnKeyPressed(
            event -> {
              if (event.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
              }
            });
        editor
            .focusedProperty()
            .addListener(
                (_, _, focused) -> {
                  if (!focused && isEditing()) {
                    commitEditorValue();
                  }
                });
      }
      editor.setText(format(getItem()));
      setText(null);
      setGraphic(editor);
      setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
      editor.requestFocus();
      editor.selectAll();
    }

    @Override
    public void cancelEdit() {
      super.cancelEdit();
      setText(format(getItem()));
      setGraphic(null);
      setContentDisplay(ContentDisplay.TEXT_ONLY);
    }

    @Override
    protected void updateItem(Integer item, boolean empty) {
      super.updateItem(item, empty);
      if (empty) {
        setText(null);
        setGraphic(null);
        return;
      }
      if (isEditing()) {
        editor.setText(format(item));
        setText(null);
        setGraphic(editor);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        return;
      }
      setText(format(item));
      setGraphic(null);
      setContentDisplay(ContentDisplay.TEXT_ONLY);
    }

    private void commitEditorValue() {
      try {
        int newValue = Integer.parseInt(editor.getText().trim());
        if (newValue < min || newValue > max) {
          cancelEdit();
          return;
        }
        T row = getTableRow() == null ? null : getTableRow().getItem();
        if (row != null) {
          property.apply(row).set(newValue);
        }
        commitEdit(newValue);
      } catch (NumberFormatException _) {
        cancelEdit();
      }
    }

    private static String format(Integer value) {
      return value == null ? StringUtils.EMPTY : String.valueOf(value);
    }
  }

  private static final class MaskCell<T> extends TableCell<T, T> {

    private final String title;
    private final MaskOption[] options;
    private final Function<T, String> label;
    private final ToIntFunction<T> mask;
    private final BiConsumer<T, Integer> update;
    private final Function<T, String> dialogHeader;
    private final Button edit = new Button();

    private MaskCell(
        String title,
        MaskOption[] options,
        Function<T, String> label,
        ToIntFunction<T> mask,
        BiConsumer<T, Integer> update,
        Function<T, String> dialogHeader) {
      this.title = title;
      this.options = options;
      this.label = label;
      this.mask = mask;
      this.update = update;
      this.dialogHeader = dialogHeader;
      edit.setMaxWidth(Double.MAX_VALUE);
      edit.setOnAction(_ -> showEditor());
    }

    @Override
    protected void updateItem(T row, boolean empty) {
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
      T row = getItem();
      if (row == null) {
        return;
      }
      Dialog<Integer> dialog = new Dialog<>();
      dialog.setTitle("Edit " + title);
      dialog.setHeaderText(dialogHeader.apply(row));
      DialogPane pane = dialog.getDialogPane();
      pane.getStylesheets()
          .add(
              Objects.requireNonNull(FxTableColumns.class.getResource("/editor.css"))
                  .toExternalForm());
      pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

      List<CheckBox> boxes = new ArrayList<>();
      VBox content = new VBox(8);
      content.setPadding(new Insets(8, 0, 0, 0));
      int currentMask = mask.applyAsInt(row);
      for (MaskOption option : options) {
        CheckBox checkbox = new CheckBox(option.label());
        checkbox.setSelected((currentMask & option.bit()) != 0);
        boxes.add(checkbox);
        content.getChildren().add(checkbox);
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
