package net.turtleboi.noblephantasms.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.custom.GungnirProjectile;

public class ModEntities {
    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(NoblePhantasms.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GungnirProjectile>> GUNGNIR =
            ENTITY_TYPES.registerEntityType("gungnir", GungnirProjectile::new, MobCategory.MISC,
                    builder -> builder.noLootTable().sized(0.5F, 0.5F)
                            .clientTrackingRange(4).updateInterval(20));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
