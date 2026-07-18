package com.ff1.editor.view.heroes;

import com.ff1.editor.data.HeroClassSnapshot;
import com.ff1.editor.data.HeroClassStatsEdit;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import lombok.Builder;
import lombok.With;

@Builder
@With
public record FxHeroClassViewModel(
    HeroClassSnapshot hero,
    IntegerProperty hp,
    IntegerProperty strength,
    IntegerProperty agility,
    IntegerProperty intelligence,
    IntegerProperty stamina,
    IntegerProperty luck) {

  public FxHeroClassViewModel(HeroClassSnapshot hero) {
    this(
        hero,
        new SimpleIntegerProperty(hero.stats().hp()),
        new SimpleIntegerProperty(hero.stats().strength()),
        new SimpleIntegerProperty(hero.stats().agility()),
        new SimpleIntegerProperty(hero.stats().intelligence()),
        new SimpleIntegerProperty(hero.stats().stamina()),
        new SimpleIntegerProperty(hero.stats().luck()));
  }

  public FxHeroClassViewModel mirrorStatsFrom(FxHeroClassViewModel baseClass) {
    if (baseClass == null) {
      throw new IllegalArgumentException("Upgraded class mirror requires a base class row.");
    }
    return withHp(baseClass.hp)
        .withStrength(baseClass.strength)
        .withAgility(baseClass.agility)
        .withIntelligence(baseClass.intelligence)
        .withStamina(baseClass.stamina)
        .withLuck(baseClass.luck);
  }

  public int id() {
    return hero.id();
  }

  public String name() {
    return hero.name();
  }

  public String tier() {
    return hero.upgraded() ? "Upgraded" : "Base";
  }

  public String upgradeFrom() {
    return hero.upgraded() ? String.valueOf(hero.upgradeFromId()) : "";
  }

  public boolean baseClass() {
    return !hero.upgraded();
  }

  public IntegerProperty hpProperty() {
    return hp;
  }

  public IntegerProperty strengthProperty() {
    return strength;
  }

  public IntegerProperty agilityProperty() {
    return agility;
  }

  public IntegerProperty intelligenceProperty() {
    return intelligence;
  }

  public IntegerProperty staminaProperty() {
    return stamina;
  }

  public IntegerProperty luckProperty() {
    return luck;
  }

  public void resetStats() {
    if (!baseClass()) {
      return;
    }
    hp.set(hero.stats().hp());
    strength.set(hero.stats().strength());
    agility.set(hero.stats().agility());
    intelligence.set(hero.stats().intelligence());
    stamina.set(hero.stats().stamina());
    luck.set(hero.stats().luck());
  }

  public boolean changed() {
    return hp.get() != hero.stats().hp()
        || strength.get() != hero.stats().strength()
        || agility.get() != hero.stats().agility()
        || intelligence.get() != hero.stats().intelligence()
        || stamina.get() != hero.stats().stamina()
        || luck.get() != hero.stats().luck();
  }

  public HeroClassStatsEdit toEdit() {
    return HeroClassStatsEdit.builder()
        .classId(id())
        .hp(hp.get())
        .strength(strength.get())
        .agility(agility.get())
        .intelligence(intelligence.get())
        .stamina(stamina.get())
        .luck(luck.get())
        .build();
  }

  public String source() {
    return "%s @ 0x%08x".formatted(hero.sourceEntry(), hero.sourceOffset());
  }

  public String notes() {
    return hero.stats().sourceNote();
  }

  public boolean matches(String query) {
    String normalized = query == null ? "" : query.trim().toLowerCase();
    return normalized.isEmpty()
        || ("%d %s %s %s %s".formatted(id(), name(), tier(), source(), notes()))
            .toLowerCase()
            .contains(normalized);
  }
}
