package com.elementalcompat.data.element;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import com.elementalcompat.Elementalcompat;

import java.util.Map;

public class UnifiedBindRule {
    private final ResourceLocation itemId;
    private final Map<String, String> nbtConditions;
    private final ResourceLocation element;
    private final ElementSlot slot;

    public UnifiedBindRule(ResourceLocation itemId, Map<String, String> nbtConditions, ResourceLocation element, ElementSlot slot) {
        this.itemId = itemId;
        this.nbtConditions = nbtConditions;
        this.element = element;
        this.slot = slot;
    }

    public boolean matches(ItemStack stack, EquipmentSlot contextSlot) {
        ResourceLocation stackItemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (stackItemId == null) {
            Elementalcompat.LOGGER.debug("[规则匹配] 物品无注册ID: {}", stack);
            return false;
        }

        if (!stackItemId.equals(itemId)) {
            Elementalcompat.LOGGER.debug("[规则匹配] 物品ID不匹配: {} != {}", stackItemId, itemId);
            return false;
        }

        if (slot != ElementSlot.ANY) {
            ElementSlot actualSlot = convertToElementSlot(contextSlot);
            if (actualSlot != slot) {
                Elementalcompat.LOGGER.debug("[规则匹配] 槽位不匹配: {} != {}", actualSlot, slot);
                return false;
            }
        }

        //NBT
        if (nbtConditions != null && !nbtConditions.isEmpty()) {
            CompoundTag tag = stack.getTag();
            if (tag == null) {
                Elementalcompat.LOGGER.debug("[规则匹配] 需要NBT但物品无NBT标签");
                return false;
            }

            for (Map.Entry<String, String> entry : nbtConditions.entrySet()) {
                String key = entry.getKey();
                String expectedValue = entry.getValue();

                if (!tag.contains(key)) {
                    Elementalcompat.LOGGER.debug("[规则匹配] 缺少NBT键: {}", key);
                    return false;
                }

                String actualValue = tag.get(key).getAsString();
                if (!actualValue.equals(expectedValue)) {
                    Elementalcompat.LOGGER.debug("[规则匹配] NBT值不匹配: {}={} (应为: {})", key, actualValue, expectedValue);
                    return false;
                }
            }
        }

        Elementalcompat.LOGGER.debug("[规则匹配] 匹配成功: {} -> {} [{}]", itemId, element, slot);
        return true;
    }

    private ElementSlot convertToElementSlot(EquipmentSlot slot) {
        if (slot == null) return ElementSlot.ANY;

        switch (slot) {
            case MAINHAND: return ElementSlot.MAINHAND;
            case OFFHAND: return ElementSlot.OFFHAND;
            case HEAD: return ElementSlot.HEAD;
            case CHEST: return ElementSlot.CHEST;
            case LEGS: return ElementSlot.LEGS;
            case FEET: return ElementSlot.FEET;
            default: return ElementSlot.ANY;
        }
    }

    @Override
    public String toString() {
        return "UnifiedBindRule{" +
                "itemId=" + itemId +
                ", nbtConditions=" + nbtConditions +
                ", element=" + element +
                ", slot=" + slot +
                '}';
    }

    // Getters
    public ResourceLocation getItemId() {
        return itemId;
    }

    public ResourceLocation getElement() {
        return element;
    }

    public ElementSlot getSlot() {
        return slot;
    }
}
