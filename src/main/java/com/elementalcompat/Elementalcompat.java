package com.elementalcompat;

import com.elementalcompat.data.ElementalDamageHandler;
import com.elementalcompat.data.ElementalTypeLoader;
import com.elementalcompat.loader.EntityElementLoader;
import com.elementalcompat.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod(Elementalcompat.MODID)
public class Elementalcompat {
    public static final String MODID = "elementalcompat";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Map<ResourceLocation, ElementalType> ELEMENTAL_TYPES = new HashMap<>();


    public Elementalcompat() {
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener(this::onAddReloadListener);
        forgeBus.register(new ElementalDamageHandler());
        forgeBus.register(new EntityBinder());
        NetworkHandler.register();
    }

    public static class EntityBinder {
        @SubscribeEvent
        public void onEntitySpawn(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide()) return;

            Entity entity = event.getEntity();
            ResourceLocation entityId = EntityType.getKey(entity.getType());

            List<ResourceLocation> elements = EntityElementLoader.ENTITY_ELEMENTS.get(entityId);
            if (elements != null && !elements.isEmpty()) {
                applyElementsToEntity(entity, elements);
            }
        }

        private void applyElementsToEntity(Entity entity, List<ResourceLocation> elements) {
            CompoundTag data = entity.getPersistentData();
            ListTag elementList = new ListTag();

            for (ResourceLocation elementId : elements) {
                if (ELEMENTAL_TYPES.containsKey(elementId)) {
                    elementList.add(StringTag.valueOf(elementId.toString()));
                } else {
                    LOGGER.warn("Invalid element binding: {} -> {}",
                            EntityType.getKey(entity.getType()), elementId);
                }
            }

            if (!elementList.isEmpty()) {
                data.put("ElementalTypes", elementList);
                LOGGER.debug("Bound elements to {}: {}",
                        EntityType.getKey(entity.getType()), elements);
            }
        }
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        // 保证加载顺序：先元素类型后实体映射
        event.addListener(ElementalTypeLoader.INSTANCE);
        event.addListener(new EntityElementLoader());
        LOGGER.info("Registered data pack reload listeners");
    }
}

