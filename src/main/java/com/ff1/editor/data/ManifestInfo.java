package com.ff1.editor.data;

import java.util.Map;
import lombok.Builder;
import lombok.With;
import org.apache.commons.lang3.StringUtils;

@Builder
@With
public record ManifestInfo(Map<String, String> attributes) {

  public String get(String key) {
    return attributes.getOrDefault(key, StringUtils.EMPTY);
  }

  public boolean notMatchesExpectedFinalFantasyJar() {
    return !"Final Fantasy".equals(get("MIDlet-Name"))
        || !"Namco Bandai".equals(get("MIDlet-Vendor"))
        || !get("MIDlet-1").contains("FinalFantasy")
        || !"MIDP-2.0".equals(get("MicroEdition-Profile"))
        || !"CLDC-1.1".equals(get("MicroEdition-Configuration"));
  }
}
