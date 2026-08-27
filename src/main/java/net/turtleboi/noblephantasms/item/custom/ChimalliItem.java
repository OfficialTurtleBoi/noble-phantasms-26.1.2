package net.turtleboi.noblephantasms.item.custom;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.phys.Vec3;

public final class ChimalliItem extends ShieldItem {
    private static final int BLOCK_DELAY_TICKS = 5;
    private static final int MAX_CHARGE_TICKS = 60;
    private static final int MIN_SPEED_TICKS = 20;
    private static final int MAX_SPEED_TICKS = 160;
    private static final double MIN_THRUST = 0.35;
    private static final double MAX_THRUST = 1.0;

    public ChimalliItem(Properties properties) {
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

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        int usedTicks = getUseDuration(stack, entity) - remainingTime;
        int chargeTicks = Math.min(MAX_CHARGE_TICKS, Math.max(0, usedTicks - BLOCK_DELAY_TICKS));
        if (chargeTicks == 0 || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        float progress = chargeTicks / (float) MAX_CHARGE_TICKS;
        int amplifier = Math.min(2, (chargeTicks - 1) / 20);
        int duration = MIN_SPEED_TICKS + Math.round(progress * (MAX_SPEED_TICKS - MIN_SPEED_TICKS));
        applyThrust(entity, progress);
        entity.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, amplifier, false, false, true));
        serverLevel.sendParticles(ParticleTypes.GUST, entity.getX(), entity.getY() + 0.15, entity.getZ(),
                4 + amplifier * 2, 0.35, 0.08, 0.35, 0.04);
        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.BREEZE_JUMP,
                SoundSource.PLAYERS, 0.65F + progress * 0.35F, 0.9F + progress * 0.25F);
        return true;
    }

    private static void applyThrust(LivingEntity entity, float progress) {
        Vec3 look = entity.getLookAngle().normalize();
        Vec3 movement = entity.getDeltaMovement();
        double strength = MIN_THRUST + progress * (MAX_THRUST - MIN_THRUST);
        double verticalThrust = look.y * strength * 0.25;
        entity.setDeltaMovement(movement.x * 0.35 + look.x * strength,
                movement.y + verticalThrust, movement.z * 0.35 + look.z * strength);
        entity.hurtMarked = true;
    }
}
