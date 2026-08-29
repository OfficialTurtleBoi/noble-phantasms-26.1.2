package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.turtleboi.noblephantasms.particle.FireFangsParticleOptions;
import net.turtleboi.noblephantasms.component.ModDataComponents;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class GramItem extends Item {
    private static final ToolMaterial GRAM_MATERIAL = new ToolMaterial(
            ToolMaterial.DIAMOND.incorrectBlocksForDrops(),
            ToolMaterial.NETHERITE.durability(),
            ToolMaterial.DIAMOND.speed(),
            ToolMaterial.DIAMOND.attackDamageBonus(),
            ToolMaterial.DIAMOND.enchantmentValue(),
            ToolMaterial.DIAMOND.repairItems());
    private static final int HITS_PER_BITE = 3;
    private static final Map<ServerPlayer, PendingAttack> PENDING_ATTACKS = new WeakHashMap<>();

    public GramItem(Properties properties) {
        super(properties
                .sword(GRAM_MATERIAL, 3.0F, -2.8F)
                .rarity(Rarity.EPIC)
                .component(ModDataComponents.GRAM_HIT_COUNT.get(), 0));
    }

    public static void handleAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PENDING_ATTACKS.remove(player);
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GramItem)
                || !(event.getTarget() instanceof LivingEntity target)
                || !isValidTarget(target)
                || player.getAttackStrengthScale(0.5F) <= 0.9F) {
            return;
        }

        PENDING_ATTACKS.put(player, new PendingAttack(
                target.getUUID(), player.level().getGameTime()));
    }

    public static void handleDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getSource().getDirectEntity() != player
                || !event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
            return;
        }

        PendingAttack pending = PENDING_ATTACKS.get(player);
        ItemStack stack = player.getMainHandItem();
        if (pending == null
                || pending.gameTime() != player.level().getGameTime()
                || !pending.targetId().equals(event.getEntity().getUUID())
                || !(stack.getItem() instanceof GramItem gram)) {
            return;
        }

        PENDING_ATTACKS.remove(player);
        gram.handleHit(player, stack, event);
    }

    public static void handleLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player instanceof ServerPlayer serverPlayer) {
                PENDING_ATTACKS.remove(serverPlayer);
            }
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.getItem() instanceof GramItem) {
                    resetBite(stack);
                }
            }
        }
    }

    private void handleHit(ServerPlayer player, ItemStack stack, LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        float damage = event.getNewDamage();
        if (!isValidTarget(target)
                || damage <= target.getAbsorptionAmount()
                || event.getContainer().getReduction(DamageContainer.Reduction.INVULNERABILITY) > 0.0F) {
            return;
        }

        long gameTime = player.level().getGameTime();

        int hitCount = getHitCount(stack) + 1;
        if (hitCount < HITS_PER_BITE) {
            stack.set(ModDataComponents.GRAM_HIT_COUNT.get(), hitCount);
            if (hitCount == HITS_PER_BITE - 1) {
                stack.set(ModDataComponents.GRAM_READY_TICK.get(), gameTime);
            }
            return;
        }

        resetBite(stack);
        float finalDamage = damage + calculateBonusDamage(target, player);
        event.setNewDamage(finalDamage);
        if (target.level() instanceof ServerLevel level) {
            float healthDamage = Math.min(target.getHealth(),
                    Math.max(0.0F, finalDamage - target.getAbsorptionAmount()));
            spawnBiteParticles(level, target, healthDamage);
        }
    }

    private static boolean isValidTarget(LivingEntity target) {
        return !(target instanceof ArmorStand);
    }

    private static float calculateBonusDamage(LivingEntity target, Player player) {
        float difference = target.getMaxHealth() - player.getMaxHealth();
        return difference <= 0.0F ? 0.0F : (float) (10.0 * Math.log1p(difference / 10.0));
    }

    private static void spawnBiteParticles(ServerLevel level, LivingEntity target, float damage) {
        level.sendParticles(new FireFangsParticleOptions(
                        damage, target.getBbWidth(), target.getBbHeight(), target.getId()),
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
    }

    public static boolean isBiteReady(ItemStack stack) {
        return getHitCount(stack) >= HITS_PER_BITE - 1;
    }

    private static int getHitCount(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(ModDataComponents.GRAM_HIT_COUNT.get(), 0),
                0, HITS_PER_BITE - 1);
    }

    private static void resetBite(ItemStack stack) {
        stack.set(ModDataComponents.GRAM_HIT_COUNT.get(), 0);
        stack.remove(ModDataComponents.GRAM_READY_TICK.get());
        stack.remove(ModDataComponents.GRAM_LAST_HIT_TICK.get());
    }

    private record PendingAttack(UUID targetId, long gameTime) {
    }
}
