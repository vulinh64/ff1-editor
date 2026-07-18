package com.ff1.editor.data;

import lombok.Builder;
import lombok.With;

@Builder
@With
public record SkillSnapshot(
    int id,
    String name,
    String learnableLabel,
    int price,
    int raw0,
    int effectId,
    int effectKind,
    String effectKindName,
    int powerOrStatus,
    int accuracy,
    int raw5,
    int animationId,
    int animationFlags,
    int elementOrStatusMask,
    int permissionMask,
    String invokers,
    String sourceEntry,
    int sourceOffset) {}
