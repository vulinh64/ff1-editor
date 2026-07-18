package com.ff1.editor.data;

import java.nio.file.Path;
import java.util.List;
import lombok.Builder;
import lombok.With;

@Builder
@With
public record BuildResult(Path outputJar, List<String> replacedEntries, String summary) {}
