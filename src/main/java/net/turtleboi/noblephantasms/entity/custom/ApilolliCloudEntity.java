package net.turtleboi.noblephantasms.entity.custom;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.particle.ModParticles;
import org.jspecify.annotations.Nullable;

public final class ApilolliCloudEntity extends Entity {
    private static final int LIFETIME = 20 * 90;
    private static final int EFFECT_RADIUS = 6;
    private static final double CLOUD_SCALE = 2.0;
    private static final double[][] MIDDLE_LOBES = {
            {-0.22, 0.12},
            {0.18, -0.20},
            {0.24, 0.17}
    };
    private static final double[][] TOP_LOBES = {
            {-0.13, -0.07},
            {0.12, 0.10}
    };
    private static final EntityDataAccessor<Integer> OWNER =
            SynchedEntityData.defineId(ApilolliCloudEntity.class, EntityDataSerializers.INT);
    private @Nullable UUID ownerId;

    public ApilolliCloudEntity(EntityType<? extends ApilolliCloudEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public ApilolliCloudEntity(ServerLevel level, Player owner) {
        this(ModEntities.APILOLLI_CLOUD.get(), level);
        ownerId = owner.getUUID();
        entityData.set(OWNER, owner.getId());
        setPos(owner.getX(), owner.getY() + 3.5, owner.getZ());
    }

    public static boolean hasActiveCloud(Player player) {
        return !player.level().getEntitiesOfClass(ApilolliCloudEntity.class,
                player.getBoundingBox().inflate(128.0), cloud -> cloud.isOwnedBy(player)).isEmpty();
    }

    public boolean isOwnedBy(Player player) {
        return ownerId != null && ownerId.equals(player.getUUID())
                || entityData.get(OWNER) == player.getId();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            int repetitions = tickCount < 5 ? 2 : 1;
            for (int repetition = 0; repetition < repetitions; repetition++) {
                for (int i = 0; i < 4; i++) {
                    spawnLowerCloudParticle();
                }
                for (int i = 0; i < 2; i++) {
                    spawnLobeParticle(MIDDLE_LOBES, 0.42, 0.10, 0.08, 1.65);
                }
                spawnLobeParticle(TOP_LOBES, 0.68, 0.06, 0.05, 1.35);
            }
            for (int i = 0; i < 2; i++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double radius = Math.sqrt(random.nextDouble()) * 1.5;
                level().addParticle(ParticleTypes.FALLING_WATER,
                        getX() + Math.cos(angle) * radius,
                        getY() + 0.05,
                        getZ() + Math.sin(angle) * radius,
                        0.0, -0.08, 0.0);
            }
            return;
        }

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Entity ownerEntity = ownerId == null ? null : serverLevel.getEntity(ownerId);
        if (!(ownerEntity instanceof Player owner)
                || !owner.isAlive() || tickCount >= LIFETIME) {
            discard();
            return;
        }

        setPos(owner.getX(), owner.getY() + 3.5, owner.getZ());
        if (tickCount % 20 == 0) {
            nourish(serverLevel, owner);
        }
    }

    private void spawnLowerCloudParticle() {
        double angle = random.nextDouble() * Mth.TWO_PI;
        double radius = Math.sqrt(random.nextDouble()) * 0.39 * CLOUD_SCALE;
        level().addParticle(ModParticles.APILOLLI_CLOUD.get(),
                getX() + Math.cos(angle) * radius,
                getY() + (0.18 + random.nextDouble() * 0.16) * CLOUD_SCALE,
                getZ() + Math.sin(angle) * radius,
                getId(), CLOUD_SCALE, 0.0);
    }

    private void spawnLobeParticle(double[][] lobes, double yOffset, double yVariation,
                                   double spread, double particleScale) {
        double[] lobe = lobes[random.nextInt(lobes.length)];
        level().addParticle(ModParticles.APILOLLI_CLOUD.get(),
                getX() + (lobe[0] + random.nextGaussian() * spread) * CLOUD_SCALE,
                getY() + (yOffset + random.nextDouble() * yVariation) * CLOUD_SCALE,
                getZ() + (lobe[1] + random.nextGaussian() * spread) * CLOUD_SCALE,
                getId(), particleScale, 0.0);
    }

    private void nourish(ServerLevel level, Player owner) {
        owner.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 45, 0, true, false));
        owner.addEffect(new MobEffectInstance(MobEffects.LUCK, 45, 0, true, false));

        BlockPos center = owner.blockPosition();
        for (BlockPos cursor : BlockPos.betweenClosed(
                center.offset(-EFFECT_RADIUS, -2, -EFFECT_RADIUS),
                center.offset(EFFECT_RADIUS, 2, EFFECT_RADIUS))) {
            if (cursor.distSqr(center) > EFFECT_RADIUS * EFFECT_RADIUS) {
                continue;
            }
            BlockState state = level.getBlockState(cursor);
            if (state.is(BlockTags.FIRE)) {
                level.removeBlock(cursor, false);
                continue;
            }
            FluidState fluidState = state.getFluidState();
            if (fluidState.is(FluidTags.LAVA)) {
                level.setBlock(cursor, fluidState.isSource()
                        ? Blocks.OBSIDIAN.defaultBlockState()
                        : Blocks.COBBLESTONE.defaultBlockState(), 3);
                continue;
            }
            if (state.getBlock() instanceof BonemealableBlock grower
                    && grower.getType() == BonemealableBlock.Type.GROWER
                    && random.nextFloat() < 0.055F) {
                grower.performBonemeal(level, level.getRandom(), cursor, state);
            }
            if ((state.is(BlockTags.CAMPFIRES)
                    || state.is(BlockTags.CANDLES)
                    || state.is(BlockTags.CANDLE_CAKES))
                    && state.hasProperty(BlockStateProperties.LIT)
                    && state.getValue(BlockStateProperties.LIT)) {
                level.setBlock(cursor, state.setValue(BlockStateProperties.LIT, false), 3);
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, -1);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        String savedOwner = input.getStringOr("Owner", "");
        try {
            ownerId = savedOwner.isEmpty() ? null : UUID.fromString(savedOwner);
        } catch (IllegalArgumentException ignored) {
            ownerId = null;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (ownerId != null) {
            output.putString("Owner", ownerId.toString());
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
