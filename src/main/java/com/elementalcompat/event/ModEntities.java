package com.elementalcompat.event;

import com.elementalcompat.Elementalcompat;
import com.elementalcompat.capability.Capabilities;
import com.elementalcompat.capability.ElementalProvider;
import com.elementalcompat.data.ElementAttributesLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Elementalcompat.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEntities {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ElementAttributesLoader());
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity) {
            if (!event.getObject().getCapability(Capabilities.ELEMENT_CAP).isPresent()) {
                event.addCapability(
                        Capabilities.ELEMENT_CAP_KEY,
                        new ElementalProvider()
                );
            }
        }
    }
}
