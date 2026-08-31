package net.turtleboi.noblephantasms.config;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;


public class ModConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", ModConfig::validateItemName);

    public static final ModConfigSpec.DoubleValue EYE_SHARD_DROP_CHANCE = BUILDER
            .defineInRange("eyeOfHorus.shardDropChance", 1.0, 0.0, 1.0);

    public static final ModConfigSpec.IntValue EYE_PIECE_LIFETIME_SECONDS = BUILDER
            .defineInRange("eyeOfHorus.pieceLifetimeSeconds", 300, 1, 3600);

    public static final ModConfigSpec.BooleanValue ALLOW_GUI_ACCESS_WHILE_FROZEN = BUILDER
            .define("frozen.allowGuiAccess", false);

    public static final ModConfigSpec SPEC = BUILDER.build();
    private static boolean validateItemName(final Object object) {
        return object instanceof String itemName && BuiltInRegistries.ITEM.containsKey(Identifier.parse(itemName));
    }
}
