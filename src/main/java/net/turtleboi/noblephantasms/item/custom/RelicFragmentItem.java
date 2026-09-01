package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.relic.RelicFragmentData;

public final class RelicFragmentItem extends Item {
    private final FragmentOrigin origin;

    public RelicFragmentItem(Properties properties) {
        this(properties, null);
    }

    public RelicFragmentItem(Properties properties, FragmentOrigin origin) {
        super(properties.fireResistant());
        this.origin = origin;
    }

    public boolean isUnidentified() {
        return origin != null;
    }

    public FragmentOrigin origin() {
        return origin;
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

    public enum FragmentOrigin {
        GENERIC,
        ARTHURIAN,
        AZTEC,
        EGYPTIAN,
        JAPANESE,
        NORSE
    }
}
