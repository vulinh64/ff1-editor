package com.ff1.editor.utils;

import com.ff1.editor.data.MaskOption;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public final class MaskLabelFormatter {

  private static final String HEX_BYTE_PATTERN = "0x%02x";
  private static final String LABELED_BIT_PATTERN = "%s (0x%02x)";

  private MaskLabelFormatter() {}

  public static String labelsForMask(int mask, MaskOption[] options) {
    return labelsForMask(mask, options, HEX_BYTE_PATTERN);
  }

  public static String labelsForMask(int mask, MaskOption[] options, String unknownBitPattern) {
    if (mask == 0) {
      return StringUtils.EMPTY;
    }
    StringBuilder out = new StringBuilder();
    int unknownBits = mask;
    for (MaskOption option : options) {
      if ((mask & option.bit()) == 0) {
        continue;
      }
      append(out, option.label());
      unknownBits &= ~option.bit();
    }
    for (int bit = 1; bit <= 0x80; bit <<= 1) {
      if ((unknownBits & bit) != 0) {
        append(out, unknownBitPattern.formatted(bit));
      }
    }
    return out.toString();
  }

  public static String labeledBitsForMask(
      int mask, Map<Integer, String> labels, String fallbackPrefix) {
    if (mask == 0) {
      return StringUtils.EMPTY;
    }
    StringBuilder out = new StringBuilder();
    int unknownBits = mask;
    for (Map.Entry<Integer, String> entry : labels.entrySet()) {
      int bit = entry.getKey();
      if ((mask & bit) == 0) {
        continue;
      }
      append(out, LABELED_BIT_PATTERN.formatted(entry.getValue(), bit));
      unknownBits &= ~bit;
    }
    for (int bit = 1; bit <= 0x80; bit <<= 1) {
      if ((unknownBits & bit) != 0) {
        append(out, "%s 0x%02x".formatted(fallbackPrefix, bit));
      }
    }
    return out.toString();
  }

  public static void append(StringBuilder out, String value) {
    if (!out.isEmpty()) {
      out.append("; ");
    }
    out.append(value);
  }
}
