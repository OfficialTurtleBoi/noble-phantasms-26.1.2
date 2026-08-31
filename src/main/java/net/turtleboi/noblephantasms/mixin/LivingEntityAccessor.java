package net.turtleboi.noblephantasms.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("getDeathSound")
    @Nullable SoundEvent noblePhantasms$getDeathSound();

    @Accessor("useItem")
    void noblePhantasms$setUseItem(ItemStack itemStack);

    @Accessor("useItemRemaining")
    void noblePhantasms$setUseItemRemaining(int remainingTicks);
}
