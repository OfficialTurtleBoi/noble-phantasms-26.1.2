package net.turtleboi.noblephantasms.item;

import com.google.common.collect.Maps;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.tags.ModTags;

import java.util.Map;

public class ModArmorMaterials {
    public static final ResourceKey<? extends Registry<EquipmentAsset>> ROOT_ID =
            ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("equipment_asset"));

    public static final ResourceKey<EquipmentAsset> NORSE_MYTH_KEY =
            ResourceKey.create(ROOT_ID, Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "norse_myth"));

    public static final ResourceKey<EquipmentAsset> MESOAMERICAN_MYTH_KEY =
            ResourceKey.create(ROOT_ID, Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "mesoamerican_myth"));

    public static final ArmorMaterial NORSE_MYTH_MATERIAL = new ArmorMaterial(48,
            makeDefense(3, 6, 8, 5, 21), 21, SoundEvents.ARMOR_EQUIP_GOLD,
            3f, 0.2f, ModTags.Items.NORSE_REPAIRABLE, NORSE_MYTH_KEY);

    public static final ArmorMaterial MESOAMERICAN_MYTH_MATERIAL = new ArmorMaterial(48,
            makeDefense(3, 6, 8, 5, 21), 21, SoundEvents.ARMOR_EQUIP_LEATHER,
            3f, 0.2f, ModTags.Items.MESOAMERICAN_REPAIRABLE, MESOAMERICAN_MYTH_KEY);

    private static Map<ArmorType, Integer> makeDefense(int boots, int leggings, int chestplate, int helmet, int body) {
        return Maps.newEnumMap(Map.of(ArmorType.BOOTS, boots, ArmorType.LEGGINGS, leggings,
                ArmorType.CHESTPLATE, chestplate, ArmorType.HELMET, helmet, ArmorType.BODY, body));
    }
}
