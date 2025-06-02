package com.elementalcompat.event;

import com.elementalcompat.Elementalcompat;
import com.elementalcompat.capability.Capabilities; // 关键修复1：引入全局能力管理
import com.elementalcompat.capability.IElementData;
import com.elementalcompat.data.ElementManager;
import com.elementalcompat.network.ElementSyncPacket;
import com.elementalcompat.network.NetworkHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

public class EntitySpawnHandler {


    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity entity) {
            entity.getCapability(Capabilities.ELEMENT_CAP).ifPresent(cap -> {
                // 修复3：使用新的标签加载逻辑
                loadFromEntityTags(entity, cap);

                if (!event.getLevel().isClientSide) {
                    // 关键修复4：使用统一的能力键和网络通道
                    NetworkHandler.CHANNEL.send(
                            PacketDistributor.TRACKING_ENTITY.with(() -> entity),
                            new ElementSyncPacket(entity.getId(), cap.getElements())
                    );
                    Elementalcompat.LOGGER.debug(
                            "[ElementalCompat] 服务端同步元素数据，实体ID: {}",
                            entity.getId()
                    );
                }
            });
        }
    }

    private static void loadFromEntityTags(LivingEntity entity, IElementData cap) {
        // 修复5：优化标签匹配逻辑
        for (ResourceLocation elementId : ElementManager.getAllElementIds()) {
            TagKey<EntityType<?>> tagKey = TagKey.create(
                    Registries.ENTITY_TYPE,
                    new ResourceLocation(Elementalcompat.MODID, "entity_has_" + elementId.getPath())
            );

            if (entity.getType().is(tagKey)) {
                cap.addElement(elementId);
                Elementalcompat.LOGGER.debug(
                        "[ElementalCompat] 实体 {} 加载元素标签: {}",
                        entity.getType().getDescription().getString(),
                        elementId
                );
            }
        }
    }
}
