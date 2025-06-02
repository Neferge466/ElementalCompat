package com.elementalcompat.network;

import com.elementalcompat.Elementalcompat;
import com.elementalcompat.capability.Capabilities; // 关键修复1：引入Capabilities类
import com.elementalcompat.capability.IElementData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.function.Supplier;

public class ElementSyncPacket {
    private final int entityId;
    private final List<ResourceLocation> elements;

    public ElementSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.elements = buf.readList(FriendlyByteBuf::readResourceLocation);
    }

    public ElementSyncPacket(int entityId, List<ResourceLocation> elements) {
        this.entityId = entityId;
        this.elements = elements;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeCollection(elements, FriendlyByteBuf::writeResourceLocation);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                Entity entity = level.getEntity(entityId);
                if (entity instanceof LivingEntity livingEntity) {
                    livingEntity.getCapability(Capabilities.ELEMENT_CAP).ifPresent(cap -> {
                        cap.setElements(elements);
                        Elementalcompat.LOGGER.debug("[ElementalCompat] 客户端同步元素数据成功，实体ID: {}", entityId);
                    });
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void register() {
        int packetId = 0;
        SimpleChannel channel = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(Elementalcompat.MODID, "main"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        channel.messageBuilder(ElementSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ElementSyncPacket::encode)
                .decoder(ElementSyncPacket::new)
                .consumerMainThread(ElementSyncPacket::handle)
                .add();

        Elementalcompat.LOGGER.info("[ElementalCompat] 元素同步网络通道已注册");
    }
}
