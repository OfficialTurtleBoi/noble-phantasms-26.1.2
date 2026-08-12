package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class BiaEnPetItem extends Item {
    public BiaEnPetItem(Properties properties) {
        super(properties
                .sword(ToolMaterial.IRON, 3.0F, -2.4F)
                .rarity(Rarity.RARE)
                .fireResistant());
    }

    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player player
                && event.getSource().getDirectEntity() == player
                && event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && player.getMainHandItem().getItem() instanceof BiaEnPetItem) {
            event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> 0.0F);
        }
    }
}
