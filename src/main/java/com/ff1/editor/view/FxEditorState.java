package com.ff1.editor.view;

import com.ff1.editor.data.EditorWorkspace;
import com.ff1.editor.data.EquipmentPermissionEdit;
import com.ff1.editor.data.HeroClassStatsEdit;
import com.ff1.editor.data.ItemPriceEdit;
import com.ff1.editor.data.MagicMatrixEdit;
import com.ff1.editor.data.SkillEffectEdit;
import com.ff1.editor.data.WeaponCastSpellEdit;
import java.util.List;
import java.util.function.Supplier;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class FxEditorState {

  private final ObjectProperty<EditorWorkspace> workspace = new SimpleObjectProperty<>();
  private final StringProperty status =
      new SimpleStringProperty("Choose a Final Fantasy J2ME JAR to begin.");
  private final BooleanProperty forceStrongLevelUps = new SimpleBooleanProperty(false);
  private final BooleanProperty universalSpellChargeGrowth = new SimpleBooleanProperty(false);
  private final BooleanProperty fifteenSpellCharges = new SimpleBooleanProperty(false);
  private final BooleanProperty intelligenceSpellDamage = new SimpleBooleanProperty(false);
  private final BooleanProperty corneliaMasamune = new SimpleBooleanProperty(false);
  private final BooleanProperty corneliaExcalibur = new SimpleBooleanProperty(false);
  private final BooleanProperty alwaysSuccessfulRun = new SimpleBooleanProperty(false);
  private final BooleanProperty partyActionOrder = new SimpleBooleanProperty(false);
  private final BooleanProperty cottageRevive = new SimpleBooleanProperty(false);
  private final BooleanProperty airshipLanding = new SimpleBooleanProperty(false);
  private Supplier<List<HeroClassStatsEdit>> heroStatsEditSupplier = List::of;
  private Supplier<List<MagicMatrixEdit>> magicMatrixEditSupplier = List::of;
  private Supplier<List<EquipmentPermissionEdit>> equipmentPermissionEditSupplier = List::of;
  private Supplier<List<SkillEffectEdit>> skillEffectEditSupplier = List::of;
  private Supplier<List<ItemPriceEdit>> itemPriceEditSupplier = List::of;
  private Supplier<List<WeaponCastSpellEdit>> weaponCastSpellEditSupplier = List::of;

  public ObjectProperty<EditorWorkspace> workspaceProperty() {
    return workspace;
  }

  public EditorWorkspace workspace() {
    return workspace.get();
  }

  public void workspace(EditorWorkspace workspace) {
    this.workspace.set(workspace);
  }

  public BooleanProperty forceStrongLevelUpsProperty() {
    return forceStrongLevelUps;
  }

  public boolean forceStrongLevelUps() {
    return forceStrongLevelUps.get();
  }

  public void forceStrongLevelUps(boolean enabled) {
    forceStrongLevelUps.set(enabled);
  }

  public BooleanProperty universalSpellChargeGrowthProperty() {
    return universalSpellChargeGrowth;
  }

  public boolean universalSpellChargeGrowth() {
    return universalSpellChargeGrowth.get();
  }

  public void universalSpellChargeGrowth(boolean enabled) {
    universalSpellChargeGrowth.set(enabled);
  }

  public BooleanProperty fifteenSpellChargesProperty() {
    return fifteenSpellCharges;
  }

  public boolean fifteenSpellCharges() {
    return fifteenSpellCharges.get();
  }

  public void fifteenSpellCharges(boolean enabled) {
    fifteenSpellCharges.set(enabled);
  }

  public BooleanProperty intelligenceSpellDamageProperty() {
    return intelligenceSpellDamage;
  }

  public boolean intelligenceSpellDamage() {
    return intelligenceSpellDamage.get();
  }

  public void intelligenceSpellDamage(boolean enabled) {
    intelligenceSpellDamage.set(enabled);
  }

  public BooleanProperty corneliaMasamuneProperty() {
    return corneliaMasamune;
  }

  public boolean corneliaMasamune() {
    return corneliaMasamune.get();
  }

  public void corneliaMasamune(boolean enabled) {
    corneliaMasamune.set(enabled);
  }

  public BooleanProperty corneliaExcaliburProperty() {
    return corneliaExcalibur;
  }

  public boolean corneliaExcalibur() {
    return corneliaExcalibur.get();
  }

  public void corneliaExcalibur(boolean enabled) {
    corneliaExcalibur.set(enabled);
  }

  public BooleanProperty alwaysSuccessfulRunProperty() {
    return alwaysSuccessfulRun;
  }

  public boolean alwaysSuccessfulRun() {
    return alwaysSuccessfulRun.get();
  }

  public void alwaysSuccessfulRun(boolean enabled) {
    alwaysSuccessfulRun.set(enabled);
  }

  public BooleanProperty partyActionOrderProperty() {
    return partyActionOrder;
  }

  public boolean partyActionOrder() {
    return partyActionOrder.get();
  }

  public void partyActionOrder(boolean enabled) {
    partyActionOrder.set(enabled);
  }

  public BooleanProperty cottageReviveProperty() {
    return cottageRevive;
  }

  public boolean cottageRevive() {
    return cottageRevive.get();
  }

  public void cottageRevive(boolean enabled) {
    cottageRevive.set(enabled);
  }

  public BooleanProperty airshipLandingProperty() {
    return airshipLanding;
  }

  public boolean airshipLanding() {
    return airshipLanding.get();
  }

  public void airshipLanding(boolean enabled) {
    airshipLanding.set(enabled);
  }

  public void heroStatsEditSupplier(Supplier<List<HeroClassStatsEdit>> supplier) {
    heroStatsEditSupplier = supplier == null ? List::of : supplier;
  }

  public List<HeroClassStatsEdit> heroStatsEdits() {
    return heroStatsEditSupplier.get();
  }

  public void magicMatrixEditSupplier(Supplier<List<MagicMatrixEdit>> supplier) {
    magicMatrixEditSupplier = supplier == null ? List::of : supplier;
  }

  public List<MagicMatrixEdit> magicMatrixEdits() {
    return magicMatrixEditSupplier.get();
  }

  public void equipmentPermissionEditSupplier(Supplier<List<EquipmentPermissionEdit>> supplier) {
    equipmentPermissionEditSupplier = supplier == null ? List::of : supplier;
  }

  public List<EquipmentPermissionEdit> equipmentPermissionEdits() {
    return equipmentPermissionEditSupplier.get();
  }

  public void skillEffectEditSupplier(Supplier<List<SkillEffectEdit>> supplier) {
    skillEffectEditSupplier = supplier == null ? List::of : supplier;
  }

  public List<SkillEffectEdit> skillEffectEdits() {
    return skillEffectEditSupplier.get();
  }

  public void itemPriceEditSupplier(Supplier<List<ItemPriceEdit>> supplier) {
    itemPriceEditSupplier = supplier == null ? List::of : supplier;
  }

  public List<ItemPriceEdit> itemPriceEdits() {
    return itemPriceEditSupplier.get();
  }

  public void weaponCastSpellEditSupplier(Supplier<List<WeaponCastSpellEdit>> supplier) {
    weaponCastSpellEditSupplier = supplier == null ? List::of : supplier;
  }

  public List<WeaponCastSpellEdit> weaponCastSpellEdits() {
    return weaponCastSpellEditSupplier.get();
  }

  public StringProperty statusProperty() {
    return status;
  }

  public void status(String message) {
    status.set(message);
  }
}
