package com.elementalcompat.loader;

import com.elementalcompat.Elementalcompat;
import com.elementalcompat.network.ElementalSyncPacket;
import com.elementalcompat.network.NetworkHandler; // 新增导入
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.network.PacketDistributor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class EntityElementLoader implements PreparableReloadListener {
    public static final Map<ResourceLocation, List<ResourceLocation>> ENTITY_ELEMENTS = new HashMap<>();

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager manager,
                                          ProfilerFiller prepProfiler, ProfilerFiller reloadProfiler,
                                          Executor bgExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, List<ResourceLocation>> tempMap = new HashMap<>();

            // 加载所有entity_elements目录下的JSON
            manager.listResources("entity_elements",
                    id -> id.getPath().endsWith(".json")).forEach((resourceId, resource) -> {
                try (InputStream stream = resource.open()) {
                    JsonObject json = GsonHelper.parse(new InputStreamReader(stream));
                    JsonArray entries = json.getAsJsonArray("entries");

                    for (JsonElement entry : entries) {
                        processEntry(entry.getAsJsonObject(), tempMap);
                    }
                } catch (Exception e) {
                    Elementalcompat.LOGGER.error("Failed to load entity element config", e);
                }
            });
            return tempMap;
        }, bgExecutor).thenAcceptAsync(map -> {
            ENTITY_ELEMENTS.clear();
            ENTITY_ELEMENTS.putAll(map);
            Elementalcompat.LOGGER.info("Loaded {} entity-element mappings", map.size());

            // 同步到客户端（使用NetworkHandler的通道）
            NetworkHandler.CHANNEL.send(  // 修改这里
                    PacketDistributor.ALL.noArg(),
                    new ElementalSyncPacket(Elementalcompat.ELEMENTAL_TYPES)
            );
        }, gameExecutor);
    }

    private void processEntry(JsonObject entry, Map<ResourceLocation, List<ResourceLocation>> map) {
        ResourceLocation entityId = new ResourceLocation(GsonHelper.getAsString(entry, "entity"));
        JsonArray elements = entry.getAsJsonArray("elements");

        List<ResourceLocation> elementIds = new ArrayList<>();
        for (JsonElement element : elements) {
            ResourceLocation elementId = new ResourceLocation(element.getAsString());
            if (Elementalcompat.ELEMENTAL_TYPES.containsKey(elementId)) {
                elementIds.add(elementId);
            } else {
                Elementalcompat.LOGGER.warn("Undefined element type {} for {}", elementId, entityId);
            }
        }

        if (!elementIds.isEmpty()) {
            map.put(entityId, elementIds);
        }
    }
}

