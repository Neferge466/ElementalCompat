package com.elementalcompat.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ElementalProvider implements ICapabilitySerializable<CompoundTag> {
    private final IElementData instance = new ElementDataImpl();
    private final LazyOptional<IElementData> optional = LazyOptional.of(() -> instance);

    // 核心修复：移除Entity参数（无需实体依赖）
    public ElementalProvider() {
        // 初始化默认元素数据
        instance.setElements(new ArrayList<>());
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap,
            @Nullable Direction side
    ) {
        return Capabilities.ELEMENT_CAP.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        ListTag elements = new ListTag();
        instance.getElements().forEach(rl ->
                elements.add(StringTag.valueOf(rl.toString()))
        );
        nbt.put("Elements", elements);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        ListTag elements = nbt.getList("Elements", Tag.TAG_STRING);
        List<ResourceLocation> elementList = new ArrayList<>();
        elements.forEach(tag ->
                elementList.add(new ResourceLocation(tag.getAsString()))
        );
        instance.setElements(elementList);
    }
}




