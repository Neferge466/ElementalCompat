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

    // 声明静态加载器实例
    public static final ElementalTypeLoader TYPE_LOADER = ElementalTypeLoader.INSTANCE;
    public static final EntityElementLoader ENTITY_LOADER = new EntityElementLoader();

    public Elementalcompat() {
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener(this::onAddReloadListener);
        forgeBus.register(new ElementalDamageHandler());
        forgeBus.register(new EntityBinder());
        NetworkHandler.register();

        // 建立加载器依赖关系
        ENTITY_LOADER.setDependency(TYPE_LOADER.getLoadedFuture());
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
        // 确保正确的注册顺序和实例使用
        event.addListener(TYPE_LOADER);
        event.addListener(ENTITY_LOADER);
        LOGGER.info("Registered data pack reload listeners in correct order");
    }
}
