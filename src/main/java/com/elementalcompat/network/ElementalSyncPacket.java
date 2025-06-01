package com.elementalcompat.network;

import com.elementalcompat.Elementalcompat;
import com.elementalcompat.ElementalType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

public class ElementalSyncPacket {
    private final Map<ResourceLocation, ElementalType> types;

    public ElementalSyncPacket(FriendlyByteBuf buf) {
        types = buf.readMap(
                FriendlyByteBuf::readResourceLocation,
                b -> new ElementalType(
                        b.readResourceLocation(),
                        b.readInt(),
                        b.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readFloat),
                        b.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readFloat),
                        b.readList(FriendlyByteBuf::readResourceLocation)
                )
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeMap(types,
                FriendlyByteBuf::writeResourceLocation,
                (b, t) -> {
                    b.writeResourceLocation(t.id());
                    b.writeInt(t.color());
                    b.writeMap(t.strengths(), FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeFloat);
                    b.writeMap(t.weaknesses(), FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeFloat);
                    b.writeCollection(t.immunities(), FriendlyByteBuf::writeResourceLocation);
                }
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Elementalcompat.ELEMENTAL_TYPES.clear();
            Elementalcompat.ELEMENTAL_TYPES.putAll(types);
        });
        ctx.get().setPacketHandled(true);
    }
}
