package com.elementalcompat.event;

import com.elementalcompat.Elementalcompat;
import com.elementalcompat.capability.Capabilities;
import com.elementalcompat.capability.IElementData;
import com.elementalcompat.data.ElementManager;
import com.elementalcompat.network.ElementSyncPacket;
import com.elementalcompat.network.NetworkHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.core.registries.Registries;

import java.util.Set;
import java.util.stream.Collectors;

public class EntitySpawnHandler {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity entity) {
            entity.getCapability(Capabilities.ELEMENT_CAP).ifPresent(cap -> {
                cap.clearElements();
                loadFromEntityTags(entity, cap);

                if (!event.getLevel().isClientSide) {
                    NetworkHandler.CHANNEL.send(
                            PacketDistributor.TRACKING_ENTITY.with(() -> entity),
                            new ElementSyncPacket(entity.getId(), cap.getElements())
                    );
                    Elementalcompat.LOGGER.debug(
                            "[ElementalCompat] 服务端同步元素数据，实体ID: {} 元素: {}",
                            entity.getId(),
                            cap.getElements()
                    );
                }
            });
        }
    }

    private static void loadFromEntityTags(LivingEntity entity, IElementData cap) {
        EntityType<?> entityType = entity.getType();
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        boolean hasElements = false;

        Set<TagKey<EntityType<?>>> actualTags = entityType.builtInRegistryHolder()
                .tags()
                .collect(Collectors.toSet());

        Elementalcompat.LOGGER.debug("[DEBUG] 实体 {} 的标签: {}",
                entityId,
                actualTags.stream()
                        .map(t -> t.location().toString())
                        .collect(Collectors.joining(", "))
        );

        Elementalcompat.LOGGER.debug("[元素列表] 所有注册元素: {}",
                ElementManager.getAllElementIds().stream()
                        .map(ResourceLocation::toString)
                        .collect(Collectors.joining(", "))
        );

        for (ResourceLocation elementId : ElementManager.getAllElementIds()) {
            Elementalcompat.LOGGER.debug("[元素检测] 正在检查元素: {}", elementId);

            ResourceLocation tagId = new ResourceLocation(
                    Elementalcompat.MODID,
                    "entity_has_" + elementId.getPath() // 关键点：确保路径正确
            );

            TagKey<EntityType<?>> elementTag = TagKey.create(
                    Registries.ENTITY_TYPE,
                    tagId
            );

            if (entityType.is(elementTag)) {
                cap.addElement(elementId);
                hasElements = true;
                Elementalcompat.LOGGER.info(
                        "[绑定成功] {} → {} (标签: {})",
                        entityId, elementId, tagId
                );
            } else {
                Elementalcompat.LOGGER.debug(
                        "[跳过] 实体 {} 未关联标签 {}",
                        entityId, tagId
                );
            }
        }

        if (!hasElements) {
            Elementalcompat.LOGGER.warn(
                    "[未绑定] {} | 可能原因：\n" +
                            "1. 元素ID与标签名不匹配\n" +
                            "2. 数据包未加载\n" +
                            "3. 标签文件格式错误",
                    entityId
            );
        }
    }

}
