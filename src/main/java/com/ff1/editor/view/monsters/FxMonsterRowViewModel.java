package com.ff1.editor.view.monsters;

import com.ff1.editor.data.MaskOption;
import com.ff1.editor.data.MonsterArchetype;
import com.ff1.editor.data.MonsterElementAffinity;
import com.ff1.editor.data.MonsterSnapshot;
import com.ff1.editor.data.MonsterStatsEdit;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.commons.lang3.StringUtils;

public final class FxMonsterRowViewModel {

  private final MonsterSnapshot monster;
  private final IntegerProperty exp;
  private final IntegerProperty gil;
  private final IntegerProperty hp;
  private final IntegerProperty attack;
  private final IntegerProperty hitCount;
  private final IntegerProperty defense;
  private final IntegerProperty evasion;
  private final IntegerProperty magicDefense;
  private final IntegerProperty archetypeMask;
  private final IntegerProperty weaknessMask;
  private final IntegerProperty resistanceMask;

  public FxMonsterRowViewModel(MonsterSnapshot monster) {
    this.monster = monster;
    this.exp = new SimpleIntegerProperty(monster.exp());
    this.gil = new SimpleIntegerProperty(monster.gil());
    this.hp = new SimpleIntegerProperty(monster.hp());
    this.attack = new SimpleIntegerProperty(monster.attack());
    this.hitCount = new SimpleIntegerProperty(monster.hitCount());
    this.defense = new SimpleIntegerProperty(monster.defense());
    this.evasion = new SimpleIntegerProperty(monster.evasion());
    this.magicDefense = new SimpleIntegerProperty(monster.magicDefense());
    this.archetypeMask = new SimpleIntegerProperty(monster.typeMask());
    this.weaknessMask = new SimpleIntegerProperty(monster.weaknessMask());
    this.resistanceMask = new SimpleIntegerProperty(monster.resistanceMask());
  }

  public int id() {
    return monster.id();
  }

  public String name() {
    return monster.name();
  }

  public boolean bossOrFixed() {
    return monster.bossOrFixed();
  }

  public String bossEncounterIds() {
    return monster.bossEncounterIds();
  }

  public int hp() {
    return hp.get();
  }

  public int exp() {
    return exp.get();
  }

  public int gil() {
    return gil.get();
  }

  public IntegerProperty expProperty() {
    return exp;
  }

  public IntegerProperty gilProperty() {
    return gil;
  }

  public IntegerProperty hpProperty() {
    return hp;
  }

  public int attack() {
    return attack.get();
  }

  public IntegerProperty attackProperty() {
    return attack;
  }

  public IntegerProperty hitCountProperty() {
    return hitCount;
  }

  public int defense() {
    return defense.get();
  }

  public IntegerProperty defenseProperty() {
    return defense;
  }

  public int evasion() {
    return evasion.get();
  }

  public IntegerProperty evasionProperty() {
    return evasion;
  }

  public int magicDefense() {
    return magicDefense.get();
  }

  public IntegerProperty magicDefenseProperty() {
    return magicDefense;
  }

  public String typeMask() {
    return hexByte(archetypeMask.get());
  }

  public String archetypes() {
    return archetypeLabels(archetypeMask.get());
  }

  public int archetypeMaskValue() {
    return archetypeMask.get();
  }

  public void archetypeMaskValue(int mask) {
    archetypeMask.set(mask);
  }

  public String weaknessMask() {
    return hexByte(weaknessMask.get());
  }

  public String weaknesses() {
    return elementLabels(weaknessMask.get());
  }

  public int weaknessMaskValue() {
    return weaknessMask.get();
  }

  public void weaknessMaskValue(int mask) {
    weaknessMask.set(mask);
  }

  public String resistanceMask() {
    return hexByte(resistanceMask.get());
  }

  public String resistances() {
    return elementLabels(resistanceMask.get());
  }

  public int resistanceMaskValue() {
    return resistanceMask.get();
  }

  public void resistanceMaskValue(int mask) {
    resistanceMask.set(mask);
  }

  public String rawStart() {
    return "%d, %d, %d, %d"
        .formatted(monster.raw0(), monster.raw1(), monster.raw2(), monster.raw3());
  }

  public String source() {
    return "cp0 @ 0x%08x".formatted(monster.sourceOffset());
  }

  public boolean changed() {
    return exp.get() != monster.exp()
        || gil.get() != monster.gil()
        || hp.get() != monster.hp()
        || attack.get() != monster.attack()
        || hitCount.get() != monster.hitCount()
        || defense.get() != monster.defense()
        || evasion.get() != monster.evasion()
        || magicDefense.get() != monster.magicDefense()
        || archetypeMask.get() != monster.typeMask()
        || weaknessMask.get() != monster.weaknessMask()
        || resistanceMask.get() != monster.resistanceMask();
  }

  public MonsterStatsEdit toEdit() {
    return MonsterStatsEdit.builder()
        .monsterId(id())
        .exp(exp.get())
        .gil(gil.get())
        .hp(hp.get())
        .attack(attack.get())
        .hitCount(hitCount.get())
        .defense(defense.get())
        .evasion(evasion.get())
        .magicDefense(magicDefense.get())
        .archetypeMask(archetypeMask.get())
        .weaknessMask(weaknessMask.get())
        .resistanceMask(resistanceMask.get())
        .build();
  }

  public boolean matches(String query) {
    if (query == null || query.isBlank()) {
      return true;
    }
    String normalized = query.toLowerCase();
    return String.valueOf(id()).contains(normalized)
        || name().toLowerCase().contains(normalized)
        || bossEncounterIds().toLowerCase().contains(normalized)
        || String.valueOf(exp()).contains(normalized)
        || String.valueOf(gil()).contains(normalized)
        || String.valueOf(hp()).contains(normalized)
        || String.valueOf(attack()).contains(normalized)
        || String.valueOf(defense()).contains(normalized)
        || String.valueOf(magicDefense()).contains(normalized)
        || typeMask().toLowerCase().contains(normalized)
        || archetypes().toLowerCase().contains(normalized)
        || weaknessMask().toLowerCase().contains(normalized)
        || weaknesses().toLowerCase().contains(normalized)
        || resistanceMask().toLowerCase().contains(normalized)
        || resistances().toLowerCase().contains(normalized)
        || source().toLowerCase().contains(normalized);
  }

  public static String archetypeLabels(int mask) {
    if (mask == 0) {
      return StringUtils.EMPTY;
    }
    StringBuilder out = new StringBuilder();
    appendKnownLabels(out, mask, MonsterArchetype.values());
    return out.toString();
  }

  private static String elementLabels(int mask) {
    if (mask == 0) {
      return StringUtils.EMPTY;
    }
    StringBuilder out = new StringBuilder();
    appendKnownLabels(out, mask, MonsterElementAffinity.values());
    return out.toString();
  }

  private static void appendKnownLabels(StringBuilder out, int mask, MaskOption[] options) {
    int unknownBits = mask;
    for (MaskOption option : options) {
      if ((mask & option.bit()) == 0) {
        continue;
      }
      append(out, option.label());
      unknownBits &= ~option.bit();
    }
    for (int bit = 1; bit <= 0x80; bit <<= 1) {
      if ((unknownBits & bit) != 0) {
        append(out, "0x%02x".formatted(bit));
      }
    }
  }

  private static void append(StringBuilder out, String value) {
    if (!out.isEmpty()) {
      out.append("; ");
    }
    out.append(value);
  }

  private static String hexByte(int value) {
    return value == 0 ? StringUtils.EMPTY : "0x%02x".formatted(value);
  }
}
