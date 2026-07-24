package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.turtleboi.noblephantasms.item.ModItems;
import org.jspecify.annotations.Nullable;

public class TrophyHeadItem extends Item {
    public static final String ENTITY_TYPE_KEY = "EntityType";
    public TrophyHeadItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ItemStack create(LivingEntity livingEntity) {
        ItemStack itemStack = new ItemStack(ModItems.TROPHY_HEAD.get());
        Identifier entityType = BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType());
        CompoundTag tag = new CompoundTag();
        tag.putString(ENTITY_TYPE_KEY, entityType.toString());
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return itemStack;
    }

    @Override
    public Component getName(ItemStack itemStack) {
        Identifier entityTypeId = getEntityType(itemStack);
        if (entityTypeId != null) {
            var entityType = BuiltInRegistries.ENTITY_TYPE.getValue(entityTypeId);
            return Component.translatable("item.noblephantasms.trophy_head.named", entityType.getDescription());
        }
        return super.getName(itemStack);
    }

    public static @Nullable Identifier getEntityType(ItemStack itemStack) {
        CustomData data = itemStack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        String value = data.copyTag().getStringOr(ENTITY_TYPE_KEY, "");
        return value.isEmpty() ? null : Identifier.tryParse(value);
    }
}
