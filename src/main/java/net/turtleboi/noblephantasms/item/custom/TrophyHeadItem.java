package net.turtleboi.noblephantasms.item.custom;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.npc.villager.VillagerDataHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
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
    public static final String VARIANT_NAME_KEY = "VariantName";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Consumer<CompoundTag> NO_VARIANT_DATA = tag -> {
    };
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
        Item vanillaHead = getVanillaHead(data);
        if (vanillaHead != null) {
            return new ItemStack(vanillaHead);
        }
        ItemStack itemStack = new ItemStack(ModItems.TROPHY_HEAD.get());
        itemStack.set(DataComponents.CUSTOM_DATA, data);
        return itemStack;
    }

    public static List<ItemStack> createCreativeTabHeads() {
        if (FMLEnvironment.getDist() != Dist.CLIENT) {
            return List.of();
        }

        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }

        List<CreativeHeadEntry> heads = new ArrayList<>();
        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            try {
                Entity entity = entityType.create(level, EntitySpawnReason.LOAD);
                if (entity instanceof Mob mob
                        && net.turtleboi.noblephantasms.client.renderer.TrophyHeadRenderer.hasRenderableHead(mob)) {
                    addCreativeHeadVariants(heads, mob, level.registryAccess());
                }
            } catch (RuntimeException ignored) {
            }
        }

        heads.sort(Comparator
                .comparing(CreativeHeadEntry::familyName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(CreativeHeadEntry::familyName)
                .thenComparing(CreativeHeadEntry::variantName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(CreativeHeadEntry::variantName)
                .thenComparing(CreativeHeadEntry::baby));
        return heads.stream().map(CreativeHeadEntry::stack).toList();
    }

    private static void addCreativeHeadVariants(List<CreativeHeadEntry> heads, Mob mob,
                                                RegistryAccess registryAccess) {
        EntityType<?> entityType = mob.getType();
        if (entityType == EntityType.CAT) {
            addRegistryVariants(heads, mob, registryAccess.lookupOrThrow(Registries.CAT_VARIANT));
        } else if (entityType == EntityType.CHICKEN) {
            addRegistryVariants(heads, mob, registryAccess.lookupOrThrow(Registries.CHICKEN_VARIANT));
        } else if (entityType == EntityType.COW) {
            addRegistryVariants(heads, mob, registryAccess.lookupOrThrow(Registries.COW_VARIANT));
        } else if (entityType == EntityType.FROG) {
            addRegistryVariants(heads, mob, registryAccess.lookupOrThrow(Registries.FROG_VARIANT));
        } else if (entityType == EntityType.PIG) {
            addRegistryVariants(heads, mob, registryAccess.lookupOrThrow(Registries.PIG_VARIANT));
        } else if (entityType == EntityType.WOLF) {
            addRegistryVariants(heads, mob, registryAccess.lookupOrThrow(Registries.WOLF_VARIANT));
        } else if (entityType == EntityType.ZOMBIE_NAUTILUS) {
            addRegistryVariants(heads, mob, registryAccess.lookupOrThrow(Registries.ZOMBIE_NAUTILUS_VARIANT));
        } else if (mob instanceof Axolotl) {
            for (Axolotl.Variant variant : Axolotl.Variant.values()) {
                addVariantPair(heads, mob, variant.getSerializedName(),
                        tag -> tag.putInt("Variant", variant.getId()));
            }
        } else if (mob instanceof MushroomCow) {
            for (MushroomCow.Variant variant : MushroomCow.Variant.values()) {
                addVariantPair(heads, mob, variant.getSerializedName(),
                        tag -> tag.putString("Type", variant.getSerializedName()));
            }
        } else if (mob instanceof Horse) {
            for (Variant variant : Variant.values()) {
                for (Markings markings : Markings.values()) {
                    String name = markings == Markings.NONE
                            ? variant.getSerializedName()
                            : variant.getSerializedName() + " " + markings.name();
                    int packedVariant = variant.getId() & 0xFF | markings.getId() << 8 & 0xFF00;
                    addVariantPair(heads, mob, name, tag -> tag.putInt("Variant", packedVariant));
                }
            }
        } else if (mob instanceof Llama) {
            for (Llama.Variant variant : Llama.Variant.values()) {
                addVariantPair(heads, mob, variant.getSerializedName(),
                        tag -> tag.putInt("Variant", variant.getId()));
            }
        } else if (mob instanceof Salmon) {
            for (Salmon.Variant variant : Salmon.Variant.values()) {
                addVariantPair(heads, mob, variant.getSerializedName(),
                        tag -> tag.putString("type", variant.getSerializedName()));
            }
        } else if (mob instanceof TropicalFish) {
            for (TropicalFish.Pattern pattern : TropicalFish.Pattern.values()) {
                for (DyeColor baseColor : DyeColor.values()) {
                    for (DyeColor patternColor : DyeColor.values()) {
                        TropicalFish.Variant variant = new TropicalFish.Variant(pattern, baseColor, patternColor);
                        String name = pattern.getSerializedName() + " " + baseColor.getSerializedName()
                                + " " + patternColor.getSerializedName();
                        addVariantPair(heads, mob, name,
                                tag -> tag.putInt("Variant", variant.getPackedId()));
                    }
                }
            }
        } else if (mob instanceof Fox) {
            for (Fox.Variant variant : Fox.Variant.values()) {
                addVariantPair(heads, mob, variant.getSerializedName(),
                        tag -> tag.putString("Type", variant.getSerializedName()));
            }
        } else if (mob instanceof Panda panda) {
            for (Panda.Gene gene : Panda.Gene.values()) {
                panda.setMainGene(gene);
                panda.setHiddenGene(gene.isRecessive() ? gene : Panda.Gene.NORMAL);
                addVariantPair(heads, panda, gene.getSerializedName(), NO_VARIANT_DATA);
            }
        } else if (mob instanceof Parrot) {
            for (Parrot.Variant variant : Parrot.Variant.values()) {
                addVariantPair(heads, mob, variant.getSerializedName(),
                        tag -> tag.putInt("Variant", variant.getId()));
            }
        } else if (mob instanceof Rabbit) {
            for (Rabbit.Variant variant : Rabbit.Variant.values()) {
                addVariantPair(heads, mob, variant.getSerializedName(),
                        tag -> tag.putInt("RabbitType", variant.id()));
            }
        } else if (mob instanceof Sheep sheep) {
            for (DyeColor color : DyeColor.values()) {
                sheep.setColor(color);
                addVariantPair(heads, sheep, color.getSerializedName(), NO_VARIANT_DATA);
            }
        } else if (mob instanceof Shulker) {
            addVariantPair(heads, mob, "default", tag -> tag.putByte("Color", (byte) 16));
            for (DyeColor color : DyeColor.values()) {
                addVariantPair(heads, mob, color.getSerializedName(),
                        tag -> tag.putByte("Color", (byte) color.getId()));
            }
        } else if (mob instanceof VillagerDataHolder villager) {
            registryAccess.lookupOrThrow(Registries.VILLAGER_TYPE).listElements().forEach(holder -> {
                villager.setVillagerData(villager.getVillagerData().withType(holder));
                addVariantPair(heads, mob, holder.key().identifier().getPath(), NO_VARIANT_DATA);
            });
        } else {
            addVariantPair(heads, mob, "", NO_VARIANT_DATA);
        }
    }

    private static <T> void addRegistryVariants(List<CreativeHeadEntry> heads, Mob mob, Registry<T> registry) {
        registry.listElements().forEach(holder -> {
            Identifier identifier = holder.key().identifier();
            addVariantPair(heads, mob, identifier.getPath(),
                    tag -> tag.putString("variant", identifier.toString()));
        });
    }

    private static void addVariantPair(List<CreativeHeadEntry> heads, Mob mob, String variantName,
                                       Consumer<CompoundTag> variantWriter) {
        String familyName = mob.getType().getDescription().getString();
        mob.setBaby(false);
        heads.add(createCreativeHead(mob, familyName, variantName, false, variantWriter));
        mob.setBaby(true);
        if (mob.isBaby()) {
            heads.add(createCreativeHead(mob, familyName, variantName, true, variantWriter));
        }
        mob.setBaby(false);
    }

    private static CreativeHeadEntry createCreativeHead(Mob mob, String familyName, String variantName,
                                                        boolean baby, Consumer<CompoundTag> variantWriter) {
        Identifier entityType = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        CompoundTag entityData = saveAppearanceData(mob);
        variantWriter.accept(entityData);
        String formattedVariantName = variantName.isEmpty() ? "" : formatVariantName(variantName);
        CustomData data = createData(entityType, entityData, baby, getImitationSound(mob), formattedVariantName);
        return new CreativeHeadEntry(create(data), familyName, formattedVariantName, baby);
    }

    private static String formatVariantName(String variantName) {
        String[] words = variantName.toLowerCase(Locale.ROOT).split("[_\\s-]+");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!formatted.isEmpty()) {
                formatted.append(' ');
            }
            formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return formatted.toString();
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
        return createData(entityType, entityData, baby, noteBlockSound, null);
    }

    private static CustomData createData(Identifier entityType, @Nullable CompoundTag entityData, boolean baby,
                                         @Nullable Identifier noteBlockSound, @Nullable String variantName) {
        CompoundTag tag = new CompoundTag();
        tag.putString(ENTITY_TYPE_KEY, entityType.toString());
        tag.putBoolean(BABY_KEY, baby);
        if (entityData != null && !entityData.isEmpty()) {
            tag.put(ENTITY_DATA_KEY, entityData.copy());
        }
        if (noteBlockSound != null) {
            tag.putString(NOTE_BLOCK_SOUND_KEY, noteBlockSound.toString());
        }
        if (variantName != null && !variantName.isEmpty()) {
            tag.putString(VARIANT_NAME_KEY, variantName);
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
            String variantName = trophyData.variantName();
            if (!variantName.isEmpty()) {
                String nameKey = trophyData.isBaby()
                        ? "item.noblephantasms.trophy_head.baby_variant_named"
                        : "item.noblephantasms.trophy_head.variant_named";
                return Component.translatable(nameKey, entityType.getDescription(), variantName);
            }
            return Component.translatable(trophyData.isBaby()
                    ? "item.noblephantasms.trophy_head.baby_named"
                    : "item.noblephantasms.trophy_head.named", entityType.getDescription());
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

    private static @Nullable Item getVanillaHead(@Nullable CustomData data) {
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        if (tag.getBooleanOr(BABY_KEY, false)) {
            return null;
        }
        Identifier entityTypeId = getEntityType(data);
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

    private record CreativeHeadEntry(ItemStack stack, String familyName, String variantName, boolean baby) {
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

        public String variantName() {
            return customData.copyTag().getStringOr(VARIANT_NAME_KEY, "");
        }
    }

    private static boolean inferBaby(CompoundTag entityData) {
        return entityData.getIntOr("Age", 0) < 0
                || entityData.getBooleanOr("IsBaby", false)
                || entityData.getBooleanOr("Baby", false);
    }
}
