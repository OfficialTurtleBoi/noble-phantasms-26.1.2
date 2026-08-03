package net.turtleboi.noblephantasms.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.turtleboi.noblephantasms.client.model.EagleKnightTalonsModel;
import net.turtleboi.noblephantasms.item.ModItems;

public class EagleKnightTalonsExtensions implements IClientItemExtensions {
    private EagleKnightTalonsModel talonsModel;

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(new EagleKnightTalonsExtensions(), ModItems.EAGLE_KNIGHT_TALONS.get());
    }

    @Override
    public Model getHumanoidArmorModel(ItemStack itemStack, EquipmentClientInfo.LayerType layerType, Model model) {
        if (layerType != EquipmentClientInfo.LayerType.HUMANOID) {
            return model;
        }

        if (talonsModel == null) {
            talonsModel = new EagleKnightTalonsModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(EagleKnightTalonsModel.LAYER_LOCATION));
        }
        return talonsModel;
    }
}
