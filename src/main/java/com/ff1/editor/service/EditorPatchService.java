package com.ff1.editor.service;

import static com.ff1.editor.utils.EditorSupport.replaceJarEntries;

import com.ff1.editor.data.BuildResult;
import com.ff1.editor.data.EditorWorkspace;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class EditorPatchService {

  public BuildResult buildPatch(EditorWorkspace workspace, Map<String, byte[]> replacements)
      throws IOException {
    if (workspace == null) {
      throw new IllegalArgumentException("Load a Final Fantasy jar before building a patch.");
    }
    if (replacements == null || replacements.isEmpty()) {
      throw new IllegalArgumentException("No replacement entries were provided.");
    }

    validateReplacementEntries(workspace.inputJar(), replacements.keySet());
    Files.createDirectories(workspace.outputJar().toAbsolutePath().getParent());
    Path outputJar = nextAvailableOutputJar(workspace.outputJar());
    replaceJarEntries(workspace.inputJar(), outputJar, replacements);
    return BuildResult.builder()
        .outputJar(outputJar)
        .replacedEntries(List.copyOf(replacements.keySet()))
        .summary("replaced " + replacements.size() + " jar entries")
        .build();
  }

  private static void validateReplacementEntries(Path inputJar, Set<String> replacementNames)
      throws IOException {
    Set<String> entries = new HashSet<>();
    try (ZipInputStream in = new ZipInputStream(Files.newInputStream(inputJar))) {
      ZipEntry entry;
      while ((entry = in.getNextEntry()) != null) {
        entries.add(entry.getName());
      }
    }
    for (String replacementName : replacementNames) {
      if (!entries.contains(replacementName)) {
        throw new IllegalArgumentException(
            "Replacement entry does not exist in input jar: " + replacementName);
      }
    }
  }

  static Path nextAvailableOutputJar(Path firstCandidate) {
    Path absoluteCandidate = firstCandidate.toAbsolutePath().normalize();
    if (!Files.exists(absoluteCandidate)) {
      return absoluteCandidate;
    }
    Path directory = absoluteCandidate.getParent();
    String fileName = absoluteCandidate.getFileName().toString();
    String extension = ".jar";
    String stem =
        fileName.endsWith(extension)
            ? fileName.substring(0, fileName.length() - extension.length())
            : fileName;
    String prefix = numberedPrefix(stem);
    for (int suffix = 1; suffix <= 9999; suffix++) {
      Path candidate = directory.resolve("%s%04d%s".formatted(prefix, suffix, extension));
      if (!Files.exists(candidate)) {
        return candidate;
      }
    }
    throw new IllegalStateException(
        "No available patched JAR filename remains for " + firstCandidate);
  }

  private static String numberedPrefix(String stem) {
    if (stem.length() > 5
        && stem.charAt(stem.length() - 5) == '-'
        && stem.substring(stem.length() - 4).chars().allMatch(Character::isDigit)) {
      return stem.substring(0, stem.length() - 4);
    }
    return stem + "-";
  }
}
