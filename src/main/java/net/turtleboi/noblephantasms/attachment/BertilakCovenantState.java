package net.turtleboi.noblephantasms.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

public record BertilakCovenantState(Optional<UUID> targetId, long nextBindAt) {
    public static final BertilakCovenantState EMPTY = new BertilakCovenantState(Optional.empty(), 0L);
    public static final MapCodec<BertilakCovenantState> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            UUIDUtil.CODEC.optionalFieldOf("target").forGetter(BertilakCovenantState::targetId),
            Codec.LONG.optionalFieldOf("next_bind_at", 0L).forGetter(BertilakCovenantState::nextBindAt)
    ).apply(instance, BertilakCovenantState::new));

    public boolean targets(UUID entityId) {
        return targetId.filter(entityId::equals).isPresent();
    }

    public BertilakCovenantState clearTarget() {
        return new BertilakCovenantState(Optional.empty(), nextBindAt);
    }
}
