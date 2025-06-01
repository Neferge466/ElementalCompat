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
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, ElementalType> types = new HashMap<>();
            manager.listResources("elemental_types",
                    id -> id.getPath().endsWith(".json")).forEach((resourceId, resource) -> {
                try {
                    JsonObject json = GSON.fromJson(new InputStreamReader(resource.open()), JsonObject.class);
                    ResourceLocation typeId = buildTypeId(resourceId);
                    parseAndRegisterType(typeId, json, types);
                } catch (Exception e) {
                    Elementalcompat.LOGGER.error("Failed to load {}: {}", resourceId, e.getMessage());
                }
            });
            return types;
        }, backgroundExecutor).thenCompose(stage::wait).thenAcceptAsync(types -> {
            Elementalcompat.ELEMENTAL_TYPES.clear();
            Elementalcompat.ELEMENTAL_TYPES.putAll(types);
            Elementalcompat.LOGGER.info("Loaded {} elemental types", types.size());

            if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
                NetworkHandler.CHANNEL.send(
                        PacketDistributor.ALL.noArg(),
                        new ElementalSyncPacket(types)
                );
            }
        }, gameExecutor);
    }

    private ResourceLocation buildTypeId(ResourceLocation resourceId) {
        String path = resourceId.getPath()
                .replace("elemental_types/", "")
                .replace(".json", "");
        return new ResourceLocation(resourceId.getNamespace(), path);
    }

    private void parseAndRegisterType(ResourceLocation typeId, JsonObject json,
                                      Map<ResourceLocation, ElementalType> types) {
        try {
            // 颜色解析增强
            String colorStr = json.get("color").getAsString();
            if (!colorStr.startsWith("#")) colorStr = "#" + colorStr;
            int color = Color.decode(colorStr).getRGB();

            // 关系解析
            Map<ResourceLocation, Float> strengths = parseRelation(json.getAsJsonObject("strengths"));
            Map<ResourceLocation, Float> weaknesses = parseRelation(json.getAsJsonObject("weaknesses"));

            // 免疫列表
            List<ResourceLocation> immunities = new ArrayList<>();
            json.getAsJsonArray("immunities").forEach(e ->
                    immunities.add(new ResourceLocation(e.getAsString()))
            );

            types.put(typeId, new ElementalType(typeId, color, strengths, weaknesses, immunities));
            Elementalcompat.LOGGER.debug("Registered elemental type: {}", typeId);
        } catch (Exception e) {
            Elementalcompat.LOGGER.error("Failed to parse {}: {}", typeId, e.getMessage());
        }
    }

    private Map<ResourceLocation, Float> parseRelation(JsonObject json) {
        Map<ResourceLocation, Float> map = new HashMap<>();
        json.entrySet().forEach(entry -> {
            try {
                ResourceLocation target = new ResourceLocation(entry.getKey());
                float value = entry.getValue().getAsFloat();
                map.put(target, value);
            } catch (Exception e) {
                Elementalcompat.LOGGER.warn("Invalid relation entry: {}", entry.getKey());
            }
        });
        return map;
    }
}
