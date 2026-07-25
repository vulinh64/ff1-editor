package com.ff1.editor.data;

import com.ff1.editor.utils.DataUtils;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.With;

@Builder
@With
public record JarCatalog(
    ManifestInfo manifest,
    List<JarEntryInfo> classes,
    List<JarEntryInfo> resources,
    Map<Integer, List<JarEntryInfo>> packGroups,
    List<JarEntryInfo> likelyDataResources) {

  public JarCatalog {
    classes = DataUtils.emptyListIfNull(classes);
    resources = DataUtils.emptyListIfNull(resources);
    packGroups = DataUtils.emptyMapIfNull(packGroups);
    likelyDataResources = DataUtils.emptyListIfNull(likelyDataResources);
  }
}
