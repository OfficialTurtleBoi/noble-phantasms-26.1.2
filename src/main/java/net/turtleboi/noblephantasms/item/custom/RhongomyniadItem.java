package net.turtleboi.noblephantasms.item.custom;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.item.ModRarities;

public class RhongomyniadItem extends Item {
    private static final float ATTACK_DURATION = 1.15F;
    private static final float DAMAGE_PHASE_DURATION = 3600.0F;
    private static final int JOUST_LOWER_TICKS = 8;
    public static final int FULL_CHARGE_TICKS = 60;
    public static final float MIN_LAUNCH_SPEED = 1.5F;
    public static final float MAX_LAUNCH_SPEED = 6.0F;
    public static final float MIN_ENGAGED_FORWARD_SPEED = 0.2F;
    public static final float MAX_SPIN_DEGREES_PER_TICK = 60.0F;
    public static final int DISENGAGE_RECOVERY_TICKS = 8;
    public static final int POST_LAUNCH_COOLDOWN_TICKS = 100;
    private static final Map<Player, ForcedJoust> FORCED_JOUSTS =
            Collections.synchronizedMap(new IdentityHashMap<>());

    public RhongomyniadItem(Properties properties) {
        super(properties
                .spear(ToolMaterial.NETHERITE, ATTACK_DURATION, 1.2F, JOUST_LOWER_TICKS / 20.0F, 2.5F,
                        9.0F, 5.5F, 5.1F, DAMAGE_PHASE_DURATION, 4.6F)
                .component(DataComponents.ATTACK_RANGE, new AttackRange(2.0F, 5.5F, 2.0F, 7.5F, 0.125F, 0.5F))
                .attributes(createAttributes())
                .rarity(ModRarities.LEGENDARY.getValue())
                .fireResistant());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(itemStack)) {
            return InteractionResult.FAIL;
        }
        return super.use(level, player, hand);
    }

    @Override
    public boolean releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        if (FORCED_JOUSTS.containsKey(player)) {
            return false;
        }

        int timeHeld = getUseDuration(itemStack, entity) - remainingTime;
        int chargedTicks = Mth.clamp(Mth.floor(getChargeTicks(itemStack, timeHeld)), 0, FULL_CHARGE_TICKS);
        if (chargedTicks <= 0) {
            return false;
        }

        float charge = chargedTicks / (float) FULL_CHARGE_TICKS;
        float launchSpeed = Mth.lerp(charge, MIN_LAUNCH_SPEED, MAX_LAUNCH_SPEED);
        Vec3 launchDirection = player.getLookAngle().normalize();
        Vec3 launchMomentum = launchDirection.scale(launchSpeed);
        player.setDeltaMovement(launchMomentum);
        if (!level.isClientSide()) {
            player.hurtMarked = true;
        }
        player.resetFallDistance();
        FORCED_JOUSTS.put(player,
                new ForcedJoust(level.getGameTime() + chargedTicks, launchDirection, player.getEyePosition()));
        return false;
    }

    @Override
    public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (!(owner instanceof Player player)) {
            return;
        }

        ForcedJoust forcedJoust = FORCED_JOUSTS.get(player);
        if (forcedJoust == null) {
            return;
        }

        if (!player.isAlive()
                || !player.isUsingItem()
                || !(player.getUseItem().getItem() instanceof RhongomyniadItem)) {
            FORCED_JOUSTS.remove(player);
            return;
        }

        if (!shouldKeepJousting(player)) {
            player.stopUsingItem();
            return;
        }

        if (!forcedJoust.isRecovering()) {
            damageEntitiesAlongLaunch(level, player, itemStack, forcedJoust);
        }
    }

    public static boolean shouldKeepJousting(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        ForcedJoust forcedJoust = FORCED_JOUSTS.get(player);
        if (forcedJoust == null) {
            return false;
        }

        boolean valid = player.isAlive()
                && player.getUseItem().getItem() instanceof RhongomyniadItem
                && player.getItemInHand(player.getUsedItemHand()).getItem() instanceof RhongomyniadItem;
        if (!valid) {
            return false;
        }

        long gameTime = player.level().getGameTime();
        if (!forcedJoust.isRecovering()
                && (gameTime >= forcedJoust.endGameTime()
                || player.getDeltaMovement().dot(forcedJoust.launchDirection()) < MIN_ENGAGED_FORWARD_SPEED)) {
            forcedJoust.startRecovery(gameTime, player.getTicksUsingItem());
        }

        return !forcedJoust.isRecovering()
                || gameTime - forcedJoust.recoveryStartGameTime() < DISENGAGE_RECOVERY_TICKS;
    }

    public static void clearForcedJoust(LivingEntity entity) {
        if (entity instanceof Player player) {
            ForcedJoust forcedJoust = FORCED_JOUSTS.remove(player);
            if (forcedJoust != null && !player.level().isClientSide()) {
                ItemStack cooldownStack = player.getMainHandItem().getItem() instanceof RhongomyniadItem
                        ? player.getMainHandItem()
                        : player.getOffhandItem();
                if (cooldownStack.getItem() instanceof RhongomyniadItem) {
                    player.getCooldowns().addCooldown(cooldownStack, POST_LAUNCH_COOLDOWN_TICKS);
                }
            }
        }
    }

    public static float getRecoveryProgress(LivingEntity entity, float partialTick) {
        if (!(entity instanceof Player player)) {
            return 0.0F;
        }

        ForcedJoust forcedJoust = FORCED_JOUSTS.get(player);
        if (forcedJoust == null || !forcedJoust.isRecovering()) {
            return 0.0F;
        }

        return Mth.clamp((player.level().getGameTime() - forcedJoust.recoveryStartGameTime() + partialTick)
                / DISENGAGE_RECOVERY_TICKS, 0.0F, 1.0F);
    }

    public static boolean isRecovering(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        ForcedJoust forcedJoust = FORCED_JOUSTS.get(player);
        return forcedJoust != null && forcedJoust.isRecovering();
    }

    public static float getSpinTime(LivingEntity entity, float currentUseTime) {
        if (!(entity instanceof Player player)) {
            return currentUseTime;
        }

        ForcedJoust forcedJoust = FORCED_JOUSTS.get(player);
        return forcedJoust != null && forcedJoust.isRecovering()
                ? forcedJoust.recoveryStartUseTime()
                : currentUseTime;
    }

    public static float getChargeTicks(ItemStack itemStack, float timeHeld) {
        return Math.max(timeHeld - getChargeStartTick(itemStack), 0.0F);
    }

    public static int getJoustLowerTicks() {
        return JOUST_LOWER_TICKS;
    }

    public static int getChargeStartTick(ItemStack itemStack) {
        KineticWeapon kineticWeapon = itemStack.get(DataComponents.KINETIC_WEAPON);
        if (kineticWeapon == null) {
            return Integer.MAX_VALUE;
        }

        int finishRaisingTick = kineticWeapon.delayTicks();
        return kineticWeapon.dismountConditions()
                .map(KineticWeapon.Condition::maxDurationTicks)
                .orElse(0) + finishRaisingTick;
    }

    public static float getChargeProgress(ItemStack itemStack, float timeHeld) {
        return Mth.clamp(getChargeTicks(itemStack, timeHeld) / FULL_CHARGE_TICKS, 0.0F, 1.0F);
    }

    public static float getSpinDegrees(ItemStack itemStack, float timeHeld) {
        float spinTicks = getChargeTicks(itemStack, timeHeld);
        float acceleratingTicks = Math.min(spinTicks, FULL_CHARGE_TICKS);
        float acceleration = MAX_SPIN_DEGREES_PER_TICK / FULL_CHARGE_TICKS;
        float degrees = 0.5F * acceleration * acceleratingTicks * acceleratingTicks;
        if (spinTicks > FULL_CHARGE_TICKS) {
            degrees += MAX_SPIN_DEGREES_PER_TICK * (spinTicks - FULL_CHARGE_TICKS);
        }
        return degrees;
    }

    private static void damageEntitiesAlongLaunch(ServerLevel level, Player player, ItemStack itemStack,
                                                  ForcedJoust forcedJoust) {
        Vec3 start = forcedJoust.previousEyePosition();
        Vec3 currentEyePosition = player.getEyePosition();
        float reach = player.getAttackRangeWith(itemStack).effectiveMaxRange(player);
        Vec3 end = currentEyePosition.add(forcedJoust.launchDirection().scale(reach));
        HitResult blockHit = level.clip(
                new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            end = blockHit.getLocation();
        }

        AABB sweptArea = new AABB(start, end).inflate(0.75);
        KineticWeapon kineticWeapon = itemStack.get(DataComponents.KINETIC_WEAPON);
        if (kineticWeapon == null) {
            forcedJoust.setPreviousEyePosition(currentEyePosition);
            return;
        }

        double attackerSpeed = forcedJoust.launchDirection().dot(KineticWeapon.getMotion(player));
        double baseDamage = player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        boolean affected = false;
        for (Entity target : level.getEntities(player, sweptArea,
                entity -> PiercingWeapon.canHitEntity(player, entity))) {
            if (forcedJoust.hasHit(target.getUUID())) {
                continue;
            }

            AABB targetBounds = target.getBoundingBox().inflate(0.5);
            if (!targetBounds.contains(start) && targetBounds.clip(start, end).isEmpty()) {
                continue;
            }

            forcedJoust.rememberHit(target.getUUID());
            if (player.wasRecentlyStabbed(target, kineticWeapon.contactCooldownTicks())) {
                continue;
            }

            player.rememberStabbedEntity(target);
            double targetSpeed = forcedJoust.launchDirection().dot(KineticWeapon.getMotion(target));
            double relativeSpeed = Math.max(0.0, attackerSpeed - targetSpeed);
            float damage = (float) baseDamage + Mth.floor(relativeSpeed * kineticWeapon.damageMultiplier());
            affected |= player.stabAttack(
                    player.getUsedItemHand().asEquipmentSlot(), target, damage, true, true, false);
        }

        if (affected) {
            level.broadcastEntityEvent(player, (byte) 2);
        }
        forcedJoust.setPreviousEyePosition(currentEyePosition);
    }

    private static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 8.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, 1.0F / ATTACK_DURATION - 4.0F,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    private static final class ForcedJoust {
        private final long endGameTime;
        private final Vec3 launchDirection;
        private final Set<UUID> hitEntities = new HashSet<>();
        private Vec3 previousEyePosition;
        private long recoveryStartGameTime = Long.MIN_VALUE;
        private float recoveryStartUseTime;

        private ForcedJoust(long endGameTime, Vec3 launchDirection, Vec3 previousEyePosition) {
            this.endGameTime = endGameTime;
            this.launchDirection = launchDirection;
            this.previousEyePosition = previousEyePosition;
        }

        private long endGameTime() {
            return endGameTime;
        }

        private Vec3 launchDirection() {
            return launchDirection;
        }

        private boolean isRecovering() {
            return recoveryStartGameTime != Long.MIN_VALUE;
        }

        private void startRecovery(long gameTime, float useTime) {
            recoveryStartGameTime = gameTime;
            recoveryStartUseTime = useTime;
        }

        private long recoveryStartGameTime() {
            return recoveryStartGameTime;
        }

        private float recoveryStartUseTime() {
            return recoveryStartUseTime;
        }

        private Vec3 previousEyePosition() {
            return previousEyePosition;
        }

        private void setPreviousEyePosition(Vec3 previousEyePosition) {
            this.previousEyePosition = previousEyePosition;
        }

        private boolean hasHit(UUID entityId) {
            return hitEntities.contains(entityId);
        }

        private void rememberHit(UUID entityId) {
            hitEntities.add(entityId);
        }
    }
}
