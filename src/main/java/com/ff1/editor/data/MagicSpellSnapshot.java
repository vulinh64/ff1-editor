package com.ff1.editor.data;

import lombok.Builder;
import lombok.With;

@Builder
@With
public record MagicSpellSnapshot(
    int spellId,
    String name,
    String description,
    SpellSchool school,
    int level,
    int slot,
    int permissionMask,
    String sourceEntry,
    int sourceOffset) {}
