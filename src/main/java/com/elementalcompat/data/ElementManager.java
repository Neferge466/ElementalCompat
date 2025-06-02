package com.elementalcompat.data;

import com.elementalcompat.Elementalcompat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.*;

public class ElementManager {
    private static final Map<ResourceLocation, ElementDefinition> ELEMENT_DEFINITIONS = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 核心修复：移除@SubscribeEvent注解，改为静态工厂方法
    public static SimpleJsonResourceReloadListener createReloadListener() {
        return new SimpleJsonResourceReloadListener(GSON, "element_definitions") {
            @Override
            protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager manager, ProfilerFiller profiler) {
                ELEMENT_DEFINITIONS.clear();
                jsonMap.forEach((id, json) -> {
                    try {
                        ElementDefinition definition = parseDefinition(json.getAsJsonObject());
                        ELEMENT_DEFINITIONS.put(id, definition);
                        Elementalcompat.LOGGER.info("Loaded element definition: {}", id);
                    } catch (Exception e) {
                        Elementalcompat.LOGGER.error("元素定义加载失败 {}: {}", id, e.getMessage());
                    }
                });
            }
        };
    }

    public static Set<ResourceLocation> getAllElementIds() {
        return Collections.unmodifiableSet(ELEMENT_DEFINITIONS.keySet());
    }

    private static ElementDefinition parseDefinition(JsonObject json) {
        Map<ResourceLocation, Float> multipliers = new HashMap<>();
        JsonObject interactions = json.getAsJsonObject("interactions");
        for (Map.Entry<String, JsonElement> entry : interactions.entrySet()) {
            ResourceLocation target = new ResourceLocation(entry.getKey());
            float value = entry.getValue().getAsFloat();
            multipliers.put(target, value);
        }
        return new ElementDefinition(multipliers);
    }

    public static float getMultiplier(ResourceLocation attacker, ResourceLocation defender) {
        ElementDefinition def = ELEMENT_DEFINITIONS.get(attacker);
        return def != null ? def.getMultiplier(defender) : 1.0f;
    }

    public static class ElementDefinition {
        private final Map<ResourceLocation, Float> multipliers;

        public ElementDefinition(Map<ResourceLocation, Float> multipliers) {
            this.multipliers = multipliers;
        }

        public float getMultiplier(ResourceLocation target) {
            return multipliers.getOrDefault(target, 1.0f);
        }
    }
}
