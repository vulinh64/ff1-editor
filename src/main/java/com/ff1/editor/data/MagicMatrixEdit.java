package com.ff1.editor.data;

import lombok.Builder;
import lombok.With;

@Builder
@With
public record MagicMatrixEdit(int spellId, int permissionMask) {}
