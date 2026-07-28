package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.world.item.Item;
import net.turtleboi.noblephantasms.item.ModRarities;

public class AndvaranautItem extends Item {
    public AndvaranautItem(Properties properties) {
        super(properties
                .stacksTo(1)
                .rarity(ModRarities.LEGENDARY.getValue())
                .fireResistant());
    }
}
