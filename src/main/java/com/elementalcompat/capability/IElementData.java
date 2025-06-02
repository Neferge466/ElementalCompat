package com.elementalcompat.capability;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public interface IElementData {
    List<ResourceLocation> getElements();
    void setElements(List<ResourceLocation> elements);
    void addElement(ResourceLocation element);
    void removeElement(ResourceLocation element);
}
