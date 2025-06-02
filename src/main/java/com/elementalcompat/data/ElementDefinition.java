package com.elementalcompat.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ElementDefinition {
    private final Map<ResourceLocation, Float> multipliers;

    public ElementDefinition(Map<ResourceLocation, Float> multipliers) {
        this.multipliers = Collections.unmodifiableMap(new HashMap<>(multipliers));
    }

    public float getMultiplier(ResourceLocation targetElement) {
        return multipliers.getOrDefault(targetElement, 1.0f);
    }

    public Set<ResourceLocation> getDefinedInteractions() {
        return multipliers.keySet();
    }
}
