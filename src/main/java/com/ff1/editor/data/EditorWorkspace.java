package com.ff1.editor.data;

import java.nio.file.Path;
import lombok.Builder;
import lombok.With;

@Builder
@With
public record EditorWorkspace(
    Path inputJar,
    Path workDir,
    Path outputJar,
    JarCatalog catalog,
    PatchState strongLevelUpsState,
    PatchState universalSpellChargesState,
    PatchState fifteenSpellChargesState,
    PatchState intelligenceSpellDamageState,
    PatchState corneliaMasamuneState,
    PatchState alwaysSuccessfulRunState,
    PatchState partyActionOrderState,
    PatchState cottageReviveState,
    PatchState airshipLandingState) {}
