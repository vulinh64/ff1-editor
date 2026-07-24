package com.ff1.editor.view.music;

import static com.ff1.editor.view.ui.FxTableColumns.textColumn;

import com.ff1.editor.data.AudioResource;
import com.ff1.editor.data.EditorWorkspace;
import com.ff1.editor.service.MidiPreviewService;
import com.ff1.editor.service.MidiPreviewService.PlayResult;
import com.ff1.editor.view.FxEditorState;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

public final class FxMusicView extends BorderPane {

  private final FxEditorState state;
  private final MidiPreviewService previewService = new MidiPreviewService();
  private final ObservableList<FxMusicRowViewModel> rows =
      FXCollections.observableArrayList(
          Arrays.stream(AudioResource.values()).map(FxMusicRowViewModel::new).toList());

  public FxMusicView(FxEditorState state) {
    this.state = state;
    getStyleClass().add("music-view");
    setPadding(new Insets(8));
    setTop(toolbar());
    setCenter(table());
    state.audioReplacementSupplier(this::audioReplacements);
    state.closeHook(this::releasePreview);
    state.workspaceProperty().addListener((_, _, _) -> stopPreview());
  }

  private TableView<FxMusicRowViewModel> table() {
    TableView<FxMusicRowViewModel> table = new TableView<>(rows);
    table.setEditable(false);
    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    table
        .getColumns()
        .setAll(
            List.of(
                textColumn("Entry", FxMusicRowViewModel::entryName, 72),
                textColumn("Theme", FxMusicRowViewModel::label, 260),
                replacementColumn(),
                browseColumn(),
                clearColumn(),
                previewColumn()));
    return table;
  }

  private HBox toolbar() {
    Button stop = new Button("Stop Preview");
    stop.setOnAction(_ -> stopPreview());
    HBox controls = new HBox(8, new Label("Music"), stop);
    controls.getStyleClass().add("filter-row");
    controls.setPadding(new Insets(0, 0, 8, 0));
    return controls;
  }

  private static TableColumn<FxMusicRowViewModel, String> replacementColumn() {
    TableColumn<FxMusicRowViewModel, String> column = new TableColumn<>("Replacement File");
    column.setCellValueFactory(cell -> cell.getValue().replacementProperty());
    column.setPrefWidth(560);
    return column;
  }

  private TableColumn<FxMusicRowViewModel, FxMusicRowViewModel> browseColumn() {
    TableColumn<FxMusicRowViewModel, FxMusicRowViewModel> column = new TableColumn<>("Browse");
    column.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
    column.setCellFactory(_ -> new BrowseCell());
    column.setPrefWidth(92);
    return column;
  }

  private static TableColumn<FxMusicRowViewModel, String> clearColumn() {
    TableColumn<FxMusicRowViewModel, String> column = new TableColumn<>("Clear");
    column.setCellValueFactory(_ -> new SimpleStringProperty(""));
    column.setCellFactory(_ -> new ClearCell());
    column.setPrefWidth(82);
    return column;
  }

  private TableColumn<FxMusicRowViewModel, FxMusicRowViewModel> previewColumn() {
    TableColumn<FxMusicRowViewModel, FxMusicRowViewModel> column = new TableColumn<>("Preview");
    column.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
    column.setCellFactory(_ -> new PreviewCell());
    column.setPrefWidth(92);
    return column;
  }

  private Map<AudioResource, Path> audioReplacements() {
    Map<AudioResource, Path> replacements = new EnumMap<>(AudioResource.class);
    for (FxMusicRowViewModel row : rows) {
      if (row.changed()) {
        replacements.put(row.resource(), row.replacementPath());
      }
    }
    return replacements;
  }

  private static final class BrowseCell
      extends TableCell<FxMusicRowViewModel, FxMusicRowViewModel> {

    private final Button button = new Button("...");

    private BrowseCell() {
      button.setMaxWidth(Double.MAX_VALUE);
      button.setOnAction(_ -> chooseReplacement(getItem()));
    }

    @Override
    protected void updateItem(FxMusicRowViewModel row, boolean empty) {
      super.updateItem(row, empty);
      button.disableProperty().unbind();
      setGraphic(empty || row == null ? null : button);
      if (!empty && row != null) {
        button.disableProperty().bind(row.playingProperty());
      }
    }

    private void chooseReplacement(FxMusicRowViewModel row) {
      if (row == null) {
        return;
      }
      FileChooser chooser = new FileChooser();
      chooser.setTitle("Choose replacement for " + row.entryName());
      chooser
          .getExtensionFilters()
          .addAll(
              new FileChooser.ExtensionFilter("MIDI and audio files", "*.mid", "*.midi", "*.mmf"),
              new FileChooser.ExtensionFilter("All files", "*.*"));
      File chosen = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
      if (chosen != null) {
        row.replacement(chosen.toPath().toAbsolutePath().normalize());
      }
    }
  }

  private final class PreviewCell extends TableCell<FxMusicRowViewModel, FxMusicRowViewModel> {

    private final Button button = new Button("Preview");

    private PreviewCell() {
      button.setMaxWidth(Double.MAX_VALUE);
      button.setOnAction(_ -> preview(getItem()));
    }

    @Override
    protected void updateItem(FxMusicRowViewModel row, boolean empty) {
      super.updateItem(row, empty);
      setGraphic(empty || row == null ? null : button);
    }
  }

  private static final class ClearCell extends TableCell<FxMusicRowViewModel, String> {

    private final Button button = new Button("Clear");

    private ClearCell() {
      button.setMaxWidth(Double.MAX_VALUE);
      button.setOnAction(
          _ -> {
            FxMusicRowViewModel row = getTableRow() == null ? null : getTableRow().getItem();
            if (row != null) {
              row.replacement(null);
            }
          });
    }

    @Override
    protected void updateItem(String ignored, boolean empty) {
      super.updateItem(ignored, empty);
      button.disableProperty().unbind();
      FxMusicRowViewModel row = getTableRow() == null ? null : getTableRow().getItem();
      setGraphic(empty || row == null ? null : button);
      if (!empty && row != null) {
        button.disableProperty().bind(row.playingProperty());
      }
    }
  }

  private void preview(FxMusicRowViewModel row) {
    if (row == null) {
      return;
    }
    try {
      Path path = previewPath(row);
      PlayResult result =
          previewService.play(
              path,
              () ->
                  Platform.runLater(
                      () -> {
                        clearPlaying();
                        state.status("Finished previewing " + row.entryName() + ".");
                      }));
      if (result == PlayResult.STARTED) {
        markPlaying(row);
        state.status(
            "Previewing "
                + row.entryName()
                + " via "
                + previewService.outputName()
                + " from "
                + path
                + ".");
      }
    } catch (Exception error) {
      state.status("Music preview failed: " + error.getMessage());
      previewService.stop();
      clearPlaying();
    }
  }

  private Path previewPath(FxMusicRowViewModel row) {
    if (row.changed()) {
      return row.replacementPath();
    }
    EditorWorkspace workspace = state.workspace();
    if (workspace == null) {
      throw new IllegalStateException("Load a Final Fantasy J2ME JAR before previewing music.");
    }
    return workspace.workDir().resolve(row.entryName());
  }

  private void stopPreview() {
    if (releasePreview()) {
      state.status("Stopped music preview.");
    }
  }

  private boolean releasePreview() {
    boolean stopped = previewService.stop();
    clearPlaying();
    return stopped;
  }

  private void markPlaying(FxMusicRowViewModel playingRow) {
    for (FxMusicRowViewModel row : rows) {
      row.playing(row == playingRow);
    }
  }

  private void clearPlaying() {
    for (FxMusicRowViewModel row : rows) {
      row.playing(false);
    }
  }
}
