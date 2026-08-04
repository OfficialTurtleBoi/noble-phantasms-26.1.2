package net.turtleboi.noblephantasms.item.custom;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.effect.ModEffects;

public class NekhakhaItem extends Item {
    private static final double FEAR_RANGE = 8.0;
    private static final int FEAR_DURATION = 20 * 3;
    private static final int FEAR_COOLDOWN = 20 * 12;
    private static final double DECREE_RADIUS = 4.0;
    private static final int DECREE_DURATION = 20 * 8;
    private static final int DECREE_COOLDOWN = 20 * 35;
    private static final Identifier SET_DAMAGE_ID =
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "nekhakha_set_damage");
    private static final Identifier SET_KNOCKBACK_ID =
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "nekhakha_set_knockback");
    private static final AttributeModifier SET_DAMAGE =
            new AttributeModifier(SET_DAMAGE_ID, 2.0, AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier SET_KNOCKBACK =
            new AttributeModifier(SET_KNOCKBACK_ID, 0.5, AttributeModifier.Operation.ADD_VALUE);
    private static final Map<UUID, FearState> FEARED_MOBS = new HashMap<>();
    private static final Map<UUID, Long> ACTIVE_DECREES = new HashMap<>();

    public NekhakhaItem(Properties properties) {
        super(properties
                .sword(ToolMaterial.NETHERITE, 3.0F, -2.6F)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (tryActivatePharaohsDecree(level, player)) {
            return InteractionResult.SUCCESS;
        }

        return activateFearPulse(level, player, player.getItemInHand(hand))
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }

    public static boolean tryActivatePharaohsDecree(Level level, Player player) {
        if (!HekaItem.isSetHeld(player)) {
            return false;
        }

        ItemStack heka = player.getMainHandItem().is(ModItems.HEKA)
                ? player.getMainHandItem() : player.getOffhandItem();
        ItemStack nekhakha = player.getMainHandItem().is(ModItems.NEKHAKHA)
                ? player.getMainHandItem() : player.getOffhandItem();
        if (player.getCooldowns().isOnCooldown(heka) || player.getCooldowns().isOnCooldown(nekhakha)) {
            return false;
        }
        if (level.isClientSide()) {
            return true;
        }

        ACTIVE_DECREES.put(player.getUUID(), level.getGameTime() + DECREE_DURATION);
        player.getCooldowns().addCooldown(heka, DECREE_COOLDOWN);
        player.getCooldowns().addCooldown(nekhakha, DECREE_COOLDOWN);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 0.8F, 0.8F);
        return true;
    }

    public static void handlePlayerTick(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        boolean paired = HekaItem.isSetHeld(player);
        updateSetModifiers(player, paired && player.getMainHandItem().is(ModItems.NEKHAKHA));

        Long decreeEnd = ACTIVE_DECREES.get(player.getUUID());
        if (decreeEnd == null) {
            return;
        }
        if (level.getGameTime() >= decreeEnd || !paired) {
            ACTIVE_DECREES.remove(player.getUUID());
            return;
        }

        applyPharaohsDecree(level, player);
    }

    public static void handleMobTick(Mob mob) {
        FearState fear = FEARED_MOBS.get(mob.getUUID());
        if (fear == null) {
            return;
        }
        if (mob.level().getGameTime() >= fear.endTick() || !mob.hasEffect(ModEffects.FEARED)) {
            FEARED_MOBS.remove(mob.getUUID());
            return;
        }

        mob.setTarget(null);
        if (mob.tickCount % 5 == 0) {
            Vec3 away = mob.position().subtract(fear.origin());
            if (away.horizontalDistanceSqr() < 0.01) {
                away = new Vec3(1.0, 0.0, 0.0);
            }
            Vec3 destination = mob.position().add(away.normalize().scale(8.0));
            mob.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.35);
        }
    }

    private static boolean activateFearPulse(Level level, Player player, ItemStack nekhakha) {
        if (player.getCooldowns().isOnCooldown(nekhakha)) {
            return false;
        }
        if (level.isClientSide()) {
            return true;
        }

        long endTick = level.getGameTime() + FEAR_DURATION;
        AABB area = player.getBoundingBox().inflate(FEAR_RANGE);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area, NekhakhaItem::isFearTarget)) {
            FEARED_MOBS.put(mob.getUUID(), new FearState(player.position(), endTick));
            mob.setTarget(null);
            mob.addEffect(new MobEffectInstance(ModEffects.FEARED, FEAR_DURATION, 0, false, true, true), player);
        }

        player.getCooldowns().addCooldown(nekhakha, FEAR_COOLDOWN);
        level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR,
                SoundSource.PLAYERS, 0.6F, 1.25F);
        return true;
    }

    private static void applyPharaohsDecree(ServerLevel level, Player player) {
        AABB court = player.getBoundingBox().inflate(DECREE_RADIUS);
        for (Animal animal : level.getEntitiesOfClass(Animal.class, court, LivingEntity::isAlive)) {
            if (animal.distanceToSqr(player) > 2.25) {
                animal.getNavigation().moveTo(player, 1.25);
            }
            animal.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 25, 0, false, false, true));
            if (player.tickCount % 20 == 0 && animal.getHealth() < animal.getMaxHealth()) {
                animal.heal(1.0F);
            }
        }

        for (Mob mob : level.getEntitiesOfClass(Mob.class, court, NekhakhaItem::isFearTarget)) {
            mob.setTarget(null);
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, false, true));
            if (player.tickCount % 5 == 0 && mob.distanceToSqr(player) < DECREE_RADIUS * DECREE_RADIUS) {
                mob.knockback(0.35, player.getX() - mob.getX(), player.getZ() - mob.getZ());
            }
        }
    }

    private static void updateSetModifiers(Player player, boolean active) {
        updateModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), SET_DAMAGE, active);
        updateModifier(player.getAttribute(Attributes.ATTACK_KNOCKBACK), SET_KNOCKBACK, active);
    }

    private static void updateModifier(AttributeInstance attribute, AttributeModifier modifier, boolean active) {
        if (attribute == null) {
            return;
        }
        if (active) {
            attribute.addOrUpdateTransientModifier(modifier);
        } else {
            attribute.removeModifier(modifier.id());
        }
    }

    private static boolean isFearTarget(Mob mob) {
        return mob instanceof Enemy
                && !(mob instanceof WitherBoss)
                && !(mob instanceof Warden)
                && !(mob instanceof EnderDragon);
    }

    private record FearState(Vec3 origin, long endTick) {
    }
}
