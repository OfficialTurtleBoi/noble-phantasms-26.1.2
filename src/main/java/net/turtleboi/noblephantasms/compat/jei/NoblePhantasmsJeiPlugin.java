package net.turtleboi.noblephantasms.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.ModItems;

@JeiPlugin
public final class NoblePhantasmsJeiPlugin implements IModPlugin {
    private static final Identifier PLUGIN_ID =
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addIngredientInfo(ModItems.ANKH.get(),
                Component.translatable("jei.noblephantasms.info.ankh"));
        registration.addIngredientInfo(ModItems.EYE_OF_HORUS.get(),
                Component.translatable("jei.noblephantasms.info.eye_of_horus"));
        registration.addIngredientInfo(ModItems.KHEPER_SCARAB.get(),
                Component.translatable("jei.noblephantasms.info.kheper_scarab"));
        registration.addIngredientInfo(ModItems.SCALES_OF_MAAT.get(),
                Component.translatable("jei.noblephantasms.info.scales_of_maat"));
        registration.addIngredientInfo(ModItems.ANDVARANAUT.get(),
                Component.translatable("jei.noblephantasms.info.andvaranaut"));
        registration.addIngredientInfo(ModItems.DRAUPNIR.get(),
                Component.translatable("jei.noblephantasms.info.draupnir"));
        registration.addIngredientInfo(ModItems.MEGINGJORD.get(),
                Component.translatable("jei.noblephantasms.info.meginjord"));
        registration.addIngredientInfo(ModItems.SCABBARD.get(),
                Component.translatable("jei.noblephantasms.info.scabbard"));
        registration.addIngredientInfo(ModItems.BOOK_OF_THOTH.get(),
                Component.translatable("jei.noblephantasms.info.book_of_thoth"));
        registration.addIngredientInfo(ModItems.EAGLE_KNIGHT_TALONS.get(),
                Component.translatable("jei.noblephantasms.info.eagle_knight_talons"));
        registration.addIngredientInfo(ModItems.KAZAGURUMA.get(),
                Component.translatable("jei.noblephantasms.info.kazaguruma"));
    }
}
