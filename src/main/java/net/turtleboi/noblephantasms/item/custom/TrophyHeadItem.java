package net.turtleboi.noblephantasms.item.custom;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.mixin.LivingEntityAccessor;
import net.turtleboi.noblephantasms.mixin.MobAccessor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TrophyHeadItem extends StandingAndWallBlockItem {
    public static final String ENTITY_TYPE_KEY = "EntityType";
    public static final String ENTITY_DATA_KEY = "EntityData";
    public static final String BABY_KEY = "IsBaby";
    public static final String NOTE_BLOCK_SOUND_KEY = "NoteBlockSound";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> NON_APPEARANCE_TAGS = Set.of(
            "Pos", "Motion", "Rotation", "UUID", "Passengers", "Leash", "Brain", "Attributes",
            "active_effects", "ArmorItems", "HandItems", "ArmorDropChances", "HandDropChances",
            "DeathLootTable", "DeathLootTableSeed", "Health", "AbsorptionAmount", "HurtTime",
            "DeathTime", "Fire", "Air", "FallDistance", "OnGround", "PortalCooldown", "Tags",
            "equipment", "drop_chances");

    public TrophyHeadItem(Block block, Block wallBlock, Item.Properties properties) {
        super(block, wallBlock, Direction.DOWN,
                properties.stacksTo(1).equippableUnswappable(EquipmentSlot.HEAD));
    }

    public static ItemStack create(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);
            playerHead.set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
            return playerHead;
        }
        Identifier entityType = BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType());
        return create(createData(entityType, saveAppearanceData(livingEntity), livingEntity.isBaby(),
                livingEntity instanceof Mob mob ? getImitationSound(mob) : null));
    }

    public static ItemStack create(Identifier entityType) {
        return create(createData(entityType));
    }

    public static ItemStack create(CustomData data) {
        Item vanillaHead = getVanillaHead(getEntityType(data));
        if (vanillaHead != null) {
            return new ItemStack(vanillaHead);
        }
        ItemStack itemStack = new ItemStack(ModItems.TROPHY_HEAD.get());
        itemStack.set(DataComponents.CUSTOM_DATA, data);
        return itemStack;
    }

    public static ItemStack createRandom() {
        Identifier fallback = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ZOMBIE);
        if (FMLEnvironment.getDist() != Dist.CLIENT) {
            return create(fallback);
        }

        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) {
            return create(fallback);
        }

        List<EntityType<?>> entityTypes = BuiltInRegistries.ENTITY_TYPE.stream().toList();
        int start = ThreadLocalRandom.current().nextInt(entityTypes.size());
        for (int index = 0; index < entityTypes.size(); index++) {
            EntityType<?> entityType = entityTypes.get((start + index) % entityTypes.size());
            try {
                Entity entity = entityType.create(level, EntitySpawnReason.LOAD);
                if (entity instanceof Mob mob
                        && net.turtleboi.noblephantasms.client.renderer.TrophyHeadRenderer.hasRenderableHead(mob)) {
                    return create(mob);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return create(fallback);
    }

    public static CustomData createData(Identifier entityType) {
        return createData(entityType, null);
    }

    public static CustomData createData(Identifier entityType, @Nullable CompoundTag entityData) {
        return createData(entityType, entityData, entityData != null && inferBaby(entityData));
    }

    private static CustomData createData(Identifier entityType, @Nullable CompoundTag entityData, boolean baby) {
        return createData(entityType, entityData, baby, null);
    }

    private static CustomData createData(Identifier entityType, @Nullable CompoundTag entityData, boolean baby,
                                         @Nullable Identifier noteBlockSound) {
        CompoundTag tag = new CompoundTag();
        tag.putString(ENTITY_TYPE_KEY, entityType.toString());
        tag.putBoolean(BABY_KEY, baby);
        if (entityData != null && !entityData.isEmpty()) {
            tag.put(ENTITY_DATA_KEY, entityData.copy());
        }
        if (noteBlockSound != null) {
            tag.putString(NOTE_BLOCK_SOUND_KEY, noteBlockSound.toString());
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

    public static @Nullable Identifier getNoteBlockSound(@Nullable TrophyData trophyData, Level level) {
        if (trophyData == null) {
            return null;
        }
        Identifier storedSound = trophyData.noteBlockSound();
        if (storedSound != null) {
            return storedSound;
        }
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(trophyData.entityTypeId());
        if (entityType == null) {
            return null;
        }
        try {
            Entity entity = entityType.create(level, EntitySpawnReason.LOAD);
            return entity instanceof Mob mob ? getImitationSound(mob) : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static @Nullable Identifier getImitationSound(Mob mob) {
        SoundEvent sound = ((MobAccessor) mob).noblePhantasms$getAmbientSound();
        if (sound == null) {
            sound = ((LivingEntityAccessor) mob).noblePhantasms$getDeathSound();
        }
        return sound != null ? BuiltInRegistries.SOUND_EVENT.getKey(sound) : null;
    }

    private static @Nullable Item getVanillaHead(@Nullable Identifier entityTypeId) {
        if (entityTypeId == null) {
            return null;
        }
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(entityTypeId);
        if (entityType == EntityType.SKELETON) {
            return Items.SKELETON_SKULL;
        }
        if (entityType == EntityType.WITHER_SKELETON) {
            return Items.WITHER_SKELETON_SKULL;
        }
        if (entityType == EntityType.ZOMBIE) {
            return Items.ZOMBIE_HEAD;
        }
        if (entityType == EntityType.CREEPER) {
            return Items.CREEPER_HEAD;
        }
        if (entityType == EntityType.PIGLIN) {
            return Items.PIGLIN_HEAD;
        }
        if (entityType == EntityType.ENDER_DRAGON) {
            return Items.DRAGON_HEAD;
        }
        if (entityType == EntityType.PLAYER) {
            return Items.PLAYER_HEAD;
        }
        return null;
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

        public @Nullable Identifier noteBlockSound() {
            String value = customData.copyTag().getStringOr(NOTE_BLOCK_SOUND_KEY, "");
            return value.isEmpty() ? null : Identifier.tryParse(value);
        }
    }

    private static boolean inferBaby(CompoundTag entityData) {
        return entityData.getIntOr("Age", 0) < 0
                || entityData.getBooleanOr("IsBaby", false)
                || entityData.getBooleanOr("Baby", false);
    }
}
