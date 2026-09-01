package net.turtleboi.noblephantasms.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.custom.ApilolliCloudEntity;
import net.turtleboi.noblephantasms.entity.custom.AnubiteEntity;
import net.turtleboi.noblephantasms.entity.custom.EcclesiasticEntity;
import net.turtleboi.noblephantasms.entity.custom.DraugrEntity;
import net.turtleboi.noblephantasms.entity.custom.EyeShardEntity;
import net.turtleboi.noblephantasms.entity.custom.ExcaliburProjectile;
import net.turtleboi.noblephantasms.entity.custom.GungnirProjectile;
import net.turtleboi.noblephantasms.entity.custom.KazagurumaProjectile;
import net.turtleboi.noblephantasms.entity.custom.JaguarMicquiEntity;
import net.turtleboi.noblephantasms.entity.custom.OniEntity;
import net.turtleboi.noblephantasms.entity.custom.PridwenBarrierEntity;
import net.turtleboi.noblephantasms.entity.custom.RelicFragmentEntity;
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

    public static final DeferredHolder<EntityType<?>, EntityType<PridwenBarrierEntity>> PRIDWEN_BARRIER =
            ENTITY_TYPES.registerEntityType("pridwen_barrier", PridwenBarrierEntity::new, MobCategory.MISC,
                    builder -> builder.noLootTable()
                            .sized(0.01F, 0.01F)
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

    public static final DeferredHolder<EntityType<?>, EntityType<RelicFragmentEntity>> RELIC_FRAGMENT =
            ENTITY_TYPES.registerEntityType("relic_fragment", RelicFragmentEntity::new, MobCategory.MISC,
                    builder -> builder.noLootTable().sized(0.3F, 0.3F)
                            .clientTrackingRange(8).updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<ApilolliCloudEntity>> APILOLLI_CLOUD =
            ENTITY_TYPES.registerEntityType("apilolli_cloud", ApilolliCloudEntity::new, MobCategory.MISC,
                    builder -> builder.noLootTable().sized(2.5F, 2.0F)
                            .clientTrackingRange(10).updateInterval(1));

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

    public static final DeferredHolder<EntityType<?>, EntityType<AnubiteEntity>> ANUBITE =
            ENTITY_TYPES.registerEntityType("anubite", AnubiteEntity::new, MobCategory.MONSTER,
                    builder -> builder.noLootTable().sized(0.6F, 1.95F)
                            .clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<EcclesiasticEntity>> ECCLESIASTIC =
            ENTITY_TYPES.registerEntityType("ecclesiastic", EcclesiasticEntity::new, MobCategory.MONSTER,
                    builder -> builder.noLootTable().sized(0.6F, 1.95F)
                            .clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<DraugrEntity>> DRAUGR =
            ENTITY_TYPES.registerEntityType("draugr", DraugrEntity::new, MobCategory.MONSTER,
                    builder -> builder.noLootTable().sized(0.6F, 1.95F)
                            .clientTrackingRange(8).updateInterval(1));

    public static final DeferredHolder<EntityType<?>, EntityType<OniEntity>> ONI =
            ENTITY_TYPES.registerEntityType("oni", OniEntity::new, MobCategory.MONSTER,
                    builder -> builder.noLootTable().sized(1.125F, 2.5F)
                            .clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<JaguarMicquiEntity>> JAGUAR_MICQUI =
            ENTITY_TYPES.registerEntityType("jaguar_micqui", JaguarMicquiEntity::new, MobCategory.MONSTER,
                    builder -> builder.noLootTable().sized(0.7F, 1.95F)
                            .clientTrackingRange(10).updateInterval(1));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
