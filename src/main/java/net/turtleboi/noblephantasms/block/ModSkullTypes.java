package net.turtleboi.noblephantasms.block;

import net.minecraft.world.level.block.SkullBlock;
import net.turtleboi.noblephantasms.NoblePhantasms;

public enum ModSkullTypes implements SkullBlock.Type {
    TROPHY(NoblePhantasms.MOD_ID + ":trophy");

    private final String name;

    ModSkullTypes(String name) {
        this.name = name;
        SkullBlock.Type.TYPES.put(name, this);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
