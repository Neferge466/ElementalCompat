package com.elementalcompat.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class Capabilities {
    // 全局唯一能力键
    public static final ResourceLocation ELEMENT_CAP_KEY =
            new ResourceLocation("elementalcompat", "elements");

    // 全局能力实例
    public static final Capability<IElementData> ELEMENT_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});
}
