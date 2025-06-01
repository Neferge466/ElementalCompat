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
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        // 注册核心系统
        forgeBus.addListener(this::onAddReloadListener);
        forgeBus.register(new ElementalDamageHandler());
        forgeBus.register(new EntityBinder()); // 新增实体绑定器

        // 初始化网络系统
        NetworkHandler.register();
    }

    // 实体元素属性绑定器（内部类）
    public static class EntityBinder {
        @SubscribeEvent
        public void onEntitySpawn(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide()) return;

            Entity entity = event.getEntity();
            ResourceLocation entityId = EntityType.getKey(entity.getType());

            // 从加载器获取配置的元素列表
            List<ResourceLocation> elements = EntityElementLoader.ENTITY_ELEMENTS.get(entityId);
            if (elements != null && !elements.isEmpty()) {
                applyElementsToEntity(entity, elements);
            }
        }

        private void applyElementsToEntity(Entity entity, List<ResourceLocation> elements) {
            CompoundTag data = entity.getPersistentData();
            ListTag elementList = new ListTag();

            // 过滤有效元素类型
            for (ResourceLocation elementId : elements) {
                if (Elementalcompat.ELEMENTAL_TYPES.containsKey(elementId)) {
                    elementList.add(StringTag.valueOf(elementId.toString()));
                } else {
                    LOGGER.warn("尝试为 {} 绑定无效元素类型: {}",
                            EntityType.getKey(entity.getType()), elementId);
                }
            }

            // 仅当存在有效元素时写入数据
            if (!elementList.isEmpty()) {
                data.put("ElementalTypes", elementList);
                LOGGER.debug("为实体 {} 绑定元素属性: {}",
                        EntityType.getKey(entity.getType()), elements);
            }
        }
    }

    // 数据包加载器注册
    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(ElementalTypeLoader.INSTANCE);
        event.addListener(new EntityElementLoader()); // 新增实体元素加载器
    }
}




