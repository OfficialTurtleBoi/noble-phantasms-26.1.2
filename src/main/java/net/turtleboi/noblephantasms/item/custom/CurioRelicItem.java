package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public abstract class CurioRelicItem extends Item implements ICurioItem {
    protected CurioRelicItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, net.minecraft.world.item.ItemStack stack) {
        return true;
    }

    public static boolean isEquipped(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.isEquipped(item))
                .orElse(false);
    }
}
