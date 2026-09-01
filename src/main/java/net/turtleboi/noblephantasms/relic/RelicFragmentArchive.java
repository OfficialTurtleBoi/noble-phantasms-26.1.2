package net.turtleboi.noblephantasms.relic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.turtleboi.noblephantasms.item.custom.RelicFragmentItem;

public record RelicFragmentArchive(List<RelicSet> sets) {
    public static final RelicFragmentArchive EMPTY = new RelicFragmentArchive(List.of());
    public static final Codec<RelicFragmentArchive> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RelicSet.CODEC.listOf().optionalFieldOf("sets", List.of()).forGetter(RelicFragmentArchive::sets)
    ).apply(instance, RelicFragmentArchive::new));

    public RelicFragmentArchive {
        sets = List.copyOf(sets);
    }

    public RelicSet get(Identifier relicId) {
        return sets.stream().filter(set -> set.relicId().equals(relicId)).findFirst().orElse(null);
    }

    public Reveal reveal(RelicFragmentItem.FragmentOrigin origin, RandomSource random) {
        List<RelicFragmentDefinitions.Definition> eligible = RelicFragmentDefinitions.definitions().stream()
                .filter(definition -> origin == RelicFragmentItem.FragmentOrigin.GENERIC
                        || definition.civilization() == origin)
                .filter(definition -> {
                    RelicSet set = get(definition.relicId());
                    return set == null || !set.complete();
                })
                .toList();
        if (eligible.isEmpty()) {
            return null;
        }

        List<RelicFragmentDefinitions.Definition> started = eligible.stream()
                .filter(definition -> get(definition.relicId()) != null)
                .toList();
        List<RelicFragmentDefinitions.Definition> pool = !started.isEmpty() && random.nextFloat() < 0.75F
                ? started : eligible;
        RelicFragmentDefinitions.Definition definition = pool.get(random.nextInt(pool.size()));
        RelicSet set = get(definition.relicId());
        if (set == null) {
            long seed = random.nextLong();
            RelicFragmenter.Layout layout = RelicFragmenter.create(definition.relicId(), seed);
            if (layout == null || layout.pieceCount() <= 0 || layout.pieceCount() >= Integer.SIZE) {
                return null;
            }
            set = new RelicSet(definition.relicId(), seed, layout.pieceCount(), 0);
        }

        List<Integer> missing = new ArrayList<>();
        for (int index = 0; index < set.pieceCount(); index++) {
            if ((set.discoveredMask() & 1 << index) == 0) {
                missing.add(index);
            }
        }
        if (missing.isEmpty()) {
            return null;
        }
        int pieceIndex = missing.get(random.nextInt(missing.size()));
        RelicSet updatedSet = new RelicSet(set.relicId(), set.seed(), set.pieceCount(),
                set.discoveredMask() | 1 << pieceIndex);
        List<RelicSet> updatedSets = new ArrayList<>(sets);
        updatedSets.removeIf(existing -> existing.relicId().equals(updatedSet.relicId()));
        updatedSets.add(updatedSet);
        return new Reveal(new RelicFragmentArchive(updatedSets),
                new RelicFragmentData(updatedSet.relicId(), updatedSet.seed(), pieceIndex,
                        updatedSet.pieceCount()));
    }

    public RelicFragmentArchive store(RelicFragmentData fragment) {
        if (!RelicFragmentDefinitions.supports(fragment.relicId())
                || fragment.pieceIndex() < 0
                || fragment.pieceCount() <= 0
                || fragment.pieceCount() >= Integer.SIZE) {
            return null;
        }
        RelicFragmenter.Layout layout = RelicFragmenter.createExact(
                fragment.relicId(), fragment.seed(), fragment.pieceCount());
        if (layout == null || fragment.pieceIndex() >= layout.pieceCount()) {
            return null;
        }
        RelicSet set = get(fragment.relicId());
        if (set != null && (set.seed() != fragment.seed()
                || set.pieceCount() != fragment.pieceCount()
                || (set.discoveredMask() & 1 << fragment.pieceIndex()) != 0)) {
            return null;
        }
        RelicSet updatedSet = set == null
                ? new RelicSet(fragment.relicId(), fragment.seed(), fragment.pieceCount(),
                1 << fragment.pieceIndex())
                : new RelicSet(set.relicId(), set.seed(), set.pieceCount(),
                set.discoveredMask() | 1 << fragment.pieceIndex());
        List<RelicSet> updatedSets = new ArrayList<>(sets);
        updatedSets.removeIf(existing -> existing.relicId().equals(fragment.relicId()));
        updatedSets.add(updatedSet);
        return new RelicFragmentArchive(updatedSets);
    }

    public RelicFragmentArchive consume(Identifier relicId, long seed) {
        RelicSet set = get(relicId);
        if (set == null || set.seed() != seed || !set.complete()) {
            return this;
        }
        List<RelicSet> updated = new ArrayList<>(sets);
        updated.removeIf(existing -> existing.relicId().equals(relicId));
        return new RelicFragmentArchive(updated);
    }

    public record RelicSet(Identifier relicId, long seed, int pieceCount, int discoveredMask) {
        public static final Codec<RelicSet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("relic").forGetter(RelicSet::relicId),
                Codec.LONG.fieldOf("seed").forGetter(RelicSet::seed),
                Codec.INT.fieldOf("piece_count").forGetter(RelicSet::pieceCount),
                Codec.INT.fieldOf("discovered_mask").forGetter(RelicSet::discoveredMask)
        ).apply(instance, RelicSet::new));

        public boolean complete() {
            return pieceCount > 0 && pieceCount < Integer.SIZE
                    && (discoveredMask & (1 << pieceCount) - 1) == (1 << pieceCount) - 1;
        }

        public int discoveredCount() {
            return Integer.bitCount(discoveredMask);
        }
    }

    public record Reveal(RelicFragmentArchive archive, RelicFragmentData fragment) {
    }
}
