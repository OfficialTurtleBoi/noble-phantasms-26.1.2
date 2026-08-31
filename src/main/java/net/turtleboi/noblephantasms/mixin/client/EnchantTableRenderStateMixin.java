package net.turtleboi.noblephantasms.mixin.client;

import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.turtleboi.noblephantasms.client.renderer.MedjuNetjerBookRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EnchantTableRenderState.class)
public abstract class EnchantTableRenderStateMixin implements MedjuNetjerBookRenderState {
    @Unique
    private boolean noblePhantasms$medjuNetjerInstalled;

    @Override
    public boolean noblePhantasms$hasMedjuNetjer() {
        return noblePhantasms$medjuNetjerInstalled;
    }

    @Override
    public void noblePhantasms$setMedjuNetjer(boolean installed) {
        noblePhantasms$medjuNetjerInstalled = installed;
    }
}
