package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.relic.RelicFragmentData;

public final class RelicFragmentItem extends Item {
    public RelicFragmentItem(Properties properties) {
        super(properties.fireResistant());
    }

    public static ItemStack create(Item item, RelicFragmentData data, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(ModDataComponents.RELIC_FRAGMENT.get(), data);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        RelicFragmentData data = stack.get(ModDataComponents.RELIC_FRAGMENT.get());
        if (data == null) {
            return super.getName(stack);
        }
        Component relicName = Component.translatable("item." + data.relicId().getNamespace()
                + "." + data.relicId().getPath());
        return Component.translatable("item.noblephantasms.relic_fragment.named", relicName);
    }
}
