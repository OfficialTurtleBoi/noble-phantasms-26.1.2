package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import org.jspecify.annotations.Nullable;

public class WebenItem extends Item {
    private static final int MAX_SUNLIGHT_CHARGE = 20 * 10;
    private static final float FLARE_DAMAGE = 6.0F;

    public WebenItem(Properties properties) {
        super(properties
                .sword(ToolMaterial.NETHERITE, 3.0F, -2.2F)
                .rarity(Rarity.EPIC));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (!level.isBrightOutside()
                || !level.canSeeSky(entity.blockPosition())
                || level.isRainingAt(entity.blockPosition())) {
            return;
        }

        int charge = stack.getOrDefault(ModDataComponents.WEBEN_SUNLIGHT_CHARGE.get(), 0);
        if (charge < MAX_SUNLIGHT_CHARGE) {
            stack.set(ModDataComponents.WEBEN_SUNLIGHT_CHARGE.get(), charge + 1);
        }
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.level() instanceof ServerLevel level
                && stack.getOrDefault(ModDataComponents.WEBEN_SUNLIGHT_CHARGE.get(), 0) >= MAX_SUNLIGHT_CHARGE) {
            stack.set(ModDataComponents.WEBEN_SUNLIGHT_CHARGE.get(), 0);
            target.igniteForSeconds(4.0F);
            target.hurtServer(level, level.damageSources().onFire(), FLARE_DAMAGE);
            level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(), 40, 0.65, 0.65, 0.65, 0.08);
            level.playSound(null, target.blockPosition(), SoundEvents.FIRECHARGE_USE,
                    SoundSource.PLAYERS, 0.9F, 1.2F);
        }
        super.hurtEnemy(stack, target, attacker);
    }
}
