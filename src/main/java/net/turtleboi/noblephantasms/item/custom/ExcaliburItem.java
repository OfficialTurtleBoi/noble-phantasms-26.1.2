package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.turtleboi.noblephantasms.item.ModRarities;

public class ExcaliburItem extends Item {
    public ExcaliburItem(Properties properties) {
        super(properties
                .sword(ToolMaterial.NETHERITE, 5.0F, -2.4F)
                .rarity(ModRarities.LEGENDARY.getValue())
                .fireResistant());
    }
}
