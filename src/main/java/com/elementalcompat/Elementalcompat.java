package com.elementalcompat;

import com.elementalcompat.data.ElementalDamageHandler;
import com.elementalcompat.data.ElementalTypeLoader;
import com.elementalcompat.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Mod(Elementalcompat.MODID)
public class Elementalcompat {
    public static final String MODID = "elementalcompat";
    public static final Logger LOGGER = (Logger) LogUtils.getLogger();
    public static final Map<ResourceLocation, ElementalType> ELEMENTAL_TYPES = new HashMap<>();

    public Elementalcompat() {
        IEventBus modEventBus = FMLJavaModLanguageProvider.get().getModEventBus();

        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListener);
        MinecraftForge.EVENT_BUS.register(new ElementalDamageHandler());
        NetworkHandler.register();
    }


    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(ElementalTypeLoader.INSTANCE);
    }
}
