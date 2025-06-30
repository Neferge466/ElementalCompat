package com.elementalcompat;

import com.elementalcompat.capability.IElementData;
import com.elementalcompat.data.ElementManager;
import com.elementalcompat.data.UnifiedElementLoader;
import com.elementalcompat.event.CombatEventHandler;
import com.elementalcompat.event.EntitySpawnHandler;
import com.elementalcompat.event.ModEntities;
import com.elementalcompat.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Elementalcompat.MODID)
public class Elementalcompat {
    public static final String MODID = "elementalcompat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static boolean DEBUG_MODE = true;

    public Elementalcompat() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        forgeBus.addListener(this::registerResourceListeners);

        forgeBus.register(EntitySpawnHandler.class);
        forgeBus.register(CombatEventHandler.class);
        forgeBus.register(ModEntities.class);

        modBus.addListener(this::registerCapabilities);
        modBus.addListener(this::onCommonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void registerResourceListeners(AddReloadListenerEvent event) {
        LOGGER.info("[资源注册] 开始注册资源加载器...");

        LOGGER.info("[资源注册] 注册元素定义加载器");
        event.addListener(ElementManager.createReloadListener());

        LOGGER.info("[资源注册] 注册绑定规则加载器");
        event.addListener(new UnifiedElementLoader());

        LOGGER.info("[资源注册] 资源加载器注册完成");
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IElementData.class);
        LOGGER.info("[ElementalCompat] 元素能力已注册");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.register();
            LOGGER.info("[ElementalCompat] 网络通道初始化完成");
        });
    }

    public static void debug(String message, Object... args) {
        if (DEBUG_MODE) {
            LOGGER.info("[DEBUG] " + message, args);
        }
    }
}
