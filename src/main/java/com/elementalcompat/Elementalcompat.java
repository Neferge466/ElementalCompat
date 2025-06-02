package com.elementalcompat;

import com.elementalcompat.capability.IElementData;
import com.elementalcompat.data.ElementManager;
import com.elementalcompat.event.CombatEventHandler;
import com.elementalcompat.event.EntitySpawnHandler;
import com.elementalcompat.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
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

    public Elementalcompat() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        // 核心修复1：AddReloadListenerEvent必须注册到Forge总线
        forgeBus.addListener(this::registerResourceListeners);

        // 注册实体事件处理器到Forge总线
        forgeBus.register(EntitySpawnHandler.class);
        forgeBus.register(CombatEventHandler.class);

        // 其他标准注册（Mod总线）
        modBus.addListener(this::registerCapabilities);
        modBus.addListener(this::onCommonSetup);

        // 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    // 资源重载监听器注册方法
    private void registerResourceListeners(AddReloadListenerEvent event) {
        event.addListener(ElementManager.createReloadListener());
        LOGGER.debug("[ElementalCompat] 元素数据重载监听器已注册");
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IElementData.class);
        LOGGER.info("[ElementalCompat] 元素能力已注册");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.register();
            LOGGER.debug("[ElementalCompat] 网络通道初始化完成");
        });
    }
}
