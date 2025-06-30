package com.elementalcompat.data.element;

import com.elementalcompat.Elementalcompat;
import com.elementalcompat.data.UnifiedElementLoader;
import com.elementalcompat.util.ElementalTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.List;

@Mod.EventBusSubscriber(modid = Elementalcompat.MODID)
public class UniversalBinder {

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        ItemStack stack = event.getCrafting();
        processItem(player, stack);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        checkAllEquipment(player);
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        Player player = event.getEntity();
        ItemStack stack = event.getStack();
        processItem(player, stack);
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        Player player = event.getEntity();
        checkAllEquipment(player);
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            EquipmentSlot slot = event.getSlot();
            ItemStack stack = event.getTo();
            applyElementTags(stack, slot);
        }
    }

    private static void checkAllEquipment(Player player) {
        Elementalcompat.LOGGER.debug("[绑定检查] 开始检查玩家 {} 的所有装备", player.getName().getString());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                applyElementTags(stack, slot);
            }
        }
        Elementalcompat.LOGGER.debug("[绑定检查] 完成检查玩家 {} 的所有装备", player.getName().getString());
    }

    private static void processItem(Player player, ItemStack stack) {
        if (stack.isEmpty()) return;

        Elementalcompat.LOGGER.debug("[绑定处理] 处理物品: {} 玩家: {}",
                ForgeRegistries.ITEMS.getKey(stack.getItem()), player.getName().getString());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack slotStack = player.getItemBySlot(slot);
            if (ItemStack.matches(slotStack, stack)) {
                applyElementTags(stack, slot);
                return;
            }
        }
        applyElementTags(stack, null);
    }

    private static void applyElementTags(ItemStack stack, EquipmentSlot contextSlot) {
        if (stack.isEmpty()) return;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return;

        Elementalcompat.LOGGER.debug("[绑定开始] 检查物品: {} 槽位: {}", itemId, contextSlot);

        List<UnifiedBindRule> rules = UnifiedElementLoader.getApplicableRules(stack, contextSlot);

        if (rules.isEmpty()) {
            Elementalcompat.LOGGER.debug("[绑定检查] 没有适用的规则: {}", itemId);
            return;
        }

        Elementalcompat.LOGGER.debug("[绑定检查] 找到 {} 条规则: {}", rules.size(), itemId);

        CompoundTag tag = stack.getOrCreateTag();
        boolean modified = false;

        for (UnifiedBindRule rule : rules) {
            String tagKey = getSlotTagKey(rule.getSlot());
            String elementStr = rule.getElement().toString();

            if (tag.contains(tagKey) && tag.getString(tagKey).equals(elementStr)) {
                Elementalcompat.LOGGER.debug("[绑定跳过] 已存在相同绑定: {} -> {}", tagKey, elementStr);
                continue;
            }

            tag.putString(tagKey, elementStr);
            modified = true;

            Elementalcompat.LOGGER.info("[应用绑定] {} → {} [{}]",
                    itemId, rule.getElement(), rule.getSlot());
        }

        if (modified) {
            stack.setTag(tag);
            Elementalcompat.LOGGER.debug("[绑定完成] 更新物品NBT: {}", stack.getTag());
        } else {
            Elementalcompat.LOGGER.debug("[绑定完成] 无需更新: {}", itemId);
        }
    }

    private static String getSlotTagKey(ElementSlot slot) {
        return switch (slot) {
            case MAINHAND -> ElementalTags.MAINHAND_ELEMENT;
            case OFFHAND -> ElementalTags.OFFHAND_ELEMENT;
            case HEAD -> ElementalTags.ARMOR_HEAD_ELEMENT;
            case CHEST -> ElementalTags.ARMOR_CHEST_ELEMENT;
            case LEGS -> ElementalTags.ARMOR_LEGS_ELEMENT;
            case FEET -> ElementalTags.ARMOR_FEET_ELEMENT;
            default -> ElementalTags.ANY_ELEMENT;
        };
    }
}
