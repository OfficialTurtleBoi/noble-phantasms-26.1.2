package net.turtleboi.noblephantasms.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.turtleboi.noblephantasms.client.model.HulioshjalmrModel;
import net.turtleboi.noblephantasms.item.ModItems;

public class HulioshjalmrExtensions implements IClientItemExtensions {
    private HulioshjalmrModel helmetModel;

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(new HulioshjalmrExtensions(), ModItems.HULIOSHJALMR.get());
    }

    @Override
    public Model getHumanoidArmorModel(ItemStack itemStack, EquipmentClientInfo.LayerType layerType, Model model) {
        if (layerType != EquipmentClientInfo.LayerType.HUMANOID) {
            return model;
        }

        if (this.helmetModel == null) {
            this.helmetModel = new HulioshjalmrModel(Minecraft.getInstance().getEntityModels().bakeLayer(HulioshjalmrModel.LAYER_LOCATION));
        }
        return this.helmetModel;
    }
}
