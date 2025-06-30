package com.elementalcompat.data;

import com.elementalcompat.Elementalcompat;
import com.elementalcompat.data.element.ElementSlot;
import com.elementalcompat.data.element.UnifiedBindRule;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UnifiedElementLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(UnifiedBindRule.class, new RuleDeserializer())
            .create();

    private static final Map<ResourceLocation, List<UnifiedBindRule>> ALL_RULES = new ConcurrentHashMap<>();

    public UnifiedElementLoader() {
        super(GSON, "element_bindings");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap,
                         ResourceManager manager,
                         ProfilerFiller profiler) {
        ALL_RULES.clear();
        Elementalcompat.LOGGER.info("[绑定加载] 开始加载元素绑定配置...");
        final int[] totalRules = {0};

        Elementalcompat.LOGGER.info("[绑定加载] 找到 {} 个配置文件", jsonMap.size());

        Elementalcompat.LOGGER.info("[RESOURCE DEBUG] 资源管理器命名空间: {}", manager.getNamespaces());
        for (String namespace : manager.getNamespaces()) {
            Elementalcompat.LOGGER.info("[RESOURCE DEBUG] 扫描命名空间: {}", namespace);

            try {
                Elementalcompat.LOGGER.info("[RESOURCE DEBUG] 列出 {} 命名空间中的资源:", namespace);
                Map<ResourceLocation, Resource> resources = manager.listResources("element_bindings",
                        rl -> rl.getNamespace().equals(namespace) && rl.getPath().endsWith(".json")
                );

                for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                    ResourceLocation rl = entry.getKey();
                    Elementalcompat.LOGGER.info("[RESOURCE DEBUG]  找到资源: {}", rl);

                    try (InputStream stream = entry.getValue().open()) {
                        JsonElement json = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonElement.class);
                        Elementalcompat.LOGGER.info("[RESOURCE DEBUG]  成功解析资源: {}", rl);
                    } catch (Exception e) {
                        Elementalcompat.LOGGER.error("[RESOURCE DEBUG]  解析资源失败: {} | {}", rl, e.getMessage());
                    }
                }
            } catch (Exception e) {
                Elementalcompat.LOGGER.error("[RESOURCE DEBUG] 列出资源时出错: {}", e.getMessage());
            }

            ResourceLocation testResource = new ResourceLocation(
                    namespace, "element_bindings/weapons.json"
            );

            try {
                Optional<Resource> resource = manager.getResource(testResource);
                if (resource.isPresent()) {
                    Elementalcompat.LOGGER.info("[RESOURCE DEBUG] 找到测试资源: {}", testResource);

                    try (InputStream stream = resource.get().open()) {
                        JsonElement json = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonElement.class);
                        Elementalcompat.LOGGER.info("[RESOURCE DEBUG]  成功解析测试资源: {}", testResource);
                    } catch (Exception e) {
                        Elementalcompat.LOGGER.error("[RESOURCE DEBUG]  解析测试资源失败: {} | {}", testResource, e.getMessage());
                    }
                } else {
                    Elementalcompat.LOGGER.warn("[RESOURCE DEBUG] 未找到测试资源: {}", testResource);
                }
            } catch (Exception e) {
                Elementalcompat.LOGGER.error("[RESOURCE DEBUG] 资源检查失败: {}", e.getMessage());
            }
        }

        if (jsonMap.isEmpty()) {
            Elementalcompat.LOGGER.error("[绑定加载] 错误：未找到任何配置文件！");
            Elementalcompat.LOGGER.error("[绑定加载] 可能原因：");
            Elementalcompat.LOGGER.error("1. 资源文件未放置在正确位置");
            Elementalcompat.LOGGER.error("2. 资源文件格式错误");
            Elementalcompat.LOGGER.error("3. 资源路径配置错误");
            Elementalcompat.LOGGER.error("4. 资源文件未包含在构建中");

            Elementalcompat.LOGGER.error("[RESOURCE DEBUG] 可用资源包：");
            for (PackResources pack : manager.listPacks().toList()) {
                Elementalcompat.LOGGER.error(" - {}: {}", pack.packId(), pack.getNamespaces(PackType.SERVER_DATA));
            }
        } else {
            Elementalcompat.LOGGER.info("[绑定加载] 找到以下配置文件：");
            for (ResourceLocation id : jsonMap.keySet()) {
                Elementalcompat.LOGGER.info(" - {}", id);
            }
        }

        jsonMap.forEach((id, json) -> {
            try {
                Elementalcompat.LOGGER.info("[绑定加载] 处理配置文件: {}", id);

                if (!json.isJsonArray()) {
                    Elementalcompat.LOGGER.error("[绑定加载] 配置文件 {} 不是有效的 JSON 数组", id);
                    return;
                }

                JsonArray rulesArray = json.getAsJsonArray();
                List<UnifiedBindRule> rules = new ArrayList<>();

                for (JsonElement element : rulesArray) {
                    try {
                        UnifiedBindRule rule = GSON.fromJson(element, UnifiedBindRule.class);
                        rules.add(rule);
                        Elementalcompat.LOGGER.debug("[绑定加载] 添加规则: {} -> {} [{}]",
                                rule.getItemId(), rule.getElement(), rule.getSlot());
                    } catch (JsonParseException e) {
                        Elementalcompat.LOGGER.error("[绑定加载] 规则解析失败: {} | {}", element, e.getMessage());
                    }
                }

                ALL_RULES.put(id, rules);
                totalRules[0] += rules.size();
                Elementalcompat.LOGGER.info("[绑定加载] 配置 {} | 规则数: {}", id, rules.size());
            } catch (Exception e) {
                Elementalcompat.LOGGER.error("[绑定加载] 配置加载失败 {}: {}", id, e.getMessage());
                e.printStackTrace();
            }
        });

        Elementalcompat.LOGGER.info("[绑定加载] 共加载 {} 个绑定配置集, {} 条规则", ALL_RULES.size(), totalRules[0]);

        if (totalRules[0] == 0) {
            Elementalcompat.LOGGER.warn("[绑定加载] 警告：未加载任何绑定规则！");
        }
    }

    public static List<UnifiedBindRule> getApplicableRules(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return Collections.emptyList();

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return Collections.emptyList();

        List<UnifiedBindRule> applicableRules = new ArrayList<>();

        for (List<UnifiedBindRule> ruleList : ALL_RULES.values()) {
            for (UnifiedBindRule rule : ruleList) {
                if (rule.matches(stack, slot)) {
                    applicableRules.add(rule);
                }
            }
        }

        Elementalcompat.LOGGER.debug("[规则查询] 物品: {} 槽位: {} → 找到 {} 条规则",
                itemId, slot, applicableRules.size());

        return applicableRules;
    }

    private static class RuleDeserializer implements JsonDeserializer<UnifiedBindRule> {
        @Override
        public UnifiedBindRule deserialize(JsonElement json,
                                           java.lang.reflect.Type typeOfT,
                                           JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject obj = json.getAsJsonObject();

                ResourceLocation itemId = new ResourceLocation(obj.get("item").getAsString());
                ResourceLocation element = new ResourceLocation(obj.get("element").getAsString());
                ElementSlot slot = ElementSlot.fromString(obj.get("slot").getAsString());

                Map<String, String> nbtConditions = new HashMap<>();
                if (obj.has("nbt")) {
                    JsonObject nbtObj = obj.getAsJsonObject("nbt");
                    for (Map.Entry<String, JsonElement> entry : nbtObj.entrySet()) {
                        nbtConditions.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }

                return new UnifiedBindRule(itemId, nbtConditions, element, slot);
            } catch (Exception e) {
                Elementalcompat.LOGGER.error("[绑定加载] 规则解析失败: {} | {}", json, e.getMessage());
                throw new JsonParseException("规则解析失败", e);
            }
        }
    }
}
