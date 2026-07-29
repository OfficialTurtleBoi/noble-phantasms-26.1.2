package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import top.theillusivec4.curios.api.SlotContext;

public final class ScarabOfKhepriItem extends CurioRelicItem {
    private static final int REPAIR_INTERVAL = 40;

    public ScarabOfKhepriItem(Properties properties) {
        super(properties.rarity(Rarity.RARE));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity().level() instanceof ServerLevel)
                || slotContext.entity().tickCount % REPAIR_INTERVAL != 0) {
            return;
        }

        for (EquipmentSlot slot : EquipmentSlotGroup.HAND) {
            repair(slotContext.entity().getItemBySlot(slot));
        }
        for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
            repair(slotContext.entity().getItemBySlot(slot));
        }
    }

    private static void repair(ItemStack stack) {
        if (stack.isDamaged()) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }
}
