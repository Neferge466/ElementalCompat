package com.elementalcompat.network;

import com.elementalcompat.Elementalcompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Elementalcompat.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ElementalSyncPacket.class,
                ElementalSyncPacket::encode,
                ElementalSyncPacket::decode,
                ElementalSyncPacket::handle);
    }

    public static void sendToAllClients(ElementalSyncPacket packet) {
        CHANNEL.sendToServer(packet);
    }
}
