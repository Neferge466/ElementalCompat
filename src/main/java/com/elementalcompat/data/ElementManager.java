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
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Elementalcompat.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ElementManager {
    private static final Map<ResourceLocation, ElementDefinition> ELEMENT_DEFINITIONS = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile boolean isLoaded = false;

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(createReloadListener());
        Elementalcompat.LOGGER.info("[ElementManager] 已注册资源加载监听器");
        event.addListener(new UnifiedElementLoader());
        Elementalcompat.LOGGER.info("[UnifiedLoader] 统一元素绑定加载器已注册");
    }

    public static SimpleJsonResourceReloadListener createReloadListener() {
        return new ElementReloadListener();
    }

    private static class ElementReloadListener extends SimpleJsonResourceReloadListener {
        private static final String DIRECTORY = "element_definitions";

        public ElementReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonMap,
                             ResourceManager manager,
                             ProfilerFiller profiler) {
            ELEMENT_DEFINITIONS.clear();
            isLoaded = false;

            jsonMap.forEach((id, json) -> {
                try {
                    if (!id.getNamespace().equals(Elementalcompat.MODID)) {
                        Elementalcompat.LOGGER.debug("跳过非本模组元素定义: {}", id);
                        return;
                    }

                    JsonObject jsonObj = json.getAsJsonObject();
                    ElementDefinition definition = parseDefinition(jsonObj);
                    ELEMENT_DEFINITIONS.put(id, definition);

                    Elementalcompat.LOGGER.info("[元素加载] 成功加载 {} | 交互规则: {} 条 | 最小伤害: {:.1f} | 最小承伤: {:.1f}",
                            id,
                            definition.getInteractionCount(),
                            definition.getMinDamage(),
                            definition.getMinIncoming());
                } catch (Exception e) {
                    Elementalcompat.LOGGER.error("元素定义加载失败 {}: {}", id, e.getMessage());
                }
            });

            isLoaded = true;
            Elementalcompat.LOGGER.info("[元素系统] 已加载 {} 个元素定义", ELEMENT_DEFINITIONS.size());
            Elementalcompat.LOGGER.debug("已加载元素列表: {}",
                    ELEMENT_DEFINITIONS.keySet().stream()
                            .map(ResourceLocation::toString)
                            .collect(Collectors.joining(", ")));
        }
    }

    private static ElementDefinition parseDefinition(JsonObject json) throws IllegalArgumentException {
        if (!json.has("interactions")) {
            throw new IllegalArgumentException("元素定义缺少必需的'interactions'字段");
        }

        JsonObject interactions = json.getAsJsonObject("interactions");
        Map<ResourceLocation, Float> multipliers = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : interactions.entrySet()) {
            ResourceLocation target = new ResourceLocation(entry.getKey());
            float value = entry.getValue().getAsFloat();
            if (value < 0) {
                Elementalcompat.LOGGER.warn("修正无效的交互值 {} → {} 为0", target, value);
                value = 0;
            }
            multipliers.put(target, value);
        }

        float minDamage = 1.0f;
        float minIncoming = 0.5f;
        if (json.has("thresholds")) {
            JsonObject thresholds = json.getAsJsonObject("thresholds");
            if (thresholds.has("min_damage")) {
                minDamage = thresholds.get("min_damage").getAsFloat();
            }
            if (thresholds.has("min_incoming")) {
                minIncoming = thresholds.get("min_incoming").getAsFloat();
            }
        }

        return new ElementDefinition(multipliers, minDamage, minIncoming);
    }

    public static boolean isLoaded() {
        return isLoaded;
    }

    public static Set<ResourceLocation> getAllElementIds() {
        if (!isLoaded) {
            Elementalcompat.LOGGER.warn("尝试访问未初始化的元素数据");
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(ELEMENT_DEFINITIONS.keySet());
    }

    public static Optional<ElementDefinition> getElementDefinition(ResourceLocation elementId) {
        if (!isLoaded) return Optional.empty();
        return Optional.ofNullable(ELEMENT_DEFINITIONS.get(elementId));
    }

    public static float getMultiplier(ResourceLocation attacker, ResourceLocation defender) {
        if (!isLoaded) return 1.0f;
        return getElementDefinition(attacker)
                .map(def -> def.getMultiplier(defender))
                .orElse(1.0f);
    }

    public static float getMinDamage(ResourceLocation element) {
        return getElementDefinition(element)
                .map(ElementDefinition::getMinDamage)
                .orElse(1.0f);
    }

    public static float getMinIncoming(ResourceLocation element) {
        return getElementDefinition(element)
                .map(ElementDefinition::getMinIncoming)
                .orElse(0.5f);
    }

    public static class ElementDefinition {
        private final Map<ResourceLocation, Float> multipliers;
        private final float minDamage;
        private final float minIncoming;

        public ElementDefinition(Map<ResourceLocation, Float> multipliers,
                                 float minDamage,
                                 float minIncoming) {
            this.multipliers = Collections.unmodifiableMap(new HashMap<>(multipliers));
            this.minDamage = minDamage;
            this.minIncoming = minIncoming;
        }

        public float getMultiplier(ResourceLocation target) {
            return multipliers.getOrDefault(target, 1.0f);
        }

        public int getInteractionCount() {
            return multipliers.size();
        }

        public float getMinDamage() {
            return minDamage;
        }

        public float getMinIncoming() {
            return minIncoming;
        }
    }
}
