package net.turtleboi.noblephantasms.item.custom;

import com.mojang.logging.LogUtils;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueOutput;
import net.turtleboi.noblephantasms.item.ModItems;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TrophyHeadItem extends StandingAndWallBlockItem {
    public static final String ENTITY_TYPE_KEY = "EntityType";
    public static final String ENTITY_DATA_KEY = "EntityData";
    public static final String BABY_KEY = "IsBaby";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> NON_APPEARANCE_TAGS = Set.of(
            "Pos", "Motion", "Rotation", "UUID", "Passengers", "Leash", "Brain", "Attributes",
            "active_effects", "ArmorItems", "HandItems", "ArmorDropChances", "HandDropChances",
            "DeathLootTable", "DeathLootTableSeed", "Health", "AbsorptionAmount", "HurtTime",
            "DeathTime", "Fire", "Air", "FallDistance", "OnGround", "PortalCooldown", "Tags",
            "equipment", "drop_chances");

    public TrophyHeadItem(Block block, Block wallBlock, Item.Properties properties) {
        super(block, wallBlock, Direction.DOWN, properties.stacksTo(1));
    }

    public static ItemStack create(LivingEntity livingEntity) {
        Identifier entityType = BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType());
        return create(createData(entityType, saveAppearanceData(livingEntity), livingEntity.isBaby()));
    }

    public static ItemStack create(Identifier entityType) {
        return create(createData(entityType));
    }

    public static ItemStack create(CustomData data) {
        ItemStack itemStack = new ItemStack(ModItems.TROPHY_HEAD.get());
        itemStack.set(DataComponents.CUSTOM_DATA, data);
        return itemStack;
    }

    public static CustomData createData(Identifier entityType) {
        return createData(entityType, null);
    }

    public static CustomData createData(Identifier entityType, @Nullable CompoundTag entityData) {
        return createData(entityType, entityData, entityData != null && inferBaby(entityData));
    }

    private static CustomData createData(Identifier entityType, @Nullable CompoundTag entityData, boolean baby) {
        CompoundTag tag = new CompoundTag();
        tag.putString(ENTITY_TYPE_KEY, entityType.toString());
        tag.putBoolean(BABY_KEY, baby);
        if (entityData != null && !entityData.isEmpty()) {
            tag.put(ENTITY_DATA_KEY, entityData.copy());
        }
        return CustomData.of(tag);
    }

    private static CompoundTag saveAppearanceData(LivingEntity livingEntity) {
        try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(livingEntity.problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, livingEntity.registryAccess());
            livingEntity.saveWithoutId(output);
            CompoundTag tag = output.buildResult();
            NON_APPEARANCE_TAGS.forEach(tag::remove);
            return tag;
        }
    }

    @Override
    public Component getName(ItemStack itemStack) {
        TrophyData trophyData = getTrophyData(itemStack);
        if (trophyData != null) {
            var entityType = BuiltInRegistries.ENTITY_TYPE.getValue(trophyData.entityTypeId());
            String nameKey = trophyData.isBaby()
                    ? "item.noblephantasms.trophy_head.baby_named"
                    : "item.noblephantasms.trophy_head.named";
            return Component.translatable(nameKey, entityType.getDescription());
        }
        return super.getName(itemStack);
    }

    public static @Nullable Identifier getEntityType(ItemStack itemStack) {
        return getEntityType(itemStack.get(DataComponents.CUSTOM_DATA));
    }

    public static @Nullable Identifier getEntityType(@Nullable CustomData data) {
        if (data == null) {
            return null;
        }
        String value = data.copyTag().getStringOr(ENTITY_TYPE_KEY, "");
        return value.isEmpty() ? null : Identifier.tryParse(value);
    }

    public static @Nullable TrophyData getTrophyData(ItemStack itemStack) {
        return getTrophyData(itemStack.get(DataComponents.CUSTOM_DATA));
    }

    public static @Nullable TrophyData getTrophyData(@Nullable CustomData data) {
        Identifier entityTypeId = getEntityType(data);
        return entityTypeId == null || data == null ? null : new TrophyData(entityTypeId, data);
    }

    public record TrophyData(Identifier entityTypeId, CustomData customData) {
        public CompoundTag entityData() {
            return customData.copyTag().getCompound(ENTITY_DATA_KEY)
                    .map(CompoundTag::copy)
                    .orElseGet(CompoundTag::new);
        }

        public boolean hasBabyMarker() {
            return customData.copyTag().contains(BABY_KEY);
        }

        public boolean isBaby() {
            CompoundTag tag = customData.copyTag();
            return tag.contains(BABY_KEY)
                    ? tag.getBooleanOr(BABY_KEY, false)
                    : inferBaby(entityData());
        }
    }

    private static boolean inferBaby(CompoundTag entityData) {
        return entityData.getIntOr("Age", 0) < 0
                || entityData.getBooleanOr("IsBaby", false)
                || entityData.getBooleanOr("Baby", false);
    }
}
