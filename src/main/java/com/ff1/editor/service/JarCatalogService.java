package com.ff1.editor.service;

import com.ff1.editor.data.JarCatalog;
import com.ff1.editor.data.JarEntryInfo;
import com.ff1.editor.data.ManifestInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JarCatalogService {

  private static final Pattern PACK_PATTERN = Pattern.compile("PACK(\\d+)_(\\d+)");

  public JarCatalog readCatalog(Path jarPath) throws IOException {
    try (JarFile jarFile = new JarFile(jarPath.toFile())) {
      ManifestInfo manifest = readManifest(jarFile);
      List<JarEntryInfo> entries =
          jarFile.stream()
              .filter(entry -> !entry.isDirectory())
              .map(
                  entry ->
                      JarEntryInfo.builder()
                          .name(entry.getName())
                          .size(entry.getSize())
                          .compressedSize(entry.getCompressedSize())
                          .crc(entry.getCrc())
                          .build())
              .sorted()
              .toList();

      List<JarEntryInfo> classes = entries.stream().filter(JarEntryInfo::isClass).toList();
      List<JarEntryInfo> resources = entries.stream().filter(entry -> !entry.isClass()).toList();
      Map<Integer, List<JarEntryInfo>> packGroups = groupPackResources(resources);
      List<JarEntryInfo> likelyDataResources =
          resources.stream()
              .filter(JarEntryInfo::isLikelyBinaryData)
              .sorted(
                  Comparator.comparingLong(JarEntryInfo::size)
                      .reversed()
                      .thenComparing(JarEntryInfo::name))
              .toList();

      return JarCatalog.builder()
          .manifest(manifest)
          .classes(classes)
          .resources(resources)
          .packGroups(packGroups)
          .likelyDataResources(likelyDataResources)
          .build();
    }
  }

  public void writeCatalog(Path jarPath, Path catalogPath) throws IOException {
    JarCatalog catalog = readCatalog(jarPath);
    if (catalog.manifest().notMatchesExpectedFinalFantasyJar()) {
      throw new IllegalArgumentException(
          "Input jar does not look like the expected Namco Bandai Final Fantasy J2ME jar: "
              + jarPath);
    }

    Files.createDirectories(catalogPath.toAbsolutePath().getParent());
    Files.writeString(catalogPath, toMarkdown(jarPath, catalog), StandardCharsets.UTF_8);
  }

  private static ManifestInfo readManifest(JarFile jarFile) throws IOException {
    if (jarFile.getManifest() == null) {
      throw new IllegalArgumentException("Jar has no manifest");
    }
    Attributes attributes = jarFile.getManifest().getMainAttributes();
    Map<String, String> values = new LinkedHashMap<>();
    attributes.forEach((key, value) -> values.put(key.toString(), value.toString()));
    return ManifestInfo.builder().attributes(Map.copyOf(values)).build();
  }

  private static Map<Integer, List<JarEntryInfo>> groupPackResources(List<JarEntryInfo> resources) {
    Map<Integer, List<JarEntryInfo>> grouped = new TreeMap<>();
    for (JarEntryInfo resource : resources) {
      Matcher matcher = PACK_PATTERN.matcher(resource.name());
      if (matcher.matches()) {
        int group = Integer.parseInt(matcher.group(1));
        grouped.computeIfAbsent(group, ignored -> new ArrayList<>()).add(resource);
      }
    }
    grouped.replaceAll(
        (ignored, values) ->
            values.stream()
                .sorted(Comparator.comparingInt(JarCatalogService::packPartIndex))
                .toList());
    return Map.copyOf(grouped);
  }

  private static int packPartIndex(JarEntryInfo entry) {
    Matcher matcher = PACK_PATTERN.matcher(entry.name());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Not a pack resource: " + entry.name());
    }
    return Integer.parseInt(matcher.group(2));
  }

  private static String toMarkdown(Path jarPath, JarCatalog catalog) {
    StringBuilder out = new StringBuilder();
    out.append("# FF1 Jar Catalog\n\n");
    out.append("Source: `").append(jarPath.toAbsolutePath()).append("`\n\n");
    out.append("## Manifest\n\n");
    catalog.manifest().attributes().entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry ->
                out.append("- `")
                    .append(entry.getKey())
                    .append("`: ")
                    .append(entry.getValue())
                    .append('\n'));

    out.append("\n## Summary\n\n");
    out.append("- Classes: ").append(catalog.classes().size()).append('\n');
    out.append("- Resources: ").append(catalog.resources().size()).append('\n');
    out.append("- PACK groups: ").append(catalog.packGroups().size()).append('\n');
    out.append("- Likely binary data resources: ")
        .append(catalog.likelyDataResources().size())
        .append("\n\n");

    appendEntries(out, "Classes", catalog.classes());
    appendPackGroups(out, catalog.packGroups());
    appendEntries(out, "Likely Binary Data Resources", catalog.likelyDataResources());
    appendEntries(out, "All Resources", catalog.resources());
    return out.toString();
  }

  private static void appendPackGroups(
      StringBuilder out, Map<Integer, List<JarEntryInfo>> packGroups) {
    out.append("## PACK Groups\n\n");
    for (Map.Entry<Integer, List<JarEntryInfo>> group :
        packGroups.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
      long total = group.getValue().stream().mapToLong(JarEntryInfo::size).sum();
      out.append("### PACK")
          .append(group.getKey())
          .append(" (")
          .append(group.getValue().size())
          .append(" entries, ")
          .append(total)
          .append(" bytes)\n\n");
      appendEntryTable(out, group.getValue());
    }
  }

  private static void appendEntries(StringBuilder out, String title, List<JarEntryInfo> entries) {
    out.append("## ").append(title).append("\n\n");
    appendEntryTable(out, entries);
  }

  private static void appendEntryTable(StringBuilder out, List<JarEntryInfo> entries) {
    out.append("| Entry | Size | Compressed | CRC32 |\n");
    out.append("| --- | ---: | ---: | --- |\n");
    for (JarEntryInfo entry : entries) {
      out.append("| `")
          .append(entry.name())
          .append("` | ")
          .append(entry.size())
          .append(" | ")
          .append(entry.compressedSize())
          .append(" | `")
          .append(String.format("%08x", entry.crc()))
          .append("` |\n");
    }
    out.append('\n');
  }
}
