package com.elementalcompat.capability;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ElementDataImpl implements IElementData {
    private List<ResourceLocation> elements = new ArrayList<>();

    @Override
    public List<ResourceLocation> getElements() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public void setElements(List<ResourceLocation> elements) {
        this.elements = new ArrayList<>(elements);
    }

    @Override
    public void addElement(ResourceLocation element) {
        if (!elements.contains(element)) {
            elements.add(element);
        }
    }

    @Override
    public void removeElement(ResourceLocation element) {
        elements.remove(element);
    }
}
