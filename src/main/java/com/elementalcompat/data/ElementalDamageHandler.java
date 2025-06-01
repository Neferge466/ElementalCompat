package com.elementalcompat.data;

import com.elementalcompat.Elementalcompat;
import com.elementalcompat.ElementalType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ElementalDamageHandler {

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            // 添加泛型类型声明
            Map<ResourceLocation, ElementalType> types = Elementalcompat.ELEMENTAL_TYPES;

            List<ResourceLocation> attackerTypes = getElementalTypes(attacker);
            List<ResourceLocation> defenderTypes = getElementalTypes(event.getEntity());

            float multiplier = 1.0f;

            for (ResourceLocation attackType : attackerTypes) {
                // 显式指定Map类型
                ElementalType aType = types.get(attackType);
                if (aType == null) continue;

                for (ResourceLocation defenseType : defenderTypes) {
                    ElementalType dType = types.get(defenseType);
                    if (dType == null) continue;

                    multiplier *= aType.getEffectivenessAgainst(dType);
                }
            }

            if (multiplier != 1.0f) {
                event.setAmount(event.getAmount() * multiplier);
            }
        }
    }

    private List<ResourceLocation> getElementalTypes(Entity entity) {
        CompoundTag tag = entity.getPersistentData();
        if (tag.contains("ElementalTypes", Tag.TAG_LIST)) {
            ListTag list = tag.getList("ElementalTypes", Tag.TAG_STRING);
            return list.stream()
                    .map(t -> new ResourceLocation(t.getAsString()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}

