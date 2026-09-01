package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.turtleboi.noblephantasms.effect.custom.StunnedEffect;
import net.turtleboi.noblephantasms.item.ModRarities;

/** Heavy relic club whose very slow swing guarantees a short, complete stun on hit. */
public final class KanaboItem extends Item {
    public static final int STUN_DURATION = 20;

    public KanaboItem(Properties properties) {
        super(properties
                .sword(ToolMaterial.NETHERITE, 7.0F, -3.5F)
                .rarity(ModRarities.LEGENDARY.getValue())
                .fireResistant());
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.level() instanceof ServerLevel) {
            StunnedEffect.apply(target, attacker, STUN_DURATION);
        }
        super.hurtEnemy(stack, target, attacker);
    }
}
