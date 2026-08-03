package net.turtleboi.noblephantasms.tags;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.turtleboi.noblephantasms.NoblePhantasms;

public class ModTags {
    public static class Blocks {


        private static TagKey<Block> createTag(String name) {
            return BlockTags.create(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> NORSE_REPAIRABLE = createTag("norse_repairable");
        public static final TagKey<Item> MESOAMERICAN_REPAIRABLE = createTag("mesoamerican_repairable");
        public static final TagKey<Item> CURIOS_BELT = createCuriosTag("belt");
        public static final TagKey<Item> CURIOS_CHARM = createCuriosTag("charm");
        public static final TagKey<Item> CURIOS_NECKLACE = createCuriosTag("necklace");
        public static final TagKey<Item> CURIOS_RING = createCuriosTag("ring");
        public static final TagKey<Item> CURIOS_TOTEM = createCuriosTag("totem");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, name));
        }

        private static TagKey<Item> createCuriosTag(String name) {
            return ItemTags.create(Identifier.fromNamespaceAndPath("curios", name));
        }
    }

    public static class EntityTypes {
        public static final TagKey<EntityType<?>> BERTILAK_EXECUTION_RESISTANT = createTag("bertilak_execution_resistant");

        private static TagKey<EntityType<?>> createTag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, name));
        }
    }
}
