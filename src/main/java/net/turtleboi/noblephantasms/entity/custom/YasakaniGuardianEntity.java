package net.turtleboi.noblephantasms.entity.custom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.AfterimageEffect;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.item.custom.YasakaniNoMagatamaItem;
import org.jspecify.annotations.Nullable;

public final class YasakaniGuardianEntity extends PathfinderMob {
    private static final int LIFETIME = 20 * 30;
    private static final Identifier GREEN_SPEED_ID = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "ikutsuhikone_speed");
    private static final Identifier GREEN_ATTACK_SPEED_ID = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "ikutsuhikone_attack_speed");
    private static final EntityDataAccessor<Integer> SPIRIT =
            SynchedEntityData.defineId(YasakaniGuardianEntity.class, EntityDataSerializers.INT);
    private @Nullable UUID ownerId;
    private @Nullable UUID targetId;
    private final List<UUID> dashTargets = new ArrayList<>();
    private int dashTargetIndex;
    private int dashTargetTicks;
    private int retreatTicks;
    private int combatFlankTicks;
    private int flankSide = 1;
    private int shoutCooldown = 20;
    private @Nullable UUID combatFocusId;
    private @Nullable UUID projectileTargetId;
    private boolean returningToOwner;
    private int guardFacingTicks;
    private float guardFacingYaw;

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
                0.3 + Math.sin(tickCount * 0.08) * 0.18, Math.sin(orbitAngle) * 1.8);
        if (shoutCooldown > 0) {
            shoutCooldown--;
        }

        if (spirit == YasakaniNoMagatamaItem.Spirit.OSHIHOMIMI
                || spirit == YasakaniNoMagatamaItem.Spirit.HOHI
                || spirit == YasakaniNoMagatamaItem.Spirit.KUMANOKUSUBI) {
            if (spirit == YasakaniNoMagatamaItem.Spirit.HOHI) {
                tryShout(level);
            }
            actSkirmisher(level, owner);
            return;
        }

        if (spirit == YasakaniNoMagatamaItem.Spirit.AMATSUHIKONE) {
            actAmatsuhikone(level, owner, target);
            return;
        }

        if (spirit == YasakaniNoMagatamaItem.Spirit.IKUTSUHIKONE) {
            actIkutsuhikone(level, owner);
            return;
        }

        driftToward(idlePoint, 0.14);
    }

    private void actAmatsuhikone(ServerLevel level, Player owner, @Nullable LivingEntity target) {
        driftToward(getCompanionSidePoint(owner, 1.0), 0.16);
        if (tickCount % 40 == 0) {
            LivingEntity healingTarget = findHealingTarget(level, owner);
            if (healingTarget != null) {
                healingTarget.heal(2.5F);
                level.sendParticles(ParticleTypes.HEART,
                        healingTarget.getX(), healingTarget.getY() + healingTarget.getBbHeight() * 0.65,
                        healingTarget.getZ(), 5, 0.3, 0.35, 0.3, 0.02);
                level.playSound(null, blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                        SoundSource.NEUTRAL, 0.7F, 1.35F);
            }
        }

        if (target == null || tickCount % 28 != 0 || distanceToSqr(target) > 18.0 * 18.0) {
            return;
        }

        float damage = (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75F;
        target.hurtServer(level, level.damageSources().magic(), damage);
        spawnJaggedLightning(level, getEyePosition(), target.getEyePosition());
        level.playSound(null, blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.NEUTRAL, 0.7F, 1.1F);
    }

    private void spawnJaggedLightning(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 path = end.subtract(start);
        double distance = path.length();
        if (distance < 0.1) {
            return;
        }

        Vec3 direction = path.normalize();
        Vec3 firstAxis = direction.cross(new Vec3(0.0, 1.0, 0.0));
        if (firstAxis.lengthSqr() < 1.0E-4) {
            firstAxis = direction.cross(new Vec3(1.0, 0.0, 0.0));
        }
        firstAxis = firstAxis.normalize();
        Vec3 secondAxis = direction.cross(firstAxis).normalize();
        int bends = Math.max(6, Mth.ceil(distance * 1.75));
        double amplitude = Math.min(0.8, 0.24 + distance * 0.035);
        Vec3 previous = start;

        for (int bend = 1; bend <= bends; bend++) {
            double progress = bend / (double) bends;
            Vec3 next = end;
            if (bend < bends) {
                double endpointFade = Math.sin(progress * Math.PI);
                double firstOffset = (random.nextDouble() - 0.5) * 2.0 * amplitude * endpointFade;
                double secondOffset = (random.nextDouble() - 0.5) * 1.4 * amplitude * endpointFade;
                next = start.lerp(end, progress)
                        .add(firstAxis.scale(firstOffset))
                        .add(secondAxis.scale(secondOffset));
            }

            spawnLightningSegment(level, previous, next);
            if (bend < bends && random.nextFloat() < 0.22F) {
                Vec3 branchDirection = firstAxis.scale((random.nextDouble() - 0.5) * 2.0)
                        .add(secondAxis.scale((random.nextDouble() - 0.5) * 1.4))
                        .normalize();
                Vec3 branchEnd = next.add(branchDirection.scale(0.35 + random.nextDouble() * 0.55));
                spawnLightningSegment(level, next, branchEnd);
            }
            previous = next;
        }
    }

    private void spawnLightningSegment(ServerLevel level, Vec3 start, Vec3 end) {
        double length = start.distanceTo(end);
        int particles = Math.max(2, Mth.ceil(length * 5.0));
        for (int particle = 0; particle <= particles; particle++) {
            Vec3 point = start.lerp(end, particle / (double) particles);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    point.x, point.y, point.z, 1, 0.015, 0.015, 0.015, 0.0);
        }
    }

    private @Nullable LivingEntity findHealingTarget(ServerLevel level, Player owner) {
        LivingEntity healingTarget = owner.getHealth() < owner.getMaxHealth() ? owner : null;
        float lowestHealthRatio = healingTarget == null
                ? 1.0F : owner.getHealth() / owner.getMaxHealth();
        for (YasakaniGuardianEntity guardian : level.getEntitiesOfClass(
                YasakaniGuardianEntity.class, owner.getBoundingBox().inflate(16.0),
                guardian -> guardian.isAlive() && guardian.isOwnedBy(owner)
                        && guardian.getHealth() < guardian.getMaxHealth())) {
            float healthRatio = guardian.getHealth() / guardian.getMaxHealth();
            if (healthRatio < lowestHealthRatio) {
                lowestHealthRatio = healthRatio;
                healingTarget = guardian;
            }
        }
        return healingTarget;
    }

    private void tryShout(ServerLevel level) {
        if (shoutCooldown > 0) {
            return;
        }
        List<Mob> enemies = level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(14.0),
                mob -> mob instanceof Enemy && mob.isAlive());
        if (enemies.isEmpty()) {
            return;
        }
        for (Mob enemy : enemies) {
            enemy.setTarget(this);
            enemy.knockback(1.35, getX() - enemy.getX(), getZ() - enemy.getZ());
        }
        shoutCooldown = 140;
        level.playSound(null, blockPosition(), SoundEvents.POLAR_BEAR_WARNING,
                SoundSource.NEUTRAL, 1.5F, 0.7F);
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() * 0.75,
                getZ(), 18, 1.0, 0.45, 1.0, 0.05);
    }

    private double getAttackReach(LivingEntity target, double extraReach) {
        return getBbWidth() * 0.5 + target.getBbWidth() * 0.5 + extraReach;
    }

    private boolean isWithinAttackReach(LivingEntity target, double extraReach) {
        double reach = getAttackReach(target, extraReach);
        double horizontalX = getX() - target.getX();
        double horizontalZ = getZ() - target.getZ();
        double verticalGap = Math.max(0.0, Math.max(
                target.getBoundingBox().minY - getBoundingBox().maxY,
                getBoundingBox().minY - target.getBoundingBox().maxY));
        return horizontalX * horizontalX + horizontalZ * horizontalZ <= reach * reach
                && verticalGap <= 1.5;
    }

    private void actSkirmisher(ServerLevel level, Player owner) {
        if (dashTargetIndex < dashTargets.size()) {
            Entity entity = level.getEntity(dashTargets.get(dashTargetIndex));
            if (!(entity instanceof LivingEntity target) || !target.isAlive()
                    || !owner.canAttack(target) || dashTargetTicks >= 14) {
                advanceDashTarget();
                return;
            }

            dashTargetTicks++;
            Vec3 destination = target.getBoundingBox().getCenter();
            if (isWithinAttackReach(target, 1.9)) {
                float damage = (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
                swing(InteractionHand.MAIN_HAND, true);
                combatFocusId = target.getUUID();
                if (target.hurtServer(level, level.damageSources().mobAttack(this), damage)
                        && getSpirit() == YasakaniNoMagatamaItem.Spirit.KUMANOKUSUBI) {
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0), this);
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1), this);
                }
                advanceDashTarget();
                return;
            }

            flyDirectly(destination, 1.35);
            return;
        }

        if (combatFlankTicks > 0) {
            LivingEntity flankTarget = findSkirmishTarget(level, owner);
            if (flankTarget == null) {
                combatFlankTicks = 0;
                retreatTicks = 16;
            } else {
                Vec3 combatFlankPoint = getCombatFlankPoint(owner, flankTarget);
                if (getSpirit() == YasakaniNoMagatamaItem.Spirit.HOHI
                        && flankTarget instanceof Mob mob) {
                    mob.setTarget(this);
                }
                double horizontalX = getX() - flankTarget.getX();
                double horizontalZ = getZ() - flankTarget.getZ();
                double targetDistanceSquared = horizontalX * horizontalX + horizontalZ * horizontalZ;
                if (targetDistanceSquared >= 4.5 * 4.5) {
                    combatFlankTicks--;
                }
                if (distanceToSqr(combatFlankPoint) > 1.0) {
                    flyDirectly(combatFlankPoint, targetDistanceSquared < 4.5 * 4.5 ? 0.72 : 0.5);
                } else {
                    setDeltaMovement(getDeltaMovement().scale(0.2));
                    faceVelocity(flankTarget.getBoundingBox().getCenter().subtract(position()));
                }
                return;
            }
        }

        if (prepareDashTargets(level, owner)) {
            retreatTicks = 0;
            AfterimageEffect.activate(this, 16);
            dashTargetIndex = 0;
            dashTargetTicks = 0;
            return;
        }

        if (getSpirit() == YasakaniNoMagatamaItem.Spirit.KUMANOKUSUBI
                && findSkirmishTarget(level, owner) != null) {
            combatFlankTicks = 10;
            return;
        }

        Vec3 flankPoint = getFlankPoint(owner);
        if (retreatTicks > 0) {
            retreatTicks--;
            if (distanceToSqr(flankPoint) > 1.2 * 1.2) {
                flyDirectly(flankPoint, 0.9);
            } else {
                retreatTicks = 0;
                setDeltaMovement(Vec3.ZERO);
            }
            return;
        }

        driftToward(flankPoint, 0.16);
    }

    private boolean prepareDashTargets(ServerLevel level, Player owner) {
        dashTargets.clear();
        List<LivingEntity> candidates = getSkirmishCandidates(level, owner);
        YasakaniNoMagatamaItem.Spirit spirit = getSpirit();
        if (spirit == YasakaniNoMagatamaItem.Spirit.OSHIHOMIMI) {
            candidates.stream()
                    .sorted(Comparator.comparingDouble(this::distanceToSqr))
                    .limit(4)
                    .map(Entity::getUUID)
                    .forEach(dashTargets::add);
        } else if (spirit == YasakaniNoMagatamaItem.Spirit.HOHI) {
            candidates.stream()
                    .max(Comparator.comparingDouble(this::getDangerScore))
                    .map(Entity::getUUID)
                    .ifPresent(dashTargets::add);
        } else {
            candidates.stream()
                    .filter(candidate -> !candidate.hasEffect(MobEffects.WITHER)
                            && !candidate.hasEffect(MobEffects.SLOWNESS))
                    .min(Comparator.comparingDouble(this::distanceToSqr))
                    .map(Entity::getUUID)
                    .ifPresent(dashTargets::add);
        }
        if (!dashTargets.isEmpty()) {
            combatFocusId = dashTargets.get(0);
        }
        return !dashTargets.isEmpty();
    }

    private List<LivingEntity> getSkirmishCandidates(ServerLevel level, Player owner) {
        return level.getEntitiesOfClass(LivingEntity.class,
                owner.getBoundingBox().inflate(20.0), candidate ->
                        candidate.isAlive() && candidate instanceof Enemy
                                && owner.canAttack(candidate) && hasLineOfSight(candidate));
    }

    private double getDangerScore(LivingEntity target) {
        AttributeInstance attackDamage = target.getAttribute(Attributes.ATTACK_DAMAGE);
        double damage = attackDamage == null ? 0.0 : attackDamage.getValue();
        return target.getMaxHealth() + target.getHealth() + damage * 6.0;
    }

    private void advanceDashTarget() {
        dashTargetIndex++;
        dashTargetTicks = 0;
        if (dashTargetIndex >= dashTargets.size()) {
            dashTargets.clear();
            dashTargetIndex = 0;
            combatFlankTicks = 28;
            flankSide = -flankSide;
            AfterimageEffect.activate(this, 14);
        } else {
            AfterimageEffect.activate(this, 16);
        }
    }

    private @Nullable LivingEntity findSkirmishTarget(ServerLevel level, Player owner) {
        Entity remembered = combatFocusId == null ? null : level.getEntity(combatFocusId);
        if (remembered instanceof LivingEntity target && target.isAlive()
                && owner.distanceToSqr(target) <= 24.0 * 24.0
                && owner.canAttack(target) && hasLineOfSight(target)) {
            return target;
        }
        List<LivingEntity> candidates = getSkirmishCandidates(level, owner);
        LivingEntity target = getSpirit() == YasakaniNoMagatamaItem.Spirit.HOHI
                ? candidates.stream().max(Comparator.comparingDouble(this::getDangerScore)).orElse(null)
                : candidates.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        combatFocusId = target == null ? null : target.getUUID();
        return target;
    }

    private Vec3 getCombatFlankPoint(Player owner, LivingEntity target) {
        Vec3 towardOwner = owner.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
        if (towardOwner.lengthSqr() < 1.0E-5) {
            towardOwner = owner.getLookAngle().multiply(-1.0, 0.0, -1.0);
        }
        if (towardOwner.lengthSqr() < 1.0E-5) {
            towardOwner = new Vec3(0.0, 0.0, 1.0);
        } else {
            towardOwner = towardOwner.normalize();
        }
        Vec3 side = new Vec3(-towardOwner.z, 0.0, towardOwner.x).scale(flankSide * 4.8);
        return target.position().add(side).add(towardOwner.scale(1.4)).add(0.0, 0.35, 0.0);
    }

    private Vec3 getFlankPoint(Player owner) {
        Vec3 forward = owner.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() < 1.0E-5) {
            forward = new Vec3(0.0, 0.0, 1.0);
        } else {
            forward = forward.normalize();
        }
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x).scale(flankSide * 1.8);
        return owner.position().add(side.x, 0.3, side.z).subtract(forward.scale(0.75));
    }

    private Vec3 getCompanionSidePoint(Player owner, double sideScale) {
        Vec3 forward = owner.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() < 1.0E-5) {
            forward = new Vec3(0.0, 0.0, 1.0);
        } else {
            forward = forward.normalize();
        }
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x).scale(sideScale * 1.55);
        return owner.position().add(side.x, 0.3, side.z);
    }

    private void actIkutsuhikone(ServerLevel level, Player owner) {
        if (guardFacingTicks > 0) {
            guardFacingTicks--;
        }
        Projectile projectile = resolveIncomingProjectile(level, owner);
        if (projectile != null) {
            updateGreenBuffs(owner, false);
            returningToOwner = false;
            Vec3 destination = projectile.getBoundingBox().getCenter();
            double interceptReach = 1.35 + projectile.getDeltaMovement().length();
            if (distanceToSqr(destination) <= interceptReach * interceptReach) {
                blockProjectile(level, owner, projectile);
                return;
            }

            flyDirectly(destination, 1.5);
            rememberGuardFacing(projectile.getDeltaMovement().scale(-1.0));
            return;
        }

        Vec3 sidePoint = getCompanionSidePoint(owner, -1.0);
        if (returningToOwner && distanceToSqr(sidePoint) > 1.0) {
            updateGreenBuffs(owner, false);
            flyDirectly(sidePoint, 0.75);
            faceRememberedGuardDirection();
            return;
        }

        returningToOwner = false;
        boolean stationed = distanceToSqr(sidePoint) <= 1.5 * 1.5;
        if (stationed) {
            setDeltaMovement(getDeltaMovement().scale(0.2));
            if (guardFacingTicks > 0) {
                faceRememberedGuardDirection();
            } else {
                faceVelocity(owner.getLookAngle());
            }
        } else {
            driftToward(sidePoint, 0.16);
        }
        updateGreenBuffs(owner, stationed);
    }

    public void blockProjectile(ServerLevel level, Player owner, Projectile projectile) {
        Vec3 incomingVelocity = projectile.getDeltaMovement();
        Entity shooter = projectile.getOwner();
        boolean reflected = shooter instanceof LivingEntity livingShooter && livingShooter.isAlive()
                && random.nextFloat() < 0.35F;
        if (reflected) {
            Vec3 reflectedDirection = ((LivingEntity) shooter).getEyePosition()
                    .subtract(projectile.getBoundingBox().getCenter());
            if (reflectedDirection.lengthSqr() > 1.0E-5) {
                double speed = Math.max(incomingVelocity.length(), 0.8);
                projectile.setOwner(owner);
                projectile.setDeltaMovement(reflectedDirection.normalize().scale(speed));
            } else {
                projectile.discard();
            }
        } else {
            projectile.discard();
        }

        projectileTargetId = null;
        returningToOwner = true;
        rememberGuardFacing(incomingVelocity.scale(-1.0));
        setDeltaMovement(Vec3.ZERO);
        hurtServer(level, level.damageSources().magic(), 3.0F);
        AfterimageEffect.activate(this, 16);
        level.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK.value(),
                SoundSource.PLAYERS, 1.0F, reflected ? 1.65F : 1.35F);
    }

    private @Nullable Projectile resolveIncomingProjectile(ServerLevel level, Player owner) {
        Entity remembered = projectileTargetId == null ? null : level.getEntity(projectileTargetId);
        if (remembered instanceof Projectile projectile && projectile.isAlive()
                && distanceToSqr(projectile) <= 16.0 * 16.0
                && threatensOwner(projectile, owner)) {
            return projectile;
        }

        Projectile projectile = level.getEntitiesOfClass(Projectile.class,
                        owner.getBoundingBox().inflate(12.0), candidate ->
                                candidate.isAlive() && isHostileProjectile(candidate, owner)
                                        && threatensOwner(candidate, owner))
                .stream().min(Comparator.comparingDouble(owner::distanceToSqr)).orElse(null);
        projectileTargetId = projectile == null ? null : projectile.getUUID();
        if (projectile != null) {
            AfterimageEffect.activate(this, 14);
        }
        return projectile;
    }

    private boolean isHostileProjectile(Projectile projectile, Player owner) {
        Entity projectileOwner = projectile.getOwner();
        return projectileOwner != owner
                && (!(projectileOwner instanceof LivingEntity livingOwner)
                || owner.canAttack(livingOwner));
    }

    private boolean threatensOwner(Projectile projectile, Player owner) {
        Vec3 velocity = projectile.getDeltaMovement();
        if (velocity.lengthSqr() < 1.0E-5) {
            return false;
        }
        Vec3 projectilePosition = projectile.getBoundingBox().getCenter();
        Vec3 toOwner = owner.getBoundingBox().getCenter().subtract(projectilePosition);
        double time = velocity.dot(toOwner) / velocity.lengthSqr();
        if (time < 0.0 || time > 20.0) {
            return false;
        }
        Vec3 closestPoint = projectilePosition.add(velocity.scale(time));
        return closestPoint.distanceToSqr(owner.getBoundingBox().getCenter()) <= 1.6 * 1.6;
    }

    private void rememberGuardFacing(Vec3 direction) {
        if (direction.horizontalDistanceSqr() <= 1.0E-5) {
            return;
        }
        guardFacingYaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
        guardFacingTicks = 12;
        faceRememberedGuardDirection();
    }

    private void faceRememberedGuardDirection() {
        if (guardFacingTicks <= 0) {
            return;
        }
        setYRot(guardFacingYaw);
        setYBodyRot(guardFacingYaw);
        setYHeadRot(guardFacingYaw);
    }

    private void updateGreenBuffs(Player owner, boolean active) {
        updateGreenBuff(owner.getAttribute(Attributes.MOVEMENT_SPEED), GREEN_SPEED_ID, 0.08, active);
        updateGreenBuff(owner.getAttribute(Attributes.ATTACK_SPEED), GREEN_ATTACK_SPEED_ID, 0.10, active);
    }

    private void updateGreenBuff(@Nullable AttributeInstance attribute, Identifier id,
                                 double amount, boolean active) {
        if (attribute == null) {
            return;
        }
        if (active) {
            attribute.addOrUpdateTransientModifier(new AttributeModifier(
                    id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            attribute.removeModifier(id);
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
        faceVelocity(velocity);
        hurtMarked = true;
    }

    private void flyDirectly(Vec3 destination, double speed) {
        Vec3 offset = destination.subtract(position());
        if (offset.lengthSqr() < 1.0E-5) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 velocity = offset.normalize().scale(Math.min(speed, offset.length()));
        setDeltaMovement(velocity);
        faceVelocity(velocity);
        hurtMarked = true;
    }

    private void faceVelocity(Vec3 velocity) {
        if (velocity.horizontalDistanceSqr() > 1.0E-5) {
            float targetYaw = (float) (Mth.atan2(velocity.z, velocity.x) * Mth.RAD_TO_DEG) - 90.0F;
            float yaw = Mth.rotLerp(0.65F, getYRot(), targetYaw);
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        }
    }

    private void spawnClientParticle() {
        if (tickCount % 2 != 0) {
            return;
        }
        int[] colors = {0x4E8FF1, 0xED2146, 0xE8C92E, 0x42D654, 0xDFD8B3};
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
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float adjustedAmount = getSpirit() == YasakaniNoMagatamaItem.Spirit.IKUTSUHIKONE
                ? amount * 0.5F : amount;
        return super.hurtServer(level, source, adjustedAmount);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void onRemoval(RemovalReason reason) {
        if (!level().isClientSide() && ownerId != null) {
            if (getSpirit() == YasakaniNoMagatamaItem.Spirit.IKUTSUHIKONE
                    && level() instanceof ServerLevel serverLevel
                    && serverLevel.getEntity(ownerId) instanceof Player owner) {
                updateGreenBuffs(owner, false);
            }
            if (reason.shouldDestroy()) {
                YasakaniNoMagatamaItem.guardianDeparted(
                        ownerId, getSpirit(), getUUID(), level().getGameTime());
            }
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
