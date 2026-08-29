package net.turtleboi.noblephantasms.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.item.custom.HolyGrailItem;
import org.jspecify.annotations.Nullable;

public record HolyGrailChargeProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<HolyGrailChargeProperty> MAP_CODEC =
            MapCodec.unit(new HolyGrailChargeProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return HolyGrailItem.getChargeProgress(stack);
    }

    @Override
    public MapCodec<HolyGrailChargeProperty> type() {
        return MAP_CODEC;
    }
}
