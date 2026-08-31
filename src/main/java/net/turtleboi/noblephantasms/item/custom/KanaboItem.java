package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.turtleboi.noblephantasms.item.ModRarities;

/** Heavy relic club. Its on-hit stun will be added with the stun effect implementation. */
public final class KanaboItem extends Item {
    public KanaboItem(Properties properties) {
        super(properties
                .sword(ToolMaterial.NETHERITE, 7.0F, -3.5F)
                .rarity(ModRarities.LEGENDARY.getValue())
                .fireResistant());
    }
}
