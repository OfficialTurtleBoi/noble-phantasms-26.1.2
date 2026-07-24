package net.turtleboi.noblephantasms.datagen.providers;

import java.util.function.BiConsumer;
import net.minecraft.client.data.models.EquipmentAssetProvider;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.ModArmorMaterials;

public class ModEquipmentAssetProvider extends EquipmentAssetProvider {
    public ModEquipmentAssetProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void registerModels(BiConsumer<ResourceKey<EquipmentAsset>, EquipmentClientInfo> output) {
        output.accept(ModArmorMaterials.NORSE_KEY, EquipmentClientInfo.builder()
                .addLayers(EquipmentClientInfo.LayerType.HUMANOID,
                        new EquipmentClientInfo.Layer(
                                Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "hulioshjalmr_helmet")))
                .build());
    }

    @Override
    public String getName() {
        return "Equipment Asset Definitions - " + NoblePhantasms.MOD_ID;
    }
}
