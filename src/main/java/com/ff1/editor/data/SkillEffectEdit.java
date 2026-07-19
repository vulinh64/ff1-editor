package com.ff1.editor.data;

import lombok.Builder;

@Builder
public record SkillEffectEdit(int skillId, int price, int powerOrStatus, int accuracy) {}
