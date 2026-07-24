package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.Instruments;
import net.minecraft.world.item.component.InstrumentComponent;
import net.turtleboi.noblephantasms.item.ModRarities;

public class GjallarhornItem extends InstrumentItem {
    public GjallarhornItem(Properties properties) {
        super(properties
                .stacksTo(1)
                .delayedComponent(DataComponents.INSTRUMENT,
                        context -> new InstrumentComponent(context.getOrThrow(Instruments.PONDER_GOAT_HORN)))
                .rarity(ModRarities.LEGENDARY.getValue())
                .fireResistant());
    }
}
