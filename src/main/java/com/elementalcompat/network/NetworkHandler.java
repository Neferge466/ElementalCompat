package com.elementalcompat.network;

import com.elementalcompat.Elementalcompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Elementalcompat.MODID, "main"),
            () -> "1.0",
            "1.0"::equals,
            "1.0"::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ElementSyncPacket.class,
                ElementSyncPacket::encode,
                ElementSyncPacket::new,
                ElementSyncPacket::handle
        );
    }
}

