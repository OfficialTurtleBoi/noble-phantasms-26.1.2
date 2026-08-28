package net.turtleboi.noblephantasms.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.custom.ApilolliCloudEntity;
import net.turtleboi.noblephantasms.entity.custom.EyeShardEntity;
import net.turtleboi.noblephantasms.entity.custom.ExcaliburProjectile;
import net.turtleboi.noblephantasms.entity.custom.GungnirProjectile;
import net.turtleboi.noblephantasms.entity.custom.KazagurumaProjectile;
import net.turtleboi.noblephantasms.entity.custom.TecpatlShardEntity;
import net.turtleboi.noblephantasms.entity.custom.WindslashProjectile;
import net.turtleboi.noblephantasms.entity.custom.XiuhcoatlProjectile;
import net.turtleboi.noblephantasms.entity.custom.YasakaniGuardianEntity;

public class ModEntities {
    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(NoblePhantasms.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GungnirProjectile>> GUNGNIR =
            ENTITY_TYPES.registerEntityType("gungnir", GungnirProjectile::new, MobCategory.MISC,
                    builder -> builder.noLootTable().sized(0.5F, 0.5F)
                            .clientTrackingRange(4).updateInterval(20));

    public static final DeferredHolder<EntityType<?>, EntityType<ExcaliburProjectile>> EXCALIBUR_PROJECTILE =
            ENTITY_TYPES.registerEntityType("excalibur_projectile", ExcaliburProjectile::new, MobCategory.MISC,
                    builder -> builder.noLootTable().sized(0.5F, 0.5F)
                            .clientTrackingRange(10).updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<WindslashProjectile>> WINDSLASH =
            ENTITY_TYPES.registerEntityType("windslash", WindslashProjectile::new, MobCategory.MISC,
                    builder -> builder.noLootTable()
                            .sized(WindslashProjectile.INITIAL_COLLISION_WIDTH, WindslashProjectile.INITIAL_COLLISION_HEIGHT)
                            .clientTrackingRange(8).updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<KazagurumaProjectile>> KAZAGURUMA =
            ENTITY_TYPES.registerEntityType("kazaguruma", KazagurumaProjectile::new, MobCategory.MISC,
                    builder -> builder.noLootTable().sized(0.5F, 0.5F)
                            .clientTrackingRange(8).updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<EyeShardEntity>> EYE_SHARD =
            ENTITY_TYPES.registerEntityType("eye_shard", EyeShardEntity::new, MobCategory.MISC,
                    builder -> builder.noLootTable().sized(0.25F, 0.25F)
                            .clientTrackingRange(8).updateInterval(10));

    public static final DeferredHolder<EntityType<?>, EntityType<ApilolliCloudEntity>> APILOLLI_CLOUD =
            ENTITY_TYPES.registerEntityType("apilolli_cloud", ApilolliCloudEntity::new, MobCategory.MISC,
                    builder -> builder.noLootTable().sized(4.0F, 1.0F)
                            .clientTrackingRange(10).updateInterval(10));

    public static final DeferredHolder<EntityType<?>, EntityType<XiuhcoatlProjectile>> XIUHCOATL =
            ENTITY_TYPES.registerEntityType("xiuhcoatl", XiuhcoatlProjectile::new, MobCategory.MISC,
                    builder -> builder.noLootTable().sized(
                                    XiuhcoatlProjectile.HEAD_HITBOX_WIDTH,
                                    XiuhcoatlProjectile.HEAD_HITBOX_HEIGHT)
                            .clientTrackingRange(8).updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<TecpatlShardEntity>> TECPATL_SHARD =
            ENTITY_TYPES.registerEntityType("tecpatl_shard", TecpatlShardEntity::new, MobCategory.MISC,
                    builder -> builder.noLootTable().sized(0.3F, 0.3F)
                            .clientTrackingRange(10).updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<YasakaniGuardianEntity>> YASAKANI_GUARDIAN =
            ENTITY_TYPES.registerEntityType("yasakani_guardian", YasakaniGuardianEntity::new, MobCategory.MISC,
                    builder -> builder.noLootTable().fireImmune().sized(0.6F, 1.8F)
                            .clientTrackingRange(10).updateInterval(1));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
