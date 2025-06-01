package com.elementalcompat.network;

import com.elementalcompat.Elementalcompat;
import com.elementalcompat.ElementalType;
import com.elementalcompat.loader.EntityElementLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ElementalSyncPacket {
    private final Map<ResourceLocation, ElementalType> elementalTypes;
    private final Map<ResourceLocation, List<ResourceLocation>> entityElements;

    // 全量数据构造器
    public ElementalSyncPacket(Map<ResourceLocation, ElementalType> types,
                               Map<ResourceLocation, List<ResourceLocation>> entities) {
        this.elementalTypes = new HashMap<>(types);
        this.entityElements = new HashMap<>(entities);
    }

    // 单数据类型构造器（保持向后兼容）
    public ElementalSyncPacket(Map<ResourceLocation, ElementalType> types) {
        this(types, Collections.emptyMap());
    }

    public static ElementalSyncPacket decode(FriendlyByteBuf buf) {
        // 读取元素类型数据
        Map<ResourceLocation, ElementalType> types = buf.readMap(
                FriendlyByteBuf::readResourceLocation,
                b -> new ElementalType(
                        b.readResourceLocation(),
                        b.readInt(),
                        b.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readFloat),
                        b.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readFloat),
                        b.readList(FriendlyByteBuf::readResourceLocation)
                )
        );

        // 读取实体元素映射
        Map<ResourceLocation, List<ResourceLocation>> entities = buf.readMap(
                FriendlyByteBuf::readResourceLocation,
                b -> b.readList(FriendlyByteBuf::readResourceLocation)
        );

        return new ElementalSyncPacket(types, entities);
    }

    public static void encode(ElementalSyncPacket packet, FriendlyByteBuf buf) {
        // 写入元素类型数据
        buf.writeMap(packet.elementalTypes,
                FriendlyByteBuf::writeResourceLocation,
                (b, t) -> {
                    b.writeResourceLocation(t.id());
                    b.writeInt(t.color());
                    b.writeMap(t.strengths(), FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeFloat);
                    b.writeMap(t.weaknesses(), FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeFloat);
                    b.writeCollection(t.immunities(), FriendlyByteBuf::writeResourceLocation);
                }
        );

        // 写入实体元素映射
        buf.writeMap(packet.entityElements,
                FriendlyByteBuf::writeResourceLocation,
                (b, list) -> b.writeCollection(list, FriendlyByteBuf::writeResourceLocation)
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 更新元素类型（原子化替换）
            if (!elementalTypes.isEmpty()) {
                Elementalcompat.ELEMENTAL_TYPES.clear();
                Elementalcompat.ELEMENTAL_TYPES.putAll(elementalTypes);
            }

            // 更新实体元素映射（原子化替换）
            if (!entityElements.isEmpty()) {
                EntityElementLoader.ENTITY_ELEMENTS.clear();
                EntityElementLoader.ENTITY_ELEMENTS.putAll(entityElements);
            }

            Elementalcompat.LOGGER.debug("Synced {} elements and {} entities",
                    elementalTypes.size(), entityElements.size());
        });
        ctx.get().setPacketHandled(true);
    }
}
