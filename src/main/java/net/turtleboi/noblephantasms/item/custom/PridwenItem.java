package net.turtleboi.noblephantasms.item.custom;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class PridwenItem extends ShieldItem {
    private static final double BARRIER_RANGE = 4.0;
    private static final double BARRIER_HALF_WIDTH = 2.5;

    public PridwenItem(Properties properties) {
        super(properties
                .durability(672)
                .component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                .repairable(ItemTags.WOODEN_TOOL_MATERIALS)
                .equippableUnswappable(EquipmentSlot.OFFHAND)
                .delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
                        0.25F,
                        1.0F,
                        List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
                        new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
                        Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                        Optional.of(SoundEvents.SHIELD_BLOCK),
                        Optional.of(SoundEvents.SHIELD_BREAK)))
                .component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK)
                .rarity(Rarity.RARE)
                .fireResistant());
    }

    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        if (event.getEntity() instanceof Player player && isRaised(player)
                && event.getSource().getDirectEntity() instanceof LivingEntity attacker
                && isInFront(player, attacker.position())) {
            attacker.knockback(0.8, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
            attacker.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WEAKNESS, 60, 0));
        }
        for (Player bearer : level.players()) {
            if (bearer == event.getEntity() || !isRaised(bearer)
                    || (!(event.getEntity() instanceof Player) && !bearer.isAlliedTo(event.getEntity()))
                    || bearer.distanceToSqr(event.getEntity()) > BARRIER_RANGE * BARRIER_RANGE
                    || !protects(bearer, event.getEntity(), event.getSource().getSourcePosition())) {
                continue;
            }
            event.setCanceled(true);
            level.playSound(null, bearer.blockPosition(), SoundEvents.SHIELD_BLOCK.value(),
                    bearer.getSoundSource(), 1.0F, 0.8F);
            ItemStack shield = bearer.getUseItem();
            BlocksAttacks blocks = shield.get(DataComponents.BLOCKS_ATTACKS);
            if (blocks != null) {
                blocks.hurtBlockingItem(level, shield, bearer, bearer.getUsedItemHand(), event.getAmount());
            }
            return;
        }
    }

    private static boolean protects(Player bearer, LivingEntity protectedEntity, Vec3 sourcePosition) {
        if (sourcePosition == null || !isInFront(bearer, sourcePosition)) {
            return false;
        }
        Vec3 forward = bearer.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        Vec3 offset = protectedEntity.position().subtract(bearer.position()).multiply(1.0, 0.0, 1.0);
        double depth = offset.dot(forward);
        Vec3 lateral = offset.subtract(forward.scale(depth));
        return depth <= 0.5 && depth >= -BARRIER_RANGE && lateral.lengthSqr() <= BARRIER_HALF_WIDTH * BARRIER_HALF_WIDTH;
    }

    private static boolean isInFront(Player bearer, Vec3 position) {
        Vec3 forward = bearer.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        Vec3 direction = position.subtract(bearer.position()).multiply(1.0, 0.0, 1.0).normalize();
        return forward.dot(direction) > 0.0;
    }

    private static boolean isRaised(Player player) {
        return player.isBlocking() && player.getUseItem().getItem() instanceof PridwenItem;
    }
}
