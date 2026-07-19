package com.ff1.editor.service;

import static com.ff1.editor.utils.EditorSupport.editorUserPath;

import com.ff1.editor.data.EditorWorkspace;
import com.ff1.editor.data.JarCatalog;
import com.ff1.editor.data.PatchState;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public final class EditorLoadService {

  private final JarCatalogService catalogService;

  public EditorLoadService() {
    this(new JarCatalogService());
  }

  public EditorLoadService(JarCatalogService catalogService) {
    this.catalogService = catalogService;
  }

  public EditorWorkspace load(Path selectedJar) throws IOException {
    Path inputJar = selectedJar.toAbsolutePath().normalize();
    JarCatalog catalog = catalogService.readCatalog(inputJar);
    if (catalog.manifest().notMatchesExpectedFinalFantasyJar()) {
      throw new IllegalArgumentException(
          "Selected jar is not the expected Namco Bandai Final Fantasy J2ME jar: " + inputJar);
    }

    String baseName = baseName(inputJar);
    Path workDir = editorUserPath("temp").resolve(baseName);
    Path outputJar =
        EditorPatchService.nextAvailableOutputJar(
            editorUserPath("dist").resolve(baseName + "-patched-0001.jar"));
    recreateDirectory(workDir);
    extractJar(inputJar, workDir);
    byte[] gClass = Files.readAllBytes(workDir.resolve(HeroLevelGrowthClassPatcher.ENTRY_NAME));
    byte[] iClass =
        Files.readAllBytes(workDir.resolve(FifteenSpellChargeRecoveryClassPatcher.ENTRY_NAME));
    byte[] cp0 = Files.readAllBytes(workDir.resolve(UniversalSpellChargeGrowthPatcher.ENTRY_NAME));
    PatchState strongLevelUpsState = PatchState.from(HeroLevelGrowthClassPatcher.state(gClass));
    PatchState universalSpellChargesState =
        combinedState(
            PatchState.from(UniversalSpellChargeClassPatcher.state(gClass)),
            PatchState.from(UniversalSpellChargeGrowthPatcher.state(cp0)));
    PatchState fifteenSpellChargesState =
        fifteenSpellChargesState(
            PatchState.from(UniversalSpellChargeClassPatcher.state(gClass)),
            PatchState.from(FifteenSpellChargeCapClassPatcher.state(gClass)),
            PatchState.from(FifteenSpellChargeGrowthPatcher.state(cp0)),
            PatchState.from(FifteenSpellChargeRecoveryClassPatcher.state(iClass)));
    PatchState intelligenceSpellDamageState =
        PatchState.from(IntelligenceSpellDamageClassPatcher.state(gClass));
    PatchState corneliaMasamuneState = PatchState.from(CorneliaWeaponShopPatcher.state(cp0));
    PatchState corneliaExcaliburState = PatchState.from(CorneliaExcaliburShopPatcher.state(cp0));
    PatchState alwaysSuccessfulRunState =
        PatchState.from(AlwaysSuccessfulRunClassPatcher.state(gClass));
    PatchState partyActionOrderState = PatchState.from(PartyActionOrderClassPatcher.state(gClass));
    PatchState cottageReviveState = PatchState.from(CottageReviveClassPatcher.state(iClass));
    PatchState airshipLandingState = PatchState.from(AirshipLandingClassPatcher.state(iClass));
    return EditorWorkspace.builder()
        .inputJar(inputJar)
        .workDir(workDir)
        .outputJar(outputJar)
        .catalog(catalog)
        .strongLevelUpsState(strongLevelUpsState)
        .universalSpellChargesState(universalSpellChargesState)
        .fifteenSpellChargesState(fifteenSpellChargesState)
        .intelligenceSpellDamageState(intelligenceSpellDamageState)
        .corneliaMasamuneState(corneliaMasamuneState)
        .corneliaExcaliburState(corneliaExcaliburState)
        .alwaysSuccessfulRunState(alwaysSuccessfulRunState)
        .partyActionOrderState(partyActionOrderState)
        .cottageReviveState(cottageReviveState)
        .airshipLandingState(airshipLandingState)
        .build();
  }

  private static void recreateDirectory(Path directory) throws IOException {
    if (Files.exists(directory)) {
      try (var paths = Files.walk(directory)) {
        for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
          Files.delete(path);
        }
      }
    }
    Files.createDirectories(directory);
  }

  private static void extractJar(Path inputJar, Path workDir) throws IOException {
    Path root = workDir.toAbsolutePath().normalize();
    try (JarInputStream in = new JarInputStream(Files.newInputStream(inputJar))) {
      JarEntry entry;
      while ((entry = in.getNextJarEntry()) != null) {
        Path target = root.resolve(entry.getName()).normalize();
        if (!target.startsWith(root)) {
          throw new IOException("Refusing to extract unsafe jar entry: " + entry.getName());
        }
        if (entry.isDirectory()) {
          Files.createDirectories(target);
        } else {
          Path parent = target.getParent();
          if (parent != null) {
            Files.createDirectories(parent);
          }
          Files.write(target, in.readAllBytes());
        }
        in.closeEntry();
      }
    }
  }

  private static PatchState combinedState(PatchState classState, PatchState dataState) {
    if (classState == PatchState.PATCHED && dataState == PatchState.PATCHED) {
      return PatchState.PATCHED;
    }
    if (classState == PatchState.ORIGINAL && dataState == PatchState.ORIGINAL) {
      return PatchState.ORIGINAL;
    }
    return PatchState.UNKNOWN;
  }

  private static PatchState fifteenSpellChargesState(
      PatchState gateState, PatchState capState, PatchState growthState, PatchState recoveryState) {
    if (gateState == PatchState.PATCHED
        && capState == PatchState.PATCHED
        && growthState == PatchState.PATCHED
        && recoveryState == PatchState.PATCHED) {
      return PatchState.PATCHED;
    }
    if ((gateState == PatchState.ORIGINAL || gateState == PatchState.PATCHED)
        && capState == PatchState.ORIGINAL
        && growthState == PatchState.ORIGINAL
        && recoveryState == PatchState.ORIGINAL) {
      return PatchState.ORIGINAL;
    }
    return PatchState.UNKNOWN;
  }

  private static String baseName(Path inputJar) {
    Path fileName = inputJar.getFileName();
    String baseName = fileName == null ? "ff1" : fileName.toString();
    return baseName.toLowerCase().endsWith(".jar")
        ? baseName.substring(0, baseName.length() - 4)
        : baseName;
  }
}
