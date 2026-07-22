package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.turtleboi.noblephantasms.item.ModRarities;

public class KhopeshOfRaItem extends Item {
    public KhopeshOfRaItem(Properties properties) {
        super(properties
                .sword(ToolMaterial.NETHERITE, 3.0F, -2.2F)
                .rarity(Rarity.EPIC));
    }
}
