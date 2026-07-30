package net.turtleboi.noblephantasms.world;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;

public class ArtificialOreSavedData extends SavedData {
    private static final Codec<ArtificialOreSavedData> CODEC = BlockPos.CODEC.listOf()
            .xmap(ArtificialOreSavedData::new, data -> List.copyOf(data.positions));
    private static final SavedDataType<ArtificialOreSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "artificial_ores"),
            ArtificialOreSavedData::new, CODEC);
    private final Set<BlockPos> positions;

    public ArtificialOreSavedData() {
        positions = new HashSet<>();
    }

    public ArtificialOreSavedData(List<BlockPos> positions) {
        this.positions = new HashSet<>(positions);
    }

    public static ArtificialOreSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isArtificial(BlockPos pos) {
        return positions.contains(pos);
    }

    public void markArtificial(BlockPos pos) {
        if (positions.add(pos.immutable())) {
            setDirty();
        }
    }

    public void clear(BlockPos pos) {
        if (positions.remove(pos)) {
            setDirty();
        }
    }

    public static void handleBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof Player)) {
            return;
        }

        ArtificialOreSavedData data = get(level);
        if (event.getPlacedBlock().is(Tags.Blocks.ORES)) {
            data.markArtificial(event.getPos());
        } else {
            data.clear(event.getPos());
        }
    }

    public static void handlePistonMove(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        PistonStructureResolver resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) {
            return;
        }

        ArtificialOreSavedData data = get(level);
        Direction moveDirection = resolver.getPushDirection();
        Set<BlockPos> destinations = new HashSet<>();
        for (BlockPos source : resolver.getToPush()) {
            if (!data.isArtificial(source)) {
                continue;
            }
            data.clear(source);
            if (level.getBlockState(source).is(Tags.Blocks.ORES)) {
                destinations.add(source.relative(moveDirection));
            }
        }
        resolver.getToDestroy().forEach(data::clear);
        destinations.forEach(data::markArtificial);
    }
}
