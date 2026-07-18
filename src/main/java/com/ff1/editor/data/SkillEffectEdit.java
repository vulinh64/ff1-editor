package com.ff1.editor.data;

import lombok.Builder;

@Builder
public record SkillEffectEdit(int skillId, int powerOrStatus, int accuracy) {}
