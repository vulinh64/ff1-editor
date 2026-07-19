package com.ff1.editor.service.patcher;

import com.ff1.editor.data.MagicMatrixEdit;
import com.ff1.editor.service.*;

/** Patches cp0 learnable spell permission masks from the Magic Matrix editor tab. */
public final class MagicMatrixPatcher {

  private MagicMatrixPatcher() {}

  public static void apply(byte[] cp0, int chunkOffset, MagicMatrixEdit edit) {
    if (edit.spellId() < 1 || edit.spellId() > MagicMatrixDiscoveryService.LEARNABLE_SPELL_COUNT) {
      throw new IllegalArgumentException("Spell id must be 1..64.");
    }
    if ((edit.permissionMask() & ~0x3f3f) != 0) {
      throw new IllegalArgumentException("Magic permission mask contains unknown class bits.");
    }
    int offset =
        chunkOffset
            + 2
            + edit.spellId() * MagicMatrixDiscoveryService.SPELL_RECORD_SIZE
            + MagicMatrixDiscoveryService.MASK_OFFSET_IN_RECORD;
    cp0[offset] = (byte) ((edit.permissionMask() >>> 8) & 0xff);
    cp0[offset + 1] = (byte) (edit.permissionMask() & 0xff);
  }
}
