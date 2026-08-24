package net.turtleboi.noblephantasms.item.custom;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.turtleboi.noblephantasms.particle.ModParticles;

public final class GramItem extends Item {
    private static final ToolMaterial GRAM_MATERIAL = new ToolMaterial(
            ToolMaterial.DIAMOND.incorrectBlocksForDrops(),
            ToolMaterial.NETHERITE.durability(),
            ToolMaterial.DIAMOND.speed(),
            ToolMaterial.DIAMOND.attackDamageBonus(),
            ToolMaterial.DIAMOND.enchantmentValue(),
            ToolMaterial.DIAMOND.repairItems());
    private static final int HITS_PER_BITE = 3;
    private static final Map<Player, IdentityHashMap<ItemStack, Integer>> HIT_COUNTS = new WeakHashMap<>();

    public GramItem(Properties properties) {
        super(properties
                .sword(GRAM_MATERIAL, 3.0F, -2.8F)
                .rarity(Rarity.EPIC));
    }

    public static void handleDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getSource().getDirectEntity() != player
                || !event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || !(player.getMainHandItem().getItem() instanceof GramItem gram)
                || player.getAttackStrengthScale(0.5F) < 1.0F) {
            return;
        }
        gram.handleHit(player, player.getMainHandItem(), event);
    }

    public static void handleLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            HIT_COUNTS.remove(player);
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

        IdentityHashMap<ItemStack, Integer> counts = HIT_COUNTS.computeIfAbsent(
                player, ignored -> new IdentityHashMap<>());
        int hitCount = counts.getOrDefault(stack, 0) + 1;
        if (hitCount < HITS_PER_BITE) {
            counts.put(stack, hitCount);
            return;
        }

        counts.remove(stack);
        if (counts.isEmpty()) {
            HIT_COUNTS.remove(player);
        }
        event.setNewDamage(damage + calculateBonusDamage(target, player));
        if (target.level() instanceof ServerLevel level) {
            spawnBiteParticles(level, target);
        }
    }

    private static boolean isValidTarget(LivingEntity target) {
        return !(target instanceof ArmorStand)
                && (target instanceof Enemy || target instanceof NeutralMob);
    }

    private static float calculateBonusDamage(LivingEntity target, Player player) {
        float difference = target.getMaxHealth() - player.getMaxHealth();
        return difference <= 0.0F ? 0.0F : (float) (10.0 * Math.log1p(difference / 10.0));
    }

    private static void spawnBiteParticles(ServerLevel level, LivingEntity target) {
        level.sendParticles(ModParticles.FIRE_FANGS.get(),
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.FLAME,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                18, target.getBbWidth() * 0.45, target.getBbHeight() * 0.35,
                target.getBbWidth() * 0.45, 0.08);
    }
}
