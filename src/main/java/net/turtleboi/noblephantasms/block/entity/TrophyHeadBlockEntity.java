package net.turtleboi.noblephantasms.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.turtleboi.noblephantasms.item.custom.TrophyHeadItem;
import net.turtleboi.noblephantasms.item.custom.TrophyHeadItem.TrophyData;
import org.jspecify.annotations.Nullable;

public class TrophyHeadBlockEntity extends BlockEntity {
    private static final String TAG_ENTITY_TYPE = "entity_type";
    private static final String TAG_TROPHY_DATA = "trophy_data";
    private @Nullable CustomData trophyData;

    public TrophyHeadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TROPHY_HEAD.get(), pos, state);
    }

    public @Nullable Identifier getEntityTypeId() {
        TrophyHeadItem.TrophyData data = getTrophyData();
        return data != null ? data.entityTypeId() : null;
    }

    public @Nullable TrophyData getTrophyData() {
        return TrophyHeadItem.getTrophyData(trophyData);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable(TAG_TROPHY_DATA, CompoundTag.CODEC,
                trophyData != null ? trophyData.copyTag() : null);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        trophyData = input.read(TAG_TROPHY_DATA, CompoundTag.CODEC)
                .map(CustomData::of)
                .orElseGet(() -> input.read(TAG_ENTITY_TYPE, Identifier.CODEC)
                        .map(TrophyHeadItem::createData)
                        .orElse(null));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        CustomData data = components.get(DataComponents.CUSTOM_DATA);
        trophyData = TrophyHeadItem.getTrophyData(data) != null ? data : null;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (trophyData != null) {
            components.set(DataComponents.CUSTOM_DATA, trophyData);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
