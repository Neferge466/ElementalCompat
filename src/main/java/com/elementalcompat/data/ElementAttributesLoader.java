package com.elementalcompat.data;

import com.elementalcompat.Elementalcompat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import java.util.*;

public class ElementAttributesLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final Map<EntityType<?>, List<ResourceLocation>> ELEMENT_MAP = new HashMap<>();

    // 关键：资源路径必须对应实际文件位置
    public ElementAttributesLoader() {
        // 对应 resources/data/elementalcompat/element_attributes 目录
        super(GSON, "elementalcompat/element_attributes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources,
                         ResourceManager manager,
                         ProfilerFiller profiler) {
        ELEMENT_MAP.clear();
        resources.forEach((id, json) -> {
            try {
                EntityType<?> type = EntityType.byString(id.getPath()).orElseThrow();
                JsonArray elements = json.getAsJsonObject().getAsJsonArray("elements");
                List<ResourceLocation> elementList = new ArrayList<>();
                elements.forEach(e -> elementList.add(new ResourceLocation(e.getAsString())));
                ELEMENT_MAP.put(type, elementList);
            } catch (Exception e) {
                Elementalcompat.LOGGER.error("Error loading element attributes: {}", id, e);
            }
        });
    }
}
