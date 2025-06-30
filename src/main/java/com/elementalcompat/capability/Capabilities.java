package com.elementalcompat.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class Capabilities {
    public static final ResourceLocation ELEMENT_CAP_KEY =
            new ResourceLocation("elementalcompat", "elements");

    public static final Capability<IElementData> ELEMENT_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});
}
