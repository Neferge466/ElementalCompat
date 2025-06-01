package com.elementalcompat;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public record ElementalType(
        ResourceLocation id,
        int color,
        Map<ResourceLocation, Float> strengths,
        Map<ResourceLocation, Float> weaknesses,
        List<ResourceLocation> immunities
) {
    public float getEffectivenessAgainst(ElementalType other) {
        float multiplier = 1.0f;

        if (strengths.containsKey(other.id())) {
            multiplier *= strengths.get(other.id());
        }
        if (weaknesses.containsKey(other.id())) {
            multiplier *= weaknesses.get(other.id());
        }
        if (immunities.contains(other.id())) {
            multiplier = 0.0f;
        }

        return multiplier;
    }
}

