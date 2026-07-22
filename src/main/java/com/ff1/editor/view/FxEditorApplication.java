package com.ff1.editor.view;

import com.ff1.editor.view.heroes.FxHeroesView;
import com.ff1.editor.view.items.FxItemsView;
import com.ff1.editor.view.magic.FxMagicMatrixView;
import com.ff1.editor.view.monsters.FxMonstersView;
import com.ff1.editor.view.shops.FxShopsView;
import com.ff1.editor.view.skills.FxSkillsView;
import com.ff1.editor.view.ui.FxCommandBar;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public final class FxEditorApplication extends Application {

  public static void launchEditor(String[] args) {
    Application.launch(FxEditorApplication.class, args);
  }

  @Override
  public void start(Stage stage) {
    Path initialInputJar = chooseInitialInputJar();
    if (initialInputJar == null) {
      System.exit(0);
      return;
    }

    FxEditorState state = new FxEditorState();
    FxCommandBar commandBar = new FxCommandBar(stage, state);
    BorderPane root = new BorderPane();
    root.getStyleClass().add("app-root");
    root.setTop(commandBar);
    root.setCenter(sectionTabs(state));
    Label status = new Label();
    status.getStyleClass().add("status-bar");
    status.textProperty().bind(state.statusProperty());
    root.setBottom(status);

    Scene scene = new Scene(root, 1100, 680);
    scene
        .getStylesheets()
        .add(
            Objects.requireNonNull(FxEditorApplication.class.getResource("/editor.css"))
                .toExternalForm());
    stage.setTitle("Final Fantasy 1 J2ME Editor");
    stage.setScene(scene);
    stage.show();
    commandBar.loadInitialInputJar(initialInputJar);
  }

  private static TabPane sectionTabs(FxEditorState state) {
    TabPane tabs = new TabPane();
    Tab heroes = new Tab("Heroes", new FxHeroesView(state));
    heroes.setClosable(false);
    Tab magicMatrix = new Tab("Magic Permissions", new FxMagicMatrixView(state));
    magicMatrix.setClosable(false);
    Tab skills = new Tab("Skills", new FxSkillsView(state));
    skills.setClosable(false);
    Tab items = new Tab("Equipment / Items", new FxItemsView(state));
    items.setClosable(false);
    Tab shops = new Tab("Shops", new FxShopsView(state));
    shops.setClosable(false);
    Tab monsters = new Tab("Monsters", new FxMonstersView(state));
    monsters.setClosable(false);
    tabs.getTabs().addAll(heroes, magicMatrix, skills, items, shops, monsters);
    return tabs;
  }

  private static Path chooseInitialInputJar() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Choose Final Fantasy J2ME JAR");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR files", "*.jar"));
    File chosen = chooser.showOpenDialog(null);
    return chosen == null ? null : chosen.toPath().toAbsolutePath().normalize();
  }
}
