package com.elementalcompat.event;

import com.elementalcompat.Config;
import com.elementalcompat.capability.Capabilities;
import com.elementalcompat.capability.IElementData;
import com.elementalcompat.data.ElementManager;
import com.elementalcompat.Elementalcompat;
import com.elementalcompat.util.ElementalTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CombatEventHandler {
    private static final float STRENGTH_THRESHOLD = 0.9f;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            LivingEntity target = event.getEntity();

            if (attacker == null || target == null) return;

            List<ResourceLocation> attackElements = getAttackElements(attacker);
            List<ResourceLocation> defenseElements = getDefenseElements(target);

            float baseDamage = event.getAmount();
            float multiplier = calculateDamageMultiplier(attackElements, defenseElements);
            float finalDamage = baseDamage * multiplier;

            finalDamage = applyDamageThresholds(attackElements, defenseElements, finalDamage, attacker);


            finalDamage *= Math.max(0.1f, Config.BASE_MULTIPLIER.get());

            event.setAmount(finalDamage);

            Elementalcompat.LOGGER.debug(
                    "[ElementalCompat] 伤害计算完成 | 基础: {} 最终: {} 倍率: {} 攻击元素: {} 防御元素: {}",
                    String.format("%.2f", baseDamage),
                    String.format("%.2f", finalDamage),
                    String.format("%.2f", multiplier),
                    formatElements(attackElements),
                    formatElements(defenseElements)
            );
        }
    }




    private static String formatElements(List<ResourceLocation> elements) {
        if (elements.isEmpty()) return "无";
        return elements.stream()
                .map(rl -> {

                    String display = rl.getPath()
                            .replace("_element", "")
                            .replace("_", " ");

                    return rl.getNamespace().equals("elementalcompat") ?
                            display :
                            String.format("%s:%s", rl.getNamespace(), display);
                })
                .collect(Collectors.joining(" + "));
    }





    private static float calculateDamageMultiplier(List<ResourceLocation> attackElements,
                                                   List<ResourceLocation> defenseElements) {
        float multiplier = 1.0f;

        for (ResourceLocation atkElement : attackElements) {
            for (ResourceLocation defElement : defenseElements) {
                try {
                    float ratio = ElementManager.getMultiplier(atkElement, defElement);
                    multiplier *= ratio;

                    Elementalcompat.LOGGER.trace(
                            "元素交互: {} -> {} | 倍率: {:.2f}",
                            formatSingleElement(atkElement),
                            formatSingleElement(defElement),
                            ratio
                    );
                } catch (Exception e) {
                    Elementalcompat.LOGGER.error("元素倍率计算错误: {} -> {} | {}",
                            atkElement, defElement, e.getMessage());
                }
            }
        }
        return Math.max(0.01f, multiplier);
    }


    private static String formatSingleElement(ResourceLocation element) {
        return element.getPath()
                .replace("_element", "")
                .replace("_", " ");
    }


    private static float applyDamageThresholds(List<ResourceLocation> attackElements,
                                               List<ResourceLocation> defenseElements,
                                               float currentDamage,
                                               LivingEntity attacker) {
        float modifiedDamage = currentDamage;

        if (attacker instanceof Player player && checkAttackStrength(player)) {
            for (ResourceLocation element : attackElements) {
                float minDamage = ElementManager.getMinDamage(element);
                if (modifiedDamage < minDamage) {
                    modifiedDamage = minDamage;
                    Elementalcompat.LOGGER.debug("[攻击保护] {} 触发最小伤害: {:.2f}", element, minDamage);
                    break;
                }
            }
        }

        for (ResourceLocation element : defenseElements) {
            float minIncoming = ElementManager.getMinIncoming(element);
            if (modifiedDamage < minIncoming) {
                modifiedDamage = minIncoming;
                Elementalcompat.LOGGER.debug("[防御保护] {} 触发最小承伤: {:.2f}", element, minIncoming);
                break;
            }
        }

        return Math.max(0.01f, modifiedDamage);
    }

    private static List<ResourceLocation> getAttackElements(LivingEntity entity) {
        List<ResourceLocation> elements = new ArrayList<>();

        ItemStack mainHand = entity.getMainHandItem();
        ItemStack offHand = entity.getOffhandItem();

        if (mainHand.hasTag()) {
            CompoundTag mainTag = mainHand.getTag();
            if (mainTag.contains(ElementalTags.MAINHAND_ELEMENT)) {
                elements.add(new ResourceLocation(mainTag.getString(ElementalTags.MAINHAND_ELEMENT)));
            }
        }

        if (offHand.hasTag()) {
            CompoundTag offTag = offHand.getTag();
            if (offTag.contains(ElementalTags.OFFHAND_ELEMENT)) {
                elements.add(new ResourceLocation(offTag.getString(ElementalTags.OFFHAND_ELEMENT)));
            }
        }

        if (elements.isEmpty()) {
            elements = entity.getCapability(Capabilities.ELEMENT_CAP)
                    .map(IElementData::getElements)
                    .orElse(Collections.emptyList());
        }

        return elements;
    }

    private static List<ResourceLocation> getDefenseElements(LivingEntity entity) {
        List<ResourceLocation> elements = new ArrayList<>();

        for (ItemStack armor : entity.getArmorSlots()) {
            if (armor.hasTag()) {
                CompoundTag tag = armor.getTag();

                if (tag.contains(ElementalTags.ARMOR_HEAD_ELEMENT)) {
                    elements.add(new ResourceLocation(tag.getString(ElementalTags.ARMOR_HEAD_ELEMENT)));
                }

                if (tag.contains(ElementalTags.ARMOR_CHEST_ELEMENT)) {
                    elements.add(new ResourceLocation(tag.getString(ElementalTags.ARMOR_CHEST_ELEMENT)));
                }

                if (tag.contains(ElementalTags.ARMOR_LEGS_ELEMENT)) {
                    elements.add(new ResourceLocation(tag.getString(ElementalTags.ARMOR_LEGS_ELEMENT)));
                }

                if (tag.contains(ElementalTags.ARMOR_FEET_ELEMENT)) {
                    elements.add(new ResourceLocation(tag.getString(ElementalTags.ARMOR_FEET_ELEMENT)));
                }
            }
        }

        if (elements.isEmpty()) {
            elements = entity.getCapability(Capabilities.ELEMENT_CAP)
                    .map(IElementData::getElements)
                    .orElse(Collections.emptyList());
        }

        return elements;
    }

    private static boolean checkAttackStrength(Player player) {
        return player.getAttackStrengthScale(0.5f) >= STRENGTH_THRESHOLD;
    }
}
