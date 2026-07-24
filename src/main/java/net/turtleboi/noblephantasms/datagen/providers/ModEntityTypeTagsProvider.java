package net.turtleboi.noblephantasms.datagen.providers;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.world.entity.EntityType;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.tags.ModTags;

public class ModEntityTypeTagsProvider extends EntityTypeTagsProvider {
    public ModEntityTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, NoblePhantasms.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(ModTags.EntityTypes.BERTILAK_EXECUTION_RESISTANT)
                .add(EntityType.ENDER_DRAGON, EntityType.WITHER, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN);
        tag(ModTags.EntityTypes.BERTILAK_TROPHY_EXCLUDED)
                .add(EntityType.BEE, EntityType.BLAZE, EntityType.COD, EntityType.ENDERMITE,
                        EntityType.GHAST, EntityType.HAPPY_GHAST, EntityType.GLOW_SQUID, EntityType.MAGMA_CUBE,
                        EntityType.PHANTOM, EntityType.PUFFERFISH, EntityType.SALMON, EntityType.SHULKER,
                        EntityType.SILVERFISH, EntityType.SLIME, EntityType.SQUID, EntityType.STRIDER,
                        EntityType.TADPOLE, EntityType.TROPICAL_FISH);
    }
}
