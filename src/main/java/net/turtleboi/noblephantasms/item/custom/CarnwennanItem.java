package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.turtleboi.noblephantasms.item.ModRarities;

public class CarnwennanItem extends Item {
    public CarnwennanItem(Properties properties) {
        super(properties
                .sword(ToolMaterial.NETHERITE, 0.0F, -1.2F)
                .rarity(Rarity.EPIC));
    }
}
