package net.turtleboi.noblephantasms.item.custom;

import java.util.List;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.turtleboi.noblephantasms.item.ModItems;

public final class AndvaranautItem extends CurioRelicItem {
    public AndvaranautItem(Properties properties) {
        super(properties.rarity(Rarity.EPIC));
    }

    public static void handleDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Player player
                && isEquipped(player, ModItems.ANDVARANAUT.get())) {
            event.setNewDamage(event.getNewDamage() * 2.0F);
        }
    }

    public static void handleDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)
                || event.getEntity() instanceof Player
                || !isEquipped(player, ModItems.ANDVARANAUT.get())) {
            return;
        }

        for (ItemEntity original : List.copyOf(event.getDrops())) {
            ItemEntity duplicate = new ItemEntity(
                    original.level(),
                    original.getX(),
                    original.getY(),
                    original.getZ(),
                    original.getItem().copy());
            duplicate.setDeltaMovement(original.getDeltaMovement());
            event.getDrops().add(duplicate);
        }
    }
}
