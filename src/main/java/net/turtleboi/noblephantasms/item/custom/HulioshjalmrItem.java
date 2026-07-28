package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.turtleboi.noblephantasms.item.ModArmorMaterials;

public class HulioshjalmrItem extends Item {
    public HulioshjalmrItem(Properties properties) {
        super(properties.humanoidArmor(ModArmorMaterials.NORSE_MYTH_MATERIAL, ArmorType.HELMET));
    }
}
