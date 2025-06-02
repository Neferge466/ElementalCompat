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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Elementalcompat.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ElementManager {
    // 使用线程安全的Map以应对并行加载
    private static final Map<ResourceLocation, ElementDefinition> ELEMENT_DEFINITIONS = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 添加加载状态追踪
    private static volatile boolean isLoaded = false;

    // 注册到Forge的Reload系统
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(createReloadListener());
        Elementalcompat.LOGGER.info("[ElementManager] 已注册资源加载监听器");
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

                    Elementalcompat.LOGGER.info("[元素加载] 成功加载 {} | 包含 {} 个交互规则",
                            id, definition.getInteractionCount());
                } catch (Exception e) {
                    Elementalcompat.LOGGER.error("元素定义加载失败 {}: {}", id, e.getMessage());
                    Elementalcompat.LOGGER.debug("错误堆栈:", e);
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

    // 增强型解析方法
    private static ElementDefinition parseDefinition(JsonObject json) throws IllegalArgumentException {
        if (!json.has("interactions")) {
            throw new IllegalArgumentException("缺失必需的'interactions'字段");
        }

        JsonObject interactions = json.getAsJsonObject("interactions");
        Map<ResourceLocation, Float> multipliers = new HashMap<>(interactions.size());

        for (Map.Entry<String, JsonElement> entry : interactions.entrySet()) {
            try {
                ResourceLocation target = new ResourceLocation(entry.getKey());
                float value = entry.getValue().getAsFloat();

                if (value < 0) {
                    Elementalcompat.LOGGER.warn("无效的交互值 {} → {}，值不应小于0", target, value);
                    value = 0;
                }

                multipliers.put(target, value);
            } catch (Exception e) {
                throw new IllegalArgumentException("解析交互条目失败: " + entry.getKey(), e);
            }
        }

        return new ElementDefinition(multipliers);
    }

    // 状态检查方法
    public static boolean isLoaded() {
        return isLoaded;
    }

    // 安全访问方法
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

    public static class ElementDefinition {
        private final Map<ResourceLocation, Float> multipliers;

        public ElementDefinition(Map<ResourceLocation, Float> multipliers) {
            this.multipliers = Collections.unmodifiableMap(multipliers);
        }

        public float getMultiplier(ResourceLocation target) {
            return multipliers.getOrDefault(target, 1.0f);
        }

        public int getInteractionCount() {
            return multipliers.size();
        }
    }
}
