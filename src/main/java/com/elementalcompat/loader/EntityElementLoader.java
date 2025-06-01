package com.elementalcompat.loader;

import com.elementalcompat.Elementalcompat;
import com.elementalcompat.network.ElementalSyncPacket;
import com.elementalcompat.network.NetworkHandler;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;



public class EntityElementLoader implements PreparableReloadListener {
    public static final Map<ResourceLocation, List<ResourceLocation>> ENTITY_ELEMENTS = new HashMap<>();

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier,
                                          ResourceManager manager, ProfilerFiller prepProfiler,
                                          ProfilerFiller reloadProfiler, Executor bgExecutor,
                                          Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, List<ResourceLocation>> tempMap = new HashMap<>();
            loadAllConfigs(manager, tempMap);
            return tempMap;
        }, bgExecutor).thenCompose(barrier::wait).thenAcceptAsync(map -> {
            ENTITY_ELEMENTS.clear();
            ENTITY_ELEMENTS.putAll(map);
            Elementalcompat.LOGGER.info("Loaded {} entity-element mappings", map.size());
            syncToClients();
        }, gameExecutor);
    }

    private void loadAllConfigs(ResourceManager manager, Map<ResourceLocation, List<ResourceLocation>> map) {
        manager.listResources("entity_elements",
                id -> id.getPath().endsWith(".json")).forEach((resourceId, resource) -> {
            try (InputStream stream = resource.open()) {
                JsonObject json = GsonHelper.parse(new InputStreamReader(stream));
                JsonArray entries = json.getAsJsonArray("entries");
                entries.forEach(entry -> processMappingEntry(entry.getAsJsonObject(), map));
            } catch (Exception e) {
                Elementalcompat.LOGGER.error("Failed to load {}: {}", resourceId, e.getMessage());
            }
        });
    }

    private void processMappingEntry(JsonObject entry, Map<ResourceLocation, List<ResourceLocation>> map) {
        ResourceLocation entityId = new ResourceLocation(GsonHelper.getAsString(entry, "entity"));
        JsonArray elements = entry.getAsJsonArray("elements");

        List<ResourceLocation> validElements = new ArrayList<>();
        elements.forEach(element -> {
            ResourceLocation elementId = new ResourceLocation(element.getAsString());
            if (Elementalcompat.ELEMENTAL_TYPES.containsKey(elementId)) {
                validElements.add(elementId);
            } else {
                Elementalcompat.LOGGER.warn("Skipping undefined element {} for {}", elementId, entityId);
            }
        });

        if (!validElements.isEmpty()) {
            map.put(entityId, validElements);
        }
    }

    private void syncToClients() {
        // 仅服务端执行同步
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            // 使用正确的数据包分发方式
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.ALL.noArg(),
                    new ElementalSyncPacket(Collections.emptyMap(), new HashMap<>(ENTITY_ELEMENTS))
            );
        }
    }
}
