package com.ff1.editor.data;

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
    List<JarEntryInfo> likelyDataResources) {}
