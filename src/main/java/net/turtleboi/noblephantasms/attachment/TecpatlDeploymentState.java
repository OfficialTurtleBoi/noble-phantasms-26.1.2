package net.turtleboi.noblephantasms.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

public record TecpatlDeploymentState(Optional<UUID> batchId, ItemStack dagger,
                                     boolean mainHand, long recoverAt) {
    public static final TecpatlDeploymentState EMPTY = new TecpatlDeploymentState(
            Optional.empty(), ItemStack.EMPTY, true, 0L);
    public static final MapCodec<TecpatlDeploymentState> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            UUIDUtil.CODEC.optionalFieldOf("batch").forGetter(TecpatlDeploymentState::batchId),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("dagger", ItemStack.EMPTY)
                    .forGetter(TecpatlDeploymentState::dagger),
            Codec.BOOL.optionalFieldOf("main_hand", true).forGetter(TecpatlDeploymentState::mainHand),
            Codec.LONG.optionalFieldOf("recover_at", 0L).forGetter(TecpatlDeploymentState::recoverAt)
    ).apply(instance, TecpatlDeploymentState::new));

    public boolean matches(UUID batch) {
        return batchId.filter(batch::equals).isPresent();
    }

    public TecpatlDeploymentState refresh(long recoveryTime) {
        return new TecpatlDeploymentState(batchId, dagger, mainHand, recoveryTime);
    }
}
