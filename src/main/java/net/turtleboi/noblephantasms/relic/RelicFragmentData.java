package net.turtleboi.noblephantasms.relic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record RelicFragmentData(Identifier relicId, long seed, int pieceIndex, int pieceCount) {
    public static final Codec<RelicFragmentData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("relic").forGetter(RelicFragmentData::relicId),
            Codec.LONG.fieldOf("seed").forGetter(RelicFragmentData::seed),
            Codec.INT.fieldOf("piece_index").forGetter(RelicFragmentData::pieceIndex),
            Codec.INT.fieldOf("piece_count").forGetter(RelicFragmentData::pieceCount)
    ).apply(instance, RelicFragmentData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, RelicFragmentData> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, RelicFragmentData::relicId,
            ByteBufCodecs.VAR_LONG, RelicFragmentData::seed,
            ByteBufCodecs.VAR_INT, RelicFragmentData::pieceIndex,
            ByteBufCodecs.VAR_INT, RelicFragmentData::pieceCount,
            RelicFragmentData::new);

    public static RelicFragmentData forgePiece(Identifier relicId) {
        return new RelicFragmentData(relicId, 0L, -1, 0);
    }
}
