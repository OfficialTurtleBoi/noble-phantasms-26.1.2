package net.turtleboi.noblephantasms.entity.custom;

import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.YasakaniNoMagatamaItem;
import org.jspecify.annotations.Nullable;

public final class YasakaniGuardianEntity extends PathfinderMob implements ItemSupplier {
    private static final int LIFETIME = 20 * 30;
    private static final EntityDataAccessor<Integer> SPIRIT =
            SynchedEntityData.defineId(YasakaniGuardianEntity.class, EntityDataSerializers.INT);
    private @Nullable UUID ownerId;
    private @Nullable UUID targetId;

    public YasakaniGuardianEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 0;
    }

    public YasakaniGuardianEntity(ServerLevel level, Player owner,
                                   YasakaniNoMagatamaItem.Spirit spirit) {
        this(ModEntities.YASAKANI_GUARDIAN.get(), level);
        ownerId = owner.getUUID();
        entityData.set(SPIRIT, spirit.ordinal());
        setCustomName(Component.literal(spirit.displayName()));
        setCustomNameVisible(false);
        setPos(owner.getX(), owner.getY() + 1.0, owner.getZ());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.FLYING_SPEED, 0.45)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SPIRIT, 0);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        if (!(level() instanceof ServerLevel serverLevel)) {
            spawnClientParticle();
            return;
        }

        Entity ownerEntity = ownerId == null ? null : serverLevel.getEntity(ownerId);
        if (!(ownerEntity instanceof Player owner)
                || !owner.isAlive() || tickCount >= LIFETIME
                || !YasakaniNoMagatamaItem.isCurrentlyEquipped(owner)) {
            discard();
            return;
        }

        YasakaniNoMagatamaItem.registerGuardian(owner, getSpirit(), getUUID());
        LivingEntity target = resolveTarget(serverLevel, owner);
        act(serverLevel, owner, target);
    }

    private LivingEntity resolveTarget(ServerLevel level, Player owner) {
        Entity remembered = targetId == null ? null : level.getEntity(targetId);
        if (remembered instanceof LivingEntity target && target.isAlive()
                && distanceToSqr(target) < 24.0 * 24.0 && owner.canAttack(target)) {
            return target;
        }
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class,
                        owner.getBoundingBox().inflate(16.0), candidate ->
                                candidate.isAlive() && candidate instanceof Enemy
                                        && owner.canAttack(candidate))
                .stream().min(Comparator.comparingDouble(owner::distanceToSqr)).orElse(null);
        targetId = target == null ? null : target.getUUID();
        return target;
    }

    private void act(ServerLevel level, Player owner, @Nullable LivingEntity target) {
        YasakaniNoMagatamaItem.Spirit spirit = getSpirit();
        double orbitAngle = tickCount * 0.06 + spirit.ordinal() * (Math.PI * 2.0 / 5.0);
        Vec3 idlePoint = owner.position().add(Math.cos(orbitAngle) * 1.8,
                1.1 + Math.sin(tickCount * 0.08) * 0.25, Math.sin(orbitAngle) * 1.8);

        if (spirit == YasakaniNoMagatamaItem.Spirit.HOHI && tickCount % 10 == 0) {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(10.0),
                    mob -> mob instanceof Enemy && mob.isAlive())) {
                mob.setTarget(this);
            }
        }

        if (target == null) {
            driftToward(idlePoint, 0.24);
            return;
        }

        if (spirit == YasakaniNoMagatamaItem.Spirit.AMATSUHIKONE) {
            driftToward(idlePoint, 0.28);
            if (tickCount % 24 == 0 && distanceToSqr(target) <= 18.0 * 18.0) {
                float damage = (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
                target.hurtServer(level, level.damageSources().magic(), damage);
                Vec3 from = getEyePosition();
                Vec3 line = target.getEyePosition().subtract(from);
                for (int i = 0; i <= 10; i++) {
                    Vec3 point = from.add(line.scale(i / 10.0));
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0.0);
                }
            }
            return;
        }

        if (spirit == YasakaniNoMagatamaItem.Spirit.IKUTSUHIKONE) {
            Vec3 guardPoint = owner.getEyePosition().add(
                    target.getEyePosition().subtract(owner.getEyePosition()).normalize().scale(1.35));
            driftToward(guardPoint, 0.34);
            return;
        }

        driftToward(target.getBoundingBox().getCenter(), 0.38);
        if (distanceToSqr(target) <= 2.1 * 2.1 && tickCount % 14 == 0) {
            float damage = (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (target.hurtServer(level, level.damageSources().mobAttack(this), damage)
                    && spirit == YasakaniNoMagatamaItem.Spirit.KUMANOKUSUBI) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0), this);
            }
        }
    }

    private void driftToward(Vec3 destination, double speed) {
        Vec3 offset = destination.subtract(position());
        if (offset.lengthSqr() < 0.01) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 velocity = getDeltaMovement().scale(0.55)
                .add(offset.normalize().scale(speed * 0.45));
        setDeltaMovement(velocity);
        setPos(position().add(velocity));
        hurtMarked = true;
    }

    private void spawnClientParticle() {
        if (tickCount % 2 != 0) {
            return;
        }
        int[] colors = {0xF6E27A, 0xEF8354, 0x6ED3CF, 0x86A8E7, 0x8759A8};
        level().addParticle(new DustParticleOptions(colors[getSpirit().ordinal()], 0.75F),
                getX(), getY() + getBbHeight() * 0.5, getZ(), 0.0, 0.01, 0.0);
    }

    public YasakaniNoMagatamaItem.Spirit getSpirit() {
        YasakaniNoMagatamaItem.Spirit[] spirits = YasakaniNoMagatamaItem.Spirit.values();
        return spirits[Math.clamp(entityData.get(SPIRIT), 0, spirits.length - 1)];
    }

    public boolean isOwnedBy(Player player) {
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.YASAKANI_NO_MAGATAMA.get());
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void onRemoval(RemovalReason reason) {
        if (!level().isClientSide() && reason.shouldDestroy() && ownerId != null) {
            YasakaniNoMagatamaItem.guardianDeparted(
                    ownerId, getSpirit(), getUUID(), level().getGameTime());
        }
        super.onRemoval(reason);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String savedOwner = input.getStringOr("Owner", "");
        try {
            ownerId = savedOwner.isEmpty() ? null : UUID.fromString(savedOwner);
        } catch (IllegalArgumentException ignored) {
            ownerId = null;
        }
        entityData.set(SPIRIT, input.getIntOr("Spirit", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (ownerId != null) {
            output.putString("Owner", ownerId.toString());
        }
        output.putInt("Spirit", entityData.get(SPIRIT));
    }
}
