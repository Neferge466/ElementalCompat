package com.elementalcompat.event;

import com.elementalcompat.Config;
import com.elementalcompat.capability.Capabilities;
import com.elementalcompat.capability.IElementData;
import com.elementalcompat.data.ElementManager;
import com.elementalcompat.Elementalcompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collections;
import java.util.List;

public class CombatEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            LivingEntity target = event.getEntity();

            if (attacker == null || target == null) return;

            float baseDamage = event.getAmount();
            float multiplier = calculateDamageMultiplier(attacker, target);
            float finalDamage = baseDamage * multiplier;

            finalDamage *= Math.max(0.1f, Config.BASE_MULTIPLIER.get());

            event.setAmount(finalDamage);

            Elementalcompat.LOGGER.debug(
                    "[ElementalCompat] 伤害计算完成 | 基础: {} 最终: {} 倍率: {}",
                    baseDamage, finalDamage, multiplier
            );
        }
    }

    private static float calculateDamageMultiplier(LivingEntity attacker, LivingEntity target) {
        float multiplier = 1.0f;
        List<ResourceLocation> attackerElements = getElements(attacker);
        List<ResourceLocation> targetElements = getElements(target);

        Elementalcompat.LOGGER.debug(
                "攻击者元素: {} | 防御者元素: {}",
                attackerElements, targetElements
        );

        if (attackerElements.isEmpty() && targetElements.isEmpty()) return 1.0f;

        for (ResourceLocation atkElement : attackerElements) {
            for (ResourceLocation defElement : targetElements) {
                float ratio = ElementManager.getMultiplier(atkElement, defElement);
                multiplier *= ratio;

                Elementalcompat.LOGGER.trace(
                        "元素交互: {} -> {} | 倍率: {}",
                        atkElement, defElement, ratio
                );
            }
        }
        return Math.max(0.01f, multiplier); // 防止负伤害
    }

    private static List<ResourceLocation> getElements(LivingEntity entity) {
        return entity.getCapability(Capabilities.ELEMENT_CAP)
                .map(IElementData::getElements)
                .orElse(Collections.emptyList());
    }
}
