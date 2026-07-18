package com.ff1.editor;

import com.ff1.editor.service.Ff1TextService;
import com.ff1.editor.service.HeroClassDiscoveryService;
import com.ff1.editor.service.IntArrayDumpService;
import com.ff1.editor.service.ItemEquipmentDiscoveryService;
import com.ff1.editor.service.JarCatalogService;
import com.ff1.editor.service.SkillDiscoveryService;
import com.ff1.editor.view.FxEditorApplication;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FinalFantasyDataEditor {

  public static final String FF_1_JAR = "ff1-jar";

  private FinalFantasyDataEditor() {}

  static void main(String[] args) throws Exception {
    if (args.length == 0) {
      FxEditorApplication.launchEditor(args);
      return;
    }

    if ("--help".equals(args[0]) || "-h".equals(args[0])) {
      printUsage();
      return;
    }

    if ("--fx".equals(args[0]) || "fx".equals(args[0])) {
      FxEditorApplication.launchEditor(Arrays.copyOfRange(args, 1, args.length));
      return;
    }

    if ("dump-int-arrays".equals(args[0])) {
      if (args.length < 3) {
        printUsage();
        return;
      }
      Path jarPath = Path.of(args[1]);
      new IntArrayDumpService().dump(jarPath, args[2]);
      return;
    }

    if ("dump-text".equals(args[0])) {
      if (args.length < 5) {
        printUsage();
        return;
      }
      Path extractedDir = Path.of(args[1]);
      String prefix = args[2];
      int[] boundaries =
          Arrays.stream(args[3].split(","))
              .filter(value -> !value.isBlank())
              .mapToInt(Integer::parseInt)
              .toArray();
      int startInclusive = Integer.parseInt(args[4]);
      int endExclusive = args.length >= 6 ? Integer.parseInt(args[5]) : startInclusive + 1;
      Ff1TextService textService = new Ff1TextService(extractedDir);
      for (int id = startInclusive; id < endExclusive; id++) {
        byte[] encoded = textService.readChunk(prefix, boundaries, id);
        log.info("{}: {}", "%03d".formatted(id), textService.decodeText(encoded));
      }
      return;
    }

    if ("search-text".equals(args[0])) {
      if (args.length < 3) {
        printUsage();
        return;
      }
      Path extractedDir = Path.of(args[1]);
      Ff1TextService textService = new Ff1TextService(extractedDir);
      for (int i = 2; i < args.length; i++) {
        textService.searchEncodedText(args[i]);
      }
      return;
    }

    if ("heroes".equals(args[0])) {
      Path extractedDir = args.length >= 2 ? Path.of(args[1]) : Path.of(FF_1_JAR);
      new HeroClassDiscoveryService(extractedDir).discover().forEach(hero -> log.info("{}", hero));
      return;
    }

    if ("items".equals(args[0])) {
      Path extractedDir = args.length >= 2 ? Path.of(args[1]) : Path.of(FF_1_JAR);
      new ItemEquipmentDiscoveryService(extractedDir)
          .discover()
          .forEach(item -> log.info("{}", item));
      return;
    }

    if ("skills".equals(args[0])) {
      Path extractedDir = args.length >= 2 ? Path.of(args[1]) : Path.of(FF_1_JAR);
      new SkillDiscoveryService(extractedDir).discover().forEach(skill -> log.info("{}", skill));
      return;
    }

    Path catalogPath = getCatalogPath(args);

    log.info("Wrote catalog: {}", catalogPath.toAbsolutePath());
  }

  private static Path getCatalogPath(String[] args) throws IOException {
    Path jarPath = Path.of(args[0]);
    Path catalogPath = Path.of("target", "ff1-catalog.md");
    int index = 1;
    while (index < args.length) {
      String option = args[index];
      if ("--catalog".equals(option) && index + 1 < args.length) {
        catalogPath = Path.of(args[index + 1]);
        index += 2;
      } else {
        throw new IllegalArgumentException("Unknown argument: " + option);
      }
    }

    JarCatalogService service = new JarCatalogService();
    service.writeCatalog(jarPath, catalogPath);
    return catalogPath;
  }

  private static void printUsage() {
    log.info(
        """
                Usage: java -jar ff1-data-editor-0.1.0.jar
                       Opens the JavaFX editor.

                Developer commands:
                       java -jar ff1-data-editor-0.1.0.jar <ff1.jar> [--catalog target\\ff1-catalog.md]
                       java -jar ff1-data-editor-0.1.0.jar dump-int-arrays <ff1.jar> <className>
                       java -jar ff1-data-editor-0.1.0.jar dump-text <extractedDir> <prefix> <boundariesCsv> <startInclusive> [endExclusive]
                       java -jar ff1-data-editor-0.1.0.jar search-text <extractedDir> <text> [moreText...]
                       java -jar ff1-data-editor-0.1.0.jar heroes [extractedDir]
                       java -jar ff1-data-editor-0.1.0.jar items [extractedDir]
                       java -jar ff1-data-editor-0.1.0.jar skills [extractedDir]
                       java -jar ff1-data-editor-0.1.0.jar --fx [input.jar]""");
  }
}
