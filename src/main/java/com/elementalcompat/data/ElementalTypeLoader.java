package com.elementalcompat.data;

import com.elementalcompat.ElementalType;
import com.elementalcompat.Elementalcompat;
import com.elementalcompat.network.NetworkHandler;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.awt.Color;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class ElementalTypeLoader implements PreparableReloadListener {
    public static final ElementalTypeLoader INSTANCE = new ElementalTypeLoader();
    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager manager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() ->
                                manager.listResources("elemental_types",
                                        id -> id.getNamespace().equals(Elementalcompat.MODID) && id.getPath().endsWith(".json"))
                        , backgroundExecutor)
                .thenCompose(resources -> {
                    Map<ResourceLocation, ElementalType> types = new HashMap<>();
                    List<CompletableFuture<Void>> futures = resources.stream()
                            .map(id -> parseElementalType(id, manager, types))
                            .collect(Collectors.toList());
                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApplyAsync(v -> types, gameExecutor);
                })
                .thenAcceptAsync(loadedTypes -> {
                    Elementalcompat.ELEMENTAL_TYPES.clear();
                    Elementalcompat.ELEMENTAL_TYPES.putAll(loadedTypes);
                    NetworkHandler.sendToAllClients(new ElementalSyncPacket(loadedTypes));
                }, gameExecutor);
    }

    private CompletableFuture<Void> parseElementalType(ResourceLocation id, ResourceManager manager,
                                                       Map<ResourceLocation, ElementalType> types) {
        return CompletableFuture.runAsync(() -> {
            try {
                Resource resource = manager.getResource(id).orElseThrow();
                JsonObject json = GSON.fromJson(new InputStreamReader(resource.open()), JsonObject.class);

                String path = id.getPath()
                        .replace("elemental_types/", "")
                        .replace(".json", "");
                ResourceLocation typeId = new ResourceLocation(id.getNamespace(), path);

                int color = Color.decode(json.get("color").getAsString()).getRGB();

                Map<ResourceLocation, Float> strengths = parseMultipliers(json.getAsJsonObject("strengths"));
                Map<ResourceLocation, Float> weaknesses = parseMultipliers(json.getAsJsonObject("weaknesses"));
                List<ResourceLocation> immunities = parseList(json.getAsJsonArray("immunities"));

                types.put(typeId, new ElementalType(typeId, color, strengths, weaknesses, immunities));
            } catch (Exception e) {
                Elementalcompat.LOGGER.error("Failed to load elemental type {}", id, e);
            }
        });
    }

    private Map<ResourceLocation, Float> parseMultipliers(JsonObject json) {
        Map<ResourceLocation, Float> map = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            ResourceLocation key = new ResourceLocation(entry.getKey());
            float value = entry.getValue().getAsFloat();
            map.put(key, value);
        }
        return map;
    }

    private List<ResourceLocation> parseList(JsonArray json) {
        List<ResourceLocation> list = new ArrayList<>();
        for (JsonElement element : json) {
            list.add(new ResourceLocation(element.getAsString()));
        }
        return list;
    }
}
