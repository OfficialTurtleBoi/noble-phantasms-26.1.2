package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.turtleboi.noblephantasms.effect.custom.BleedEffect;

public final class MacuahuitlItem extends Item {
    private static final int BLEED_DURATION = 20 * 5;

    public MacuahuitlItem(Properties properties) {
        super(properties
                .sword(ToolMaterial.NETHERITE, 3.0F, -2.4F)
                .rarity(Rarity.RARE));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.level() instanceof ServerLevel) {
            BleedEffect.applyOrAmplifyBleed(target, BLEED_DURATION, 0, attacker);
        }
        super.hurtEnemy(stack, target, attacker);
    }
}
