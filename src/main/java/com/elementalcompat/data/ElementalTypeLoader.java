package com.elementalcompat.data;

import com.elementalcompat.ElementalType;
import com.elementalcompat.Elementalcompat;
import com.elementalcompat.network.ElementalSyncPacket;
import com.elementalcompat.network.NetworkHandler;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.PacketDistributor;

import java.awt.Color;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class ElementalTypeLoader implements PreparableReloadListener {
    public static final ElementalTypeLoader INSTANCE = new ElementalTypeLoader();
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, (JsonDeserializer<ResourceLocation>) (json, type, context) ->
                    new ResourceLocation(json.getAsString()))
            .create();

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager manager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() ->
                                manager.listResources("elemental_types",
                                        id -> id.getNamespace().equals(Elementalcompat.MODID) && id.getPath().endsWith(".json")),
                        backgroundExecutor)
                .thenCompose(resources -> {
                    Map<ResourceLocation, ElementalType> types = new HashMap<>();
                    // 关键修复1：使用keySet()获取资源ID流
                    List<CompletableFuture<Void>> futures = resources.keySet().stream()
                            .map(id -> parseElementalType(id, manager, types))
                            .collect(Collectors.toList());
                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApplyAsync(v -> types, gameExecutor);
                })
                .thenCompose(stage::wait)
                .thenAcceptAsync(loadedTypes -> {
                    Elementalcompat.ELEMENTAL_TYPES.clear();
                    Elementalcompat.ELEMENTAL_TYPES.putAll(loadedTypes);

                    // 修复1：使用Forge官方API判断运行环境
                    if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
                        // 修复2：使用正确的数据包发送方法
                        NetworkHandler.CHANNEL.send(
                                PacketDistributor.ALL.noArg(),
                                new ElementalSyncPacket(loadedTypes)
                        );
                    }
                }, gameExecutor);
    }

    private CompletableFuture<Void> parseElementalType(ResourceLocation id, ResourceManager manager,
                                                       Map<ResourceLocation, ElementalType> types) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 关键修复2：正确处理资源路径
                String pathSegment = id.getPath();
                if (pathSegment.startsWith("elemental_types/")) {
                    pathSegment = pathSegment.substring("elemental_types/".length());
                }
                if (pathSegment.endsWith(".json")) {
                    pathSegment = pathSegment.substring(0, pathSegment.length() - 5);
                }
                ResourceLocation typeId = new ResourceLocation(id.getNamespace(), pathSegment);

                Resource resource = manager.getResource(id).orElseThrow();
                JsonObject json = GSON.fromJson(new InputStreamReader(resource.open()), JsonObject.class);

                // 增强字段校验
                if (!json.has("color")) {
                    throw new JsonSyntaxException("Missing required field 'color' in " + id);
                }
                if (!json.has("strengths")) {
                    throw new JsonSyntaxException("Missing required field 'strengths' in " + id);
                }

                // 增强颜色解析
                String colorStr = json.get("color").getAsString();
                if (!colorStr.startsWith("#")) {
                    colorStr = "#" + colorStr;
                }
                int color;
                try {
                    color = Color.decode(colorStr).getRGB();
                } catch (NumberFormatException e) {
                    throw new JsonSyntaxException("Invalid color format: " + colorStr + " in " + id);
                }

                Map<ResourceLocation, Float> strengths = parseMultipliers(json.getAsJsonObject("strengths"));
                Map<ResourceLocation, Float> weaknesses = parseMultipliers(json.getAsJsonObject("weaknesses"));
                List<ResourceLocation> immunities = parseList(json.getAsJsonArray("immunities"));

                types.put(typeId, new ElementalType(typeId, color, strengths, weaknesses, immunities));
            } catch (Exception e) {
                Elementalcompat.LOGGER.error("Failed to load elemental type {}: {}", id, e.getMessage());
            }
        });
    }

    private Map<ResourceLocation, Float> parseMultipliers(JsonObject json) {
        Map<ResourceLocation, Float> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            try {
                ResourceLocation key = new ResourceLocation(entry.getKey());
                float value = entry.getValue().getAsFloat();
                if (value <= 0) {
                    throw new JsonSyntaxException("Multiplier value must be positive");
                }
                map.put(key, value);
            } catch (Exception e) {
                Elementalcompat.LOGGER.warn("Invalid multiplier entry: {}", entry.getKey(), e);
            }
        }
        return map;
    }

    private List<ResourceLocation> parseList(JsonArray json) {
        List<ResourceLocation> list = new ArrayList<>();
        for (JsonElement element : json) {
            try {
                list.add(new ResourceLocation(element.getAsString()));
            } catch (Exception e) {
                Elementalcompat.LOGGER.warn("Invalid resource location: {}", element.getAsString());
            }
        }
        return list;
    }
}
