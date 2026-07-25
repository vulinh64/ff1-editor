package com.ff1.editor.data;

import com.ff1.editor.utils.DataUtils;
import java.nio.file.Path;
import java.util.List;
import lombok.Builder;
import lombok.With;

@Builder
@With
public record BuildResult(Path outputJar, List<String> replacedEntries, String summary) {

  public BuildResult {
    replacedEntries = DataUtils.emptyListIfNull(replacedEntries);
  }
}
