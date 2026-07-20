package com.ff1.editor.view.ui;

import com.ff1.editor.data.ArmorStatsEdit;
import com.ff1.editor.data.BuildResult;
import com.ff1.editor.data.EditorWorkspace;
import com.ff1.editor.data.EquipmentPermissionEdit;
import com.ff1.editor.data.HeroClassStatsEdit;
import com.ff1.editor.data.ItemPriceEdit;
import com.ff1.editor.data.MagicMatrixEdit;
import com.ff1.editor.data.MonsterStatsEdit;
import com.ff1.editor.data.PatchState;
import com.ff1.editor.data.SkillEffectEdit;
import com.ff1.editor.data.WeaponCastSpellEdit;
import com.ff1.editor.data.WeaponStatsEdit;
import com.ff1.editor.service.Cp0ChunkTable;
import com.ff1.editor.service.EditorLoadService;
import com.ff1.editor.service.EditorPatchService;
import com.ff1.editor.service.ItemEquipmentDiscoveryService;
import com.ff1.editor.service.MagicMatrixDiscoveryService;
import com.ff1.editor.service.SkillDiscoveryService;
import com.ff1.editor.service.patcher.AirshipLandingClassPatcher;
import com.ff1.editor.service.patcher.AlwaysSuccessfulRunClassPatcher;
import com.ff1.editor.service.patcher.CorneliaArmorShopPatcher;
import com.ff1.editor.service.patcher.CorneliaExcaliburShopPatcher;
import com.ff1.editor.service.patcher.CorneliaWeaponShopPatcher;
import com.ff1.editor.service.patcher.CottageReviveClassPatcher;
import com.ff1.editor.service.patcher.EnemyCriticalDefenseClassPatcher;
import com.ff1.editor.service.patcher.FifteenSpellChargeCapClassPatcher;
import com.ff1.editor.service.patcher.FifteenSpellChargeGrowthPatcher;
import com.ff1.editor.service.patcher.FifteenSpellChargeRecoveryClassPatcher;
import com.ff1.editor.service.patcher.HeroClassStatsPatcher;
import com.ff1.editor.service.patcher.HeroLevelGrowthClassPatcher;
import com.ff1.editor.service.patcher.HeroMagicResistanceClassPatcher;
import com.ff1.editor.service.patcher.IntelligenceSpellDamageClassPatcher;
import com.ff1.editor.service.patcher.IntelligenceSpellHealingClassPatcher;
import com.ff1.editor.service.patcher.ItemEquipmentPatcher;
import com.ff1.editor.service.patcher.ItemPricePatcher;
import com.ff1.editor.service.patcher.MagicMatrixPatcher;
import com.ff1.editor.service.patcher.MonsterStatsPatcher;
import com.ff1.editor.service.patcher.PartyActionOrderClassPatcher;
import com.ff1.editor.service.patcher.SkillEffectPatcher;
import com.ff1.editor.service.patcher.UniversalSpellChargeClassPatcher;
import com.ff1.editor.service.patcher.UniversalSpellChargeGrowthPatcher;
import com.ff1.editor.service.patcher.WeaponAffinityDamageClassPatcher;
import com.ff1.editor.service.patcher.WeaponCastSpellPatcher;
import com.ff1.editor.view.FxEditorState;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;

public final class FxCommandBar extends VBox {

  public static final String BUILD_PATCHED_JAR_LABEL = "Build Patched JAR";
  private final Stage owner;
  private final FxEditorState state;
  private final TextField inputJar = new TextField();
  private final Button load = new Button("Load");
  private final CheckBox forceStrongLevelUps = new CheckBox("Force strong level-ups");
  private final CheckBox universalSpellChargeGrowth = new CheckBox("Universal spell-charge growth");
  private final CheckBox fifteenSpellCharges = new CheckBox("15 max spell charges");
  private final CheckBox intelligenceSpellDamage =
      new CheckBox("Damage-causing spells scale with INT");
  private final CheckBox intelligenceSpellHealing = new CheckBox("Healing spells scale with INT");
  private final CheckBox heroMagicResistance = new CheckBox("INT+STA reduce enemy spell effects");
  private final CheckBox corneliaMasamune = new CheckBox("Cornelia sells Masamune");
  private final CheckBox corneliaExcalibur = new CheckBox("Cornelia sells Excalibur");
  private final CheckBox corneliaRibbonProtectRing =
      new CheckBox("Cornelia armor shop sells Ribbon and Protect Ring");
  private final CheckBox alwaysSuccessfulRun = new CheckBox("Always successful run");
  private final CheckBox partyActionOrder = new CheckBox("Party action order");
  private final CheckBox enemyCriticalDefense = new CheckBox("Enemy crits respect party defense");
  private final CheckBox weaponAffinityDamage =
      new CheckBox("Weapon affinity damage bonus");
  private final CheckBox cottageRevive = new CheckBox("Cottage revives KO");
  private final CheckBox airshipLanding = new CheckBox("Airship lands on safe terrain");
  private final Button build = new Button(BUILD_PATCHED_JAR_LABEL);

  public FxCommandBar(Stage owner, FxEditorState state) {
    this.owner = owner;
    this.state = state;
    getStyleClass().add("command-bar");
    setPadding(new Insets(8));
    setSpacing(6);

    inputJar.setPromptText("Choose original Final Fantasy J2ME JAR...");
    Button browse = new Button("...");
    browse.setOnAction(_ -> chooseInputJar());
    load.setOnAction(_ -> loadSelectedJar());
    forceStrongLevelUps.selectedProperty().bindBidirectional(state.forceStrongLevelUpsProperty());
    universalSpellChargeGrowth
        .selectedProperty()
        .bindBidirectional(state.universalSpellChargeGrowthProperty());
    fifteenSpellCharges.selectedProperty().bindBidirectional(state.fifteenSpellChargesProperty());
    intelligenceSpellDamage
        .selectedProperty()
        .bindBidirectional(state.intelligenceSpellDamageProperty());
    intelligenceSpellHealing
        .selectedProperty()
        .bindBidirectional(state.intelligenceSpellHealingProperty());
    heroMagicResistance.selectedProperty().bindBidirectional(state.heroMagicResistanceProperty());
    corneliaMasamune.selectedProperty().bindBidirectional(state.corneliaMasamuneProperty());
    corneliaExcalibur.selectedProperty().bindBidirectional(state.corneliaExcaliburProperty());
    corneliaRibbonProtectRing
        .selectedProperty()
        .bindBidirectional(state.corneliaRibbonProtectRingProperty());
    alwaysSuccessfulRun.selectedProperty().bindBidirectional(state.alwaysSuccessfulRunProperty());
    partyActionOrder.selectedProperty().bindBidirectional(state.partyActionOrderProperty());
    enemyCriticalDefense.selectedProperty().bindBidirectional(state.enemyCriticalDefenseProperty());
    weaponAffinityDamage.selectedProperty().bindBidirectional(state.weaponAffinityDamageProperty());
    cottageRevive.selectedProperty().bindBidirectional(state.cottageReviveProperty());
    airshipLanding.selectedProperty().bindBidirectional(state.airshipLandingProperty());
    forceStrongLevelUps.setDisable(true);
    universalSpellChargeGrowth.setDisable(true);
    fifteenSpellCharges.setDisable(true);
    intelligenceSpellDamage.setDisable(true);
    intelligenceSpellHealing.setDisable(true);
    heroMagicResistance.setDisable(true);
    corneliaMasamune.setDisable(true);
    corneliaExcalibur.setDisable(true);
    corneliaRibbonProtectRing.setDisable(true);
    alwaysSuccessfulRun.setDisable(true);
    partyActionOrder.setDisable(true);
    enemyCriticalDefense.setDisable(true);
    weaponAffinityDamage.setDisable(true);
    cottageRevive.setDisable(true);
    airshipLanding.setDisable(true);
    build.setOnAction(_ -> showPatchOptionsDialog());

    HBox.setHgrow(inputJar, Priority.ALWAYS);
    HBox inputRow = new HBox(8, new Label("Input JAR"), inputJar, browse, load);
    inputRow.getStyleClass().add("command-row");
    HBox patchRow = new HBox(8, build);
    patchRow.getStyleClass().add("global-patch-row");
    getChildren().addAll(inputRow, patchRow);
  }

  public void loadInitialInputJar(Path selectedJar) {
    inputJar.setText(selectedJar.toString());
    loadSelectedJar();
  }

  private void chooseInputJar() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Choose Final Fantasy J2ME JAR");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR files", "*.jar"));
    File chosen = chooser.showOpenDialog(owner);
    if (chosen != null) {
      inputJar.setText(chosen.toPath().toString());
    }
  }

  private void loadSelectedJar() {
    String input = inputJar.getText().trim();
    if (input.isEmpty()) {
      state.status("Choose a Final Fantasy J2ME JAR to begin.");
      return;
    }
    Path selected = Path.of(input);
    Task<EditorWorkspace> task =
        new Task<>() {
          @Override
          protected EditorWorkspace call() throws Exception {
            return new EditorLoadService().load(selected);
          }
        };
    load.disableProperty().bind(task.runningProperty());
    state.status("Loading " + selected + "...");
    task.setOnSucceeded(
        _ -> {
          EditorWorkspace workspace = task.getValue();
          state.workspace(workspace);
          updateGlobalPatchControls(workspace);
          state.status(
              "Loaded %s into %s. Heroes are ready for inspection. Strong level-ups: %s. Universal charges: %s. 15 charges: %s. INT damage: %s. INT healing: %s. Hero magic resistance: %s. Cornelia Masamune: %s. Cornelia Excalibur: %s. Cornelia Ribbon/Protect Ring: %s. Run patch: %s. Action order: %s. Enemy crit defense: %s. Weapon affinity damage: %s. Cottage revive: %s. Airship landing: %s."
                  .formatted(
                      workspace.inputJar().getFileName(),
                      workspace.workDir(),
                      patchStateLabel(workspace.strongLevelUpsState()),
                      patchStateLabel(workspace.universalSpellChargesState()),
                      patchStateLabel(workspace.fifteenSpellChargesState()),
                      patchStateLabel(workspace.intelligenceSpellDamageState()),
                      patchStateLabel(workspace.intelligenceSpellHealingState()),
                      patchStateLabel(workspace.heroMagicResistanceState()),
                      patchStateLabel(workspace.corneliaMasamuneState()),
                      patchStateLabel(workspace.corneliaExcaliburState()),
                      patchStateLabel(workspace.corneliaRibbonProtectRingState()),
                      patchStateLabel(workspace.alwaysSuccessfulRunState()),
                      patchStateLabel(workspace.partyActionOrderState()),
                      patchStateLabel(workspace.enemyCriticalDefenseState()),
                      patchStateLabel(workspace.weaponAffinityDamageState()),
                      patchStateLabel(workspace.cottageReviveState()),
                      patchStateLabel(workspace.airshipLandingState())));
          load.disableProperty().unbind();
          load.setDisable(false);
        });
    task.setOnFailed(
        _ -> {
          Throwable error = task.getException();
          state.status("Load failed: " + error.getMessage());
          FxDialogs.showError("Unable to load Final Fantasy JAR", error);
          load.disableProperty().unbind();
          load.setDisable(false);
        });
    Thread thread = new Thread(task, "ff1-fx-load");
    thread.setDaemon(true);
    thread.start();
  }

  private void updateGlobalPatchControls(EditorWorkspace workspace) {
    if (workspace == null) {
      state.forceStrongLevelUps(false);
      state.universalSpellChargeGrowth(false);
      state.fifteenSpellCharges(false);
      state.intelligenceSpellDamage(false);
      state.intelligenceSpellHealing(false);
      state.heroMagicResistance(false);
      state.corneliaMasamune(false);
      state.corneliaExcalibur(false);
      state.corneliaRibbonProtectRing(false);
      state.alwaysSuccessfulRun(false);
      state.partyActionOrder(false);
      state.enemyCriticalDefense(false);
      state.weaponAffinityDamage(false);
      state.cottageRevive(false);
      state.airshipLanding(false);
      forceStrongLevelUps.setDisable(true);
      universalSpellChargeGrowth.setDisable(true);
      fifteenSpellCharges.setDisable(true);
      intelligenceSpellDamage.setDisable(true);
      intelligenceSpellHealing.setDisable(true);
      heroMagicResistance.setDisable(true);
      corneliaMasamune.setDisable(true);
      corneliaExcalibur.setDisable(true);
      corneliaRibbonProtectRing.setDisable(true);
      alwaysSuccessfulRun.setDisable(true);
      partyActionOrder.setDisable(true);
      enemyCriticalDefense.setDisable(true);
      weaponAffinityDamage.setDisable(true);
      cottageRevive.setDisable(true);
      airshipLanding.setDisable(true);
      return;
    }
    switch (workspace.strongLevelUpsState()) {
      case PATCHED -> {
        state.forceStrongLevelUps(true);
        forceStrongLevelUps.setDisable(true);
      }
      case ORIGINAL -> {
        state.forceStrongLevelUps(false);
        forceStrongLevelUps.setDisable(false);
      }
      case UNKNOWN -> {
        state.forceStrongLevelUps(false);
        forceStrongLevelUps.setDisable(true);
      }
    }
    switch (workspace.universalSpellChargesState()) {
      case PATCHED -> {
        state.universalSpellChargeGrowth(true);
        universalSpellChargeGrowth.setDisable(true);
      }
      case ORIGINAL -> {
        state.universalSpellChargeGrowth(false);
        universalSpellChargeGrowth.setDisable(false);
      }
      case UNKNOWN -> {
        state.universalSpellChargeGrowth(false);
        universalSpellChargeGrowth.setDisable(true);
      }
    }
    switch (workspace.fifteenSpellChargesState()) {
      case PATCHED -> {
        state.fifteenSpellCharges(true);
        fifteenSpellCharges.setDisable(true);
      }
      case ORIGINAL -> {
        state.fifteenSpellCharges(false);
        fifteenSpellCharges.setDisable(false);
      }
      case UNKNOWN -> {
        state.fifteenSpellCharges(false);
        fifteenSpellCharges.setDisable(true);
      }
    }
    switch (workspace.intelligenceSpellDamageState()) {
      case PATCHED -> {
        state.intelligenceSpellDamage(true);
        intelligenceSpellDamage.setDisable(true);
      }
      case ORIGINAL -> {
        state.intelligenceSpellDamage(false);
        intelligenceSpellDamage.setDisable(false);
      }
      case UNKNOWN -> {
        state.intelligenceSpellDamage(false);
        intelligenceSpellDamage.setDisable(true);
      }
    }
    switch (workspace.intelligenceSpellHealingState()) {
      case PATCHED -> {
        state.intelligenceSpellHealing(true);
        intelligenceSpellHealing.setDisable(true);
      }
      case ORIGINAL -> {
        state.intelligenceSpellHealing(false);
        intelligenceSpellHealing.setDisable(false);
      }
      case UNKNOWN -> {
        state.intelligenceSpellHealing(false);
        intelligenceSpellHealing.setDisable(true);
      }
    }
    switch (workspace.heroMagicResistanceState()) {
      case PATCHED -> {
        state.heroMagicResistance(true);
        heroMagicResistance.setDisable(true);
      }
      case ORIGINAL -> {
        state.heroMagicResistance(false);
        heroMagicResistance.setDisable(false);
      }
      case UNKNOWN -> {
        state.heroMagicResistance(false);
        heroMagicResistance.setDisable(true);
      }
    }
    switch (workspace.corneliaMasamuneState()) {
      case PATCHED -> {
        state.corneliaMasamune(true);
        corneliaMasamune.setDisable(true);
      }
      case ORIGINAL -> {
        state.corneliaMasamune(false);
        corneliaMasamune.setDisable(false);
      }
      case UNKNOWN -> {
        state.corneliaMasamune(false);
        corneliaMasamune.setDisable(true);
      }
    }
    switch (workspace.corneliaExcaliburState()) {
      case PATCHED -> {
        state.corneliaExcalibur(true);
        corneliaExcalibur.setDisable(true);
      }
      case ORIGINAL -> {
        state.corneliaExcalibur(false);
        corneliaExcalibur.setDisable(false);
      }
      case UNKNOWN -> {
        state.corneliaExcalibur(false);
        corneliaExcalibur.setDisable(true);
      }
    }
    switch (workspace.corneliaRibbonProtectRingState()) {
      case PATCHED -> {
        state.corneliaRibbonProtectRing(true);
        corneliaRibbonProtectRing.setDisable(true);
      }
      case ORIGINAL -> {
        state.corneliaRibbonProtectRing(false);
        corneliaRibbonProtectRing.setDisable(false);
      }
      case UNKNOWN -> {
        state.corneliaRibbonProtectRing(false);
        corneliaRibbonProtectRing.setDisable(true);
      }
    }
    switch (workspace.alwaysSuccessfulRunState()) {
      case PATCHED -> {
        state.alwaysSuccessfulRun(true);
        alwaysSuccessfulRun.setDisable(true);
      }
      case ORIGINAL -> {
        state.alwaysSuccessfulRun(false);
        alwaysSuccessfulRun.setDisable(false);
      }
      case UNKNOWN -> {
        state.alwaysSuccessfulRun(false);
        alwaysSuccessfulRun.setDisable(true);
      }
    }
    switch (workspace.partyActionOrderState()) {
      case PATCHED -> {
        state.partyActionOrder(true);
        partyActionOrder.setDisable(true);
      }
      case ORIGINAL -> {
        state.partyActionOrder(false);
        partyActionOrder.setDisable(false);
      }
      case UNKNOWN -> {
        state.partyActionOrder(false);
        partyActionOrder.setDisable(true);
      }
    }
    switch (workspace.enemyCriticalDefenseState()) {
      case PATCHED -> {
        state.enemyCriticalDefense(true);
        enemyCriticalDefense.setDisable(true);
      }
      case ORIGINAL -> {
        state.enemyCriticalDefense(false);
        enemyCriticalDefense.setDisable(false);
      }
      case UNKNOWN -> {
        state.enemyCriticalDefense(false);
        enemyCriticalDefense.setDisable(true);
      }
    }
    switch (workspace.weaponAffinityDamageState()) {
      case PATCHED -> {
        state.weaponAffinityDamage(true);
        weaponAffinityDamage.setDisable(true);
      }
      case ORIGINAL -> {
        state.weaponAffinityDamage(false);
        weaponAffinityDamage.setDisable(false);
      }
      case UNKNOWN -> {
        state.weaponAffinityDamage(false);
        weaponAffinityDamage.setDisable(true);
      }
    }
    switch (workspace.cottageReviveState()) {
      case PATCHED -> {
        state.cottageRevive(true);
        cottageRevive.setDisable(true);
      }
      case ORIGINAL -> {
        state.cottageRevive(false);
        cottageRevive.setDisable(false);
      }
      case UNKNOWN -> {
        state.cottageRevive(false);
        cottageRevive.setDisable(true);
      }
    }
    switch (workspace.airshipLandingState()) {
      case PATCHED -> {
        state.airshipLanding(true);
        airshipLanding.setDisable(true);
      }
      case ORIGINAL -> {
        state.airshipLanding(false);
        airshipLanding.setDisable(false);
      }
      case UNKNOWN -> {
        state.airshipLanding(false);
        airshipLanding.setDisable(true);
      }
    }
  }

  private void showPatchOptionsDialog() {
    EditorWorkspace workspace = state.workspace();
    if (workspace == null) {
      state.status("Load a Final Fantasy J2ME JAR before building a patch.");
      return;
    }

    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle(BUILD_PATCHED_JAR_LABEL);
    dialog.setHeaderText("Choose optional global patches");
    DialogPane pane = dialog.getDialogPane();
    pane.getStylesheets()
        .add(
            Objects.requireNonNull(FxCommandBar.class.getResource("/editor.css")).toExternalForm());
    pane.getStyleClass().add("patch-dialog");
    pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    Button okButton = (Button) pane.lookupButton(ButtonType.OK);
    okButton.setText(BUILD_PATCHED_JAR_LABEL);

    CheckBox strongLevelUpsOption =
        dialogCheckBox(forceStrongLevelUps, workspace.strongLevelUpsState());
    CheckBox universalChargesOption =
        dialogCheckBox(universalSpellChargeGrowth, workspace.universalSpellChargesState());
    CheckBox fifteenChargesOption =
        dialogCheckBox(fifteenSpellCharges, workspace.fifteenSpellChargesState());
    CheckBox intelligenceSpellDamageOption =
        dialogCheckBox(intelligenceSpellDamage, workspace.intelligenceSpellDamageState());
    CheckBox intelligenceSpellHealingOption =
        dialogCheckBox(intelligenceSpellHealing, workspace.intelligenceSpellHealingState());
    CheckBox heroMagicResistanceOption =
        dialogCheckBox(heroMagicResistance, workspace.heroMagicResistanceState());
    CheckBox corneliaMasamuneOption =
        dialogCheckBox(corneliaMasamune, workspace.corneliaMasamuneState());
    CheckBox corneliaExcaliburOption =
        dialogCheckBox(corneliaExcalibur, workspace.corneliaExcaliburState());
    CheckBox corneliaRibbonProtectRingOption =
        dialogCheckBox(corneliaRibbonProtectRing, workspace.corneliaRibbonProtectRingState());
    CheckBox alwaysSuccessfulRunOption =
        dialogCheckBox(alwaysSuccessfulRun, workspace.alwaysSuccessfulRunState());
    CheckBox partyActionOrderOption =
        dialogCheckBox(partyActionOrder, workspace.partyActionOrderState());
    CheckBox enemyCriticalDefenseOption =
        dialogCheckBox(enemyCriticalDefense, workspace.enemyCriticalDefenseState());
    CheckBox weaponAffinityDamageOption =
        dialogCheckBox(weaponAffinityDamage, workspace.weaponAffinityDamageState());
    CheckBox cottageReviveOption = dialogCheckBox(cottageRevive, workspace.cottageReviveState());
    CheckBox airshipLandingOption = dialogCheckBox(airshipLanding, workspace.airshipLandingState());
    VBox options =
        new VBox(
            8,
            strongLevelUpsOption,
            universalChargesOption,
            fifteenChargesOption,
            intelligenceSpellDamageOption,
            intelligenceSpellHealingOption,
            heroMagicResistanceOption,
            corneliaMasamuneOption,
            corneliaExcaliburOption,
            corneliaRibbonProtectRingOption,
            alwaysSuccessfulRunOption,
            partyActionOrderOption,
            enemyCriticalDefenseOption,
            weaponAffinityDamageOption,
            cottageReviveOption,
            airshipLandingOption);
    options.setPadding(new Insets(8, 0, 0, 0));
    pane.setContent(options);

    boolean hasDataEdits =
        !state.heroStatsEdits().isEmpty()
            || !state.magicMatrixEdits().isEmpty()
            || !state.equipmentPermissionEdits().isEmpty()
            || !state.skillEffectEdits().isEmpty()
            || !state.itemPriceEdits().isEmpty()
            || !state.weaponCastSpellEdits().isEmpty()
            || !state.weaponStatsEdits().isEmpty()
            || !state.armorStatsEdits().isEmpty()
            || !state.monsterStatsEdits().isEmpty();
    BooleanBinding hasBuildSelection =
        Bindings.createBooleanBinding(
            () ->
                hasDataEdits
                    || selectedOriginal(strongLevelUpsOption, workspace.strongLevelUpsState()) > 0
                    || selectedOriginal(
                            universalChargesOption, workspace.universalSpellChargesState())
                        > 0
                    || selectedOriginal(fifteenChargesOption, workspace.fifteenSpellChargesState())
                        > 0
                    || selectedOriginal(
                            intelligenceSpellDamageOption, workspace.intelligenceSpellDamageState())
                        > 0
                    || selectedOriginal(
                            intelligenceSpellHealingOption,
                            workspace.intelligenceSpellHealingState())
                        > 0
                    || selectedOriginal(
                            heroMagicResistanceOption, workspace.heroMagicResistanceState())
                        > 0
                    || selectedOriginal(corneliaMasamuneOption, workspace.corneliaMasamuneState())
                        > 0
                    || selectedOriginal(corneliaExcaliburOption, workspace.corneliaExcaliburState())
                        > 0
                    || selectedOriginal(
                            corneliaRibbonProtectRingOption,
                            workspace.corneliaRibbonProtectRingState())
                        > 0
                    || selectedOriginal(
                            alwaysSuccessfulRunOption, workspace.alwaysSuccessfulRunState())
                        > 0
                    || selectedOriginal(partyActionOrderOption, workspace.partyActionOrderState())
                        > 0
                    || selectedOriginal(
                            enemyCriticalDefenseOption, workspace.enemyCriticalDefenseState())
                        > 0
                    || selectedOriginal(
                            weaponAffinityDamageOption, workspace.weaponAffinityDamageState())
                        > 0
                    || selectedOriginal(cottageReviveOption, workspace.cottageReviveState()) > 0
                    || selectedOriginal(airshipLandingOption, workspace.airshipLandingState()) > 0,
            strongLevelUpsOption.selectedProperty(),
            universalChargesOption.selectedProperty(),
            fifteenChargesOption.selectedProperty(),
            intelligenceSpellDamageOption.selectedProperty(),
            intelligenceSpellHealingOption.selectedProperty(),
            heroMagicResistanceOption.selectedProperty(),
            corneliaMasamuneOption.selectedProperty(),
            corneliaExcaliburOption.selectedProperty(),
            corneliaRibbonProtectRingOption.selectedProperty(),
            alwaysSuccessfulRunOption.selectedProperty(),
            partyActionOrderOption.selectedProperty(),
            enemyCriticalDefenseOption.selectedProperty(),
            weaponAffinityDamageOption.selectedProperty(),
            cottageReviveOption.selectedProperty(),
            airshipLandingOption.selectedProperty());
    okButton.disableProperty().bind(hasBuildSelection.not());

    boolean shouldBuild = dialog.showAndWait().filter(ButtonType.OK::equals).isPresent();
    okButton.disableProperty().unbind();
    if (!shouldBuild) {
      return;
    }
    state.forceStrongLevelUps(strongLevelUpsOption.isSelected());
    state.universalSpellChargeGrowth(universalChargesOption.isSelected());
    state.fifteenSpellCharges(fifteenChargesOption.isSelected());
    state.intelligenceSpellDamage(intelligenceSpellDamageOption.isSelected());
    state.intelligenceSpellHealing(intelligenceSpellHealingOption.isSelected());
    state.heroMagicResistance(heroMagicResistanceOption.isSelected());
    state.corneliaMasamune(corneliaMasamuneOption.isSelected());
    state.corneliaExcalibur(corneliaExcaliburOption.isSelected());
    state.corneliaRibbonProtectRing(corneliaRibbonProtectRingOption.isSelected());
    state.alwaysSuccessfulRun(alwaysSuccessfulRunOption.isSelected());
    state.partyActionOrder(partyActionOrderOption.isSelected());
    state.enemyCriticalDefense(enemyCriticalDefenseOption.isSelected());
    state.weaponAffinityDamage(weaponAffinityDamageOption.isSelected());
    state.cottageRevive(cottageReviveOption.isSelected());
    state.airshipLanding(airshipLandingOption.isSelected());
    buildPatch();
  }

  private CheckBox dialogCheckBox(CheckBox source, PatchState patchState) {
    CheckBox checkbox = new CheckBox(source.getText());
    checkbox.setSelected(source.isSelected());
    checkbox.setDisable(source.isDisable());
    checkbox.setTooltip(new Tooltip(optionTooltip(patchState)));
    return checkbox;
  }

  private static int selectedOriginal(CheckBox checkbox, PatchState patchState) {
    return checkbox.isSelected() && patchState == PatchState.ORIGINAL ? 1 : 0;
  }

  private void buildPatch() {
    EditorWorkspace workspace = state.workspace();
    if (workspace == null) {
      state.status("Load a Final Fantasy J2ME JAR before building a patch.");
      return;
    }

    List<HeroClassStatsEdit> heroEdits = state.heroStatsEdits();
    List<MagicMatrixEdit> magicEdits = state.magicMatrixEdits();
    List<EquipmentPermissionEdit> equipmentPermissionEdits = state.equipmentPermissionEdits();
    List<SkillEffectEdit> skillEdits = state.skillEffectEdits();
    List<ItemPriceEdit> itemPriceEdits = state.itemPriceEdits();
    List<WeaponCastSpellEdit> weaponCastEdits = state.weaponCastSpellEdits();
    List<WeaponStatsEdit> weaponStatsEdits = state.weaponStatsEdits();
    List<ArmorStatsEdit> armorStatsEdits = state.armorStatsEdits();
    List<MonsterStatsEdit> monsterStatsEdits = state.monsterStatsEdits();
    boolean growthPatch =
        state.forceStrongLevelUps() && workspace.strongLevelUpsState() == PatchState.ORIGINAL;
    boolean universalChargesPatch =
        state.universalSpellChargeGrowth()
            && workspace.universalSpellChargesState() == PatchState.ORIGINAL;
    boolean fifteenChargesPatch =
        state.fifteenSpellCharges() && workspace.fifteenSpellChargesState() == PatchState.ORIGINAL;
    boolean intelligenceSpellDamagePatch =
        state.intelligenceSpellDamage()
            && workspace.intelligenceSpellDamageState() == PatchState.ORIGINAL;
    boolean intelligenceSpellHealingPatch =
        state.intelligenceSpellHealing()
            && workspace.intelligenceSpellHealingState() == PatchState.ORIGINAL;
    boolean heroMagicResistancePatch =
        state.heroMagicResistance() && workspace.heroMagicResistanceState() == PatchState.ORIGINAL;
    boolean corneliaMasamunePatch =
        state.corneliaMasamune() && workspace.corneliaMasamuneState() == PatchState.ORIGINAL;
    boolean corneliaExcaliburPatch =
        state.corneliaExcalibur() && workspace.corneliaExcaliburState() == PatchState.ORIGINAL;
    boolean corneliaRibbonProtectRingPatch =
        state.corneliaRibbonProtectRing()
            && workspace.corneliaRibbonProtectRingState() == PatchState.ORIGINAL;
    boolean alwaysSuccessfulRunPatch =
        state.alwaysSuccessfulRun() && workspace.alwaysSuccessfulRunState() == PatchState.ORIGINAL;
    boolean partyActionOrderPatch =
        state.partyActionOrder() && workspace.partyActionOrderState() == PatchState.ORIGINAL;
    boolean enemyCriticalDefensePatch =
        state.enemyCriticalDefense()
            && workspace.enemyCriticalDefenseState() == PatchState.ORIGINAL;
    boolean weaponAffinityDamagePatch =
        state.weaponAffinityDamage()
            && workspace.weaponAffinityDamageState() == PatchState.ORIGINAL;
    boolean cottageRevivePatch =
        state.cottageRevive() && workspace.cottageReviveState() == PatchState.ORIGINAL;
    boolean airshipLandingPatch =
        state.airshipLanding() && workspace.airshipLandingState() == PatchState.ORIGINAL;
    if (heroEdits.isEmpty()
        && magicEdits.isEmpty()
        && equipmentPermissionEdits.isEmpty()
        && skillEdits.isEmpty()
        && itemPriceEdits.isEmpty()
        && weaponCastEdits.isEmpty()
        && weaponStatsEdits.isEmpty()
        && armorStatsEdits.isEmpty()
        && monsterStatsEdits.isEmpty()
        && !growthPatch
        && !universalChargesPatch
        && !fifteenChargesPatch
        && !intelligenceSpellDamagePatch
        && !intelligenceSpellHealingPatch
        && !heroMagicResistancePatch
        && !corneliaMasamunePatch
        && !corneliaExcaliburPatch
        && !corneliaRibbonProtectRingPatch
        && !alwaysSuccessfulRunPatch
        && !partyActionOrderPatch
        && !enemyCriticalDefensePatch
        && !weaponAffinityDamagePatch
        && !cottageRevivePatch
        && !airshipLandingPatch) {
      state.status("No patch edits selected.");
      return;
    }

    Task<BuildResult> task =
        new Task<>() {
          @Override
          protected BuildResult call() throws Exception {
            Map<String, byte[]> replacements = new HashMap<>();
            if (!heroEdits.isEmpty()
                || !magicEdits.isEmpty()
                || !equipmentPermissionEdits.isEmpty()
                || !skillEdits.isEmpty()
                || !itemPriceEdits.isEmpty()
                || !weaponCastEdits.isEmpty()
                || !weaponStatsEdits.isEmpty()
                || !armorStatsEdits.isEmpty()
                || !monsterStatsEdits.isEmpty()
                || universalChargesPatch
                || fifteenChargesPatch
                || corneliaMasamunePatch
                || corneliaExcaliburPatch
                || corneliaRibbonProtectRingPatch) {
              byte[] cp0 =
                  Files.readAllBytes(workspace.workDir().resolve(HeroClassStatsPatcher.ENTRY_NAME));
              for (HeroClassStatsEdit edit : heroEdits) {
                HeroClassStatsPatcher.apply(cp0, edit);
              }
              if (!magicEdits.isEmpty()) {
                Cp0ChunkTable table = new Cp0ChunkTable(cp0);
                int chunkOffset = table.chunkOffset(MagicMatrixDiscoveryService.SPELL_CHUNK_INDEX);
                for (MagicMatrixEdit edit : magicEdits) {
                  MagicMatrixPatcher.apply(cp0, chunkOffset, edit);
                }
              }
              for (EquipmentPermissionEdit edit : equipmentPermissionEdits) {
                ItemEquipmentPatcher.applyPermission(cp0, edit);
              }
              for (WeaponStatsEdit edit : weaponStatsEdits) {
                ItemEquipmentPatcher.applyWeaponStats(cp0, edit);
              }
              for (ArmorStatsEdit edit : armorStatsEdits) {
                ItemEquipmentPatcher.applyArmorStats(cp0, edit);
              }
              for (MonsterStatsEdit edit : monsterStatsEdits) {
                MonsterStatsPatcher.apply(cp0, edit);
              }
              if (!skillEdits.isEmpty()) {
                Cp0ChunkTable table = new Cp0ChunkTable(cp0);
                int chunkOffset = table.chunkOffset(SkillDiscoveryService.SPELL_CHUNK_INDEX);
                for (SkillEffectEdit edit : skillEdits) {
                  SkillEffectPatcher.apply(cp0, chunkOffset, edit);
                }
              }
              if (!itemPriceEdits.isEmpty()) {
                Cp0ChunkTable table = new Cp0ChunkTable(cp0);
                int chunkOffset =
                    table.chunkOffset(ItemEquipmentDiscoveryService.ITEM_METADATA_CHUNK_INDEX);
                for (ItemPriceEdit edit : itemPriceEdits) {
                  ItemPricePatcher.apply(cp0, chunkOffset, edit);
                }
              }
              if (!weaponCastEdits.isEmpty()) {
                Cp0ChunkTable table = new Cp0ChunkTable(cp0);
                int chunkOffset =
                    table.chunkOffset(ItemEquipmentDiscoveryService.WEAPON_CHUNK_INDEX);
                for (WeaponCastSpellEdit edit : weaponCastEdits) {
                  WeaponCastSpellPatcher.apply(cp0, chunkOffset, edit);
                }
              }
              if (universalChargesPatch) {
                UniversalSpellChargeGrowthPatcher.apply(cp0);
              }
              if (fifteenChargesPatch) {
                FifteenSpellChargeGrowthPatcher.apply(cp0);
              }
              if (corneliaMasamunePatch) {
                CorneliaWeaponShopPatcher.apply(cp0);
              }
              if (corneliaExcaliburPatch) {
                CorneliaExcaliburShopPatcher.apply(cp0);
              }
              if (corneliaRibbonProtectRingPatch) {
                CorneliaArmorShopPatcher.apply(cp0);
              }
              replacements.put(HeroClassStatsPatcher.ENTRY_NAME, cp0);
            }
            if (growthPatch
                || universalChargesPatch
                || fifteenChargesPatch
                || intelligenceSpellDamagePatch
                || intelligenceSpellHealingPatch
                || heroMagicResistancePatch
                || alwaysSuccessfulRunPatch
                || partyActionOrderPatch
                || enemyCriticalDefensePatch
                || weaponAffinityDamagePatch) {
              byte[] gClass =
                  Files.readAllBytes(
                      workspace.workDir().resolve(HeroLevelGrowthClassPatcher.ENTRY_NAME));
              if (growthPatch) {
                gClass = HeroLevelGrowthClassPatcher.apply(gClass);
              }
              if (universalChargesPatch) {
                gClass = UniversalSpellChargeClassPatcher.apply(gClass);
              }
              if (fifteenChargesPatch) {
                gClass = UniversalSpellChargeClassPatcher.apply(gClass);
                gClass = FifteenSpellChargeCapClassPatcher.apply(gClass);
              }
              if (intelligenceSpellHealingPatch) {
                gClass = IntelligenceSpellHealingClassPatcher.apply(gClass);
              }
              if (intelligenceSpellDamagePatch) {
                gClass = IntelligenceSpellDamageClassPatcher.apply(gClass);
              }
              if (heroMagicResistancePatch) {
                gClass = HeroMagicResistanceClassPatcher.apply(gClass);
              }
              if (alwaysSuccessfulRunPatch) {
                gClass = AlwaysSuccessfulRunClassPatcher.apply(gClass);
              }
              if (partyActionOrderPatch) {
                gClass = PartyActionOrderClassPatcher.apply(gClass);
              }
              if (enemyCriticalDefensePatch) {
                gClass = EnemyCriticalDefenseClassPatcher.apply(gClass);
              }
              if (weaponAffinityDamagePatch) {
                gClass = WeaponAffinityDamageClassPatcher.apply(gClass);
              }
              replacements.put(HeroLevelGrowthClassPatcher.ENTRY_NAME, gClass);
            }
            if (fifteenChargesPatch || cottageRevivePatch || airshipLandingPatch) {
              byte[] iClass =
                  Files.readAllBytes(
                      workspace
                          .workDir()
                          .resolve(FifteenSpellChargeRecoveryClassPatcher.ENTRY_NAME));
              if (fifteenChargesPatch) {
                iClass = FifteenSpellChargeRecoveryClassPatcher.apply(iClass);
              }
              if (cottageRevivePatch) {
                iClass = CottageReviveClassPatcher.apply(iClass);
              }
              if (airshipLandingPatch) {
                iClass = AirshipLandingClassPatcher.apply(iClass);
              }
              replacements.put(FifteenSpellChargeRecoveryClassPatcher.ENTRY_NAME, iClass);
            }
            return new EditorPatchService().buildPatch(workspace, replacements);
          }
        };

    build.disableProperty().bind(task.runningProperty());
    load.disableProperty().bind(task.runningProperty());
    state.status(
        "Building patched JAR with %d hero edit(s), %d magic matrix edit(s), "
            + "%d equipment matrix edit(s), %d skill edit(s), %d item price edit(s), "
            + "%d weapon cast edit(s), %d weapon stat edit(s), %d armor stat edit(s), "
            + "%d monster stat edit(s)%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s..."
                .formatted(
                    heroEdits.size(),
                    magicEdits.size(),
                    equipmentPermissionEdits.size(),
                    skillEdits.size(),
                    itemPriceEdits.size(),
                    weaponCastEdits.size(),
                    weaponStatsEdits.size(),
                    armorStatsEdits.size(),
                    monsterStatsEdits.size(),
                    growthPatch ? ", strong level-ups" : StringUtils.EMPTY,
                    universalChargesPatch ? ", universal spell-charge growth" : StringUtils.EMPTY,
                    fifteenChargesPatch ? ", 15 max spell charges" : StringUtils.EMPTY,
                    intelligenceSpellDamagePatch
                        ? ", Damage-causing spells scale with INT"
                        : StringUtils.EMPTY,
                    intelligenceSpellHealingPatch
                        ? ", healing spells scale with INT"
                        : StringUtils.EMPTY,
                    heroMagicResistancePatch
                        ? ", INT+STA reduce enemy spell effects"
                        : StringUtils.EMPTY,
                    corneliaMasamunePatch ? ", Cornelia Masamune" : StringUtils.EMPTY,
                    corneliaExcaliburPatch ? ", Cornelia Excalibur" : StringUtils.EMPTY,
                    corneliaRibbonProtectRingPatch
                        ? ", Cornelia Ribbon and Protect Ring"
                        : StringUtils.EMPTY,
                    alwaysSuccessfulRunPatch ? ", always-successful run" : StringUtils.EMPTY,
                    partyActionOrderPatch ? ", party action order" : StringUtils.EMPTY,
                    enemyCriticalDefensePatch
                        ? ", enemy crits respect party defense"
                        : StringUtils.EMPTY,
                    weaponAffinityDamagePatch
                        ? ", weapon affinity damage bonus"
                        : StringUtils.EMPTY,
                    cottageRevivePatch ? ", Cottage revive" : StringUtils.EMPTY,
                    airshipLandingPatch ? ", and airship landing" : StringUtils.EMPTY));
    task.setOnSucceeded(
        _ -> {
          BuildResult result = task.getValue();
          state.status("Wrote %s (%s)".formatted(result.outputJar(), result.summary()));
          unbindBuildControls();
        });
    task.setOnFailed(
        _ -> {
          Throwable error = task.getException();
          state.status("Patch build failed: " + error.getMessage());
          FxDialogs.showError("Unable to build patched JAR", error);
          unbindBuildControls();
        });
    Thread thread = new Thread(task, "ff1-fx-global-patch");
    thread.setDaemon(true);
    thread.start();
  }

  private void unbindBuildControls() {
    build.disableProperty().unbind();
    load.disableProperty().unbind();
    build.setDisable(false);
    load.setDisable(false);
  }

  private static String patchStateLabel(PatchState state) {
    return switch (state) {
      case ORIGINAL -> "available";
      case PATCHED -> "already patched";
      case UNKNOWN -> "unavailable for this class layout";
    };
  }

  private static String optionTooltip(PatchState state) {
    return switch (state) {
      case ORIGINAL -> "available";
      case PATCHED -> "already patched";
      case UNKNOWN -> "unsupported layout";
    };
  }
}
