package net.turtleboi.noblephantasms.relic;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.imageio.ImageIO;
import net.minecraft.resources.Identifier;

public final class RelicFragmenter {
    private static final int MINIMUM_PIXELS_PER_PIECE = 6;
    private static final double MAXIMUM_SIZE_RATIO = 1.3;
    private static final int MAXIMUM_ATTEMPTS = 32;
    private static final Map<Key, Layout> CACHE = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, Layout> eldest) {
            return size() > 128;
        }
    };

    public static Layout create(Identifier relicId, long seed) {
        RelicFragmentDefinitions.Definition definition = RelicFragmentDefinitions.get(relicId);
        return definition == null ? null : create(definition, definition.textureId(), seed);
    }

    public static Layout createForStation(Identifier relicId, long seed) {
        RelicFragmentDefinitions.Definition definition = RelicFragmentDefinitions.get(relicId);
        return definition == null ? null : create(definition, definition.stationTextureId(), seed);
    }

    private static Layout create(RelicFragmentDefinitions.Definition definition,
                                 Identifier textureId, long seed) {
        Key key = new Key(textureId.toString(), definition.textureFrameHeight(), seed);
        synchronized (CACHE) {
            Layout cached = CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }

        ImagePixels image = load(textureId, definition.textureFrameHeight());
        int minimumPieces = Math.clamp(definition.minimumPieces(),
                image.components().size(), image.opaquePixels().size());
        int requestedPieces = Math.clamp(image.opaquePixels().size() / MINIMUM_PIXELS_PER_PIECE,
                minimumPieces, Math.max(minimumPieces, definition.maximumPieces()));
        Layout result = null;
        for (int count = requestedPieces; count >= minimumPieces && result == null; count--) {
            for (int attempt = 0; attempt < MAXIMUM_ATTEMPTS; attempt++) {
                long attemptSeed = mix(seed + count * 341873128712L + attempt * 132897987541L);
                Layout candidate = partition(image, count, new Random(attemptSeed));
                if (candidate != null && isBalanced(candidate)) {
                    result = candidate;
                    break;
                }
            }
        }
        if (result == null) {
            result = partition(image, minimumPieces, new Random(seed));
        }

        synchronized (CACHE) {
            CACHE.put(key, result);
        }
        return result;
    }

    private static ImagePixels load(Identifier textureId, int frameHeight) {
        String path = "/assets/" + textureId.getNamespace() + "/textures/" + textureId.getPath() + ".png";
        try (InputStream stream = RelicFragmenter.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing relic texture " + path);
            }
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                throw new IllegalStateException("Unable to read relic texture " + path);
            }
            if (frameHeight > 0) {
                if (frameHeight > image.getHeight()) {
                    throw new IllegalStateException("Relic texture frame exceeds image height " + path);
                }
                image = image.getSubimage(0, 0, image.getWidth(), frameHeight);
            }
            return readPixels(image);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read relic texture " + path, exception);
        }
    }

    private static ImagePixels readPixels(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[] visited = new boolean[width * height];
        List<Integer> opaquePixels = new ArrayList<>();
        List<List<Integer>> components = new ArrayList<>();
        Map<Integer, Integer> colors = new HashMap<>();
        for (int index = 0; index < visited.length; index++) {
            if (visited[index] || alpha(image.getRGB(index % width, index / width)) == 0) {
                continue;
            }
            List<Integer> component = new ArrayList<>();
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            visited[index] = true;
            queue.add(index);
            while (!queue.isEmpty()) {
                int current = queue.removeFirst();
                component.add(current);
                opaquePixels.add(current);
                colors.put(current, image.getRGB(current % width, current / width));
                for (int neighbor : neighbors(current, width, height)) {
                    if (!visited[neighbor] && alpha(image.getRGB(neighbor % width, neighbor / width)) != 0) {
                        visited[neighbor] = true;
                        queue.add(neighbor);
                    }
                }
            }
            components.add(List.copyOf(component));
        }
        if (opaquePixels.isEmpty()) {
            throw new IllegalStateException("Relic texture has no opaque pixels");
        }
        return new ImagePixels(width, height, List.copyOf(opaquePixels),
                List.copyOf(components), Map.copyOf(colors));
    }

    private static Layout partition(ImagePixels image, int count, Random random) {
        int[] owner = new int[image.width() * image.height()];
        Arrays.fill(owner, -2);
        for (int pixel : image.opaquePixels()) {
            owner[pixel] = -1;
        }
        List<Integer> seeds = chooseSeeds(image, count, random);
        List<Set<Integer>> frontiers = new ArrayList<>();
        int[] sizes = new int[count];
        for (int region = 0; region < count; region++) {
            frontiers.add(new HashSet<>());
            int pixel = seeds.get(region);
            owner[pixel] = region;
            sizes[region] = 1;
        }
        for (int region = 0; region < count; region++) {
            addUnassignedNeighbors(seeds.get(region), region, owner, frontiers, image.width(), image.height());
        }

        int remaining = image.opaquePixels().size() - count;
        while (remaining > 0) {
            int minimum = Integer.MAX_VALUE;
            List<Integer> candidates = new ArrayList<>();
            for (int region = 0; region < count; region++) {
                frontiers.get(region).removeIf(pixel -> owner[pixel] != -1);
                if (frontiers.get(region).isEmpty()) {
                    continue;
                }
                if (sizes[region] < minimum) {
                    minimum = sizes[region];
                    candidates.clear();
                    candidates.add(region);
                } else if (sizes[region] == minimum) {
                    candidates.add(region);
                }
            }
            if (candidates.isEmpty()) {
                return null;
            }
            int region = candidates.get(random.nextInt(candidates.size()));
            int pixel = chooseFrontierPixel(frontiers.get(region), region, owner,
                    image.width(), image.height(), random);
            owner[pixel] = region;
            sizes[region]++;
            frontiers.get(region).remove(pixel);
            addUnassignedNeighbors(pixel, region, owner, frontiers, image.width(), image.height());
            remaining--;
        }

        List<List<Pixel>> pixels = new ArrayList<>();
        for (int region = 0; region < count; region++) {
            pixels.add(new ArrayList<>());
        }
        for (int pixel : image.opaquePixels()) {
            pixels.get(owner[pixel]).add(new Pixel(
                    pixel % image.width(), pixel / image.width(), image.colors().get(pixel)));
        }
        List<Piece> pieces = new ArrayList<>();
        for (List<Pixel> regionPixels : pixels) {
            int minX = regionPixels.stream().mapToInt(Pixel::x).min().orElse(0);
            int minY = regionPixels.stream().mapToInt(Pixel::y).min().orElse(0);
            int maxX = regionPixels.stream().mapToInt(Pixel::x).max().orElse(0);
            int maxY = regionPixels.stream().mapToInt(Pixel::y).max().orElse(0);
            pieces.add(new Piece(List.copyOf(regionPixels), minX, minY, maxX, maxY));
        }
        return new Layout(image.width(), image.height(), List.copyOf(pieces));
    }

    private static List<Integer> chooseSeeds(ImagePixels image, int count, Random random) {
        List<Integer> seeds = new ArrayList<>();
        for (List<Integer> component : image.components()) {
            seeds.add(component.get(random.nextInt(component.size())));
        }
        while (seeds.size() < count) {
            int bestDistance = -1;
            List<Integer> best = new ArrayList<>();
            for (int pixel : image.opaquePixels()) {
                if (seeds.contains(pixel)) {
                    continue;
                }
                int distance = seeds.stream()
                        .mapToInt(seed -> manhattan(pixel, seed, image.width()))
                        .min()
                        .orElse(0);
                if (distance > bestDistance) {
                    bestDistance = distance;
                    best.clear();
                    best.add(pixel);
                } else if (distance == bestDistance) {
                    best.add(pixel);
                }
            }
            seeds.add(best.get(random.nextInt(best.size())));
        }
        return seeds;
    }

    private static int chooseFrontierPixel(Set<Integer> frontier, int region, int[] owner,
                                           int width, int height, Random random) {
        int bestScore = -1;
        List<Integer> best = new ArrayList<>();
        for (int pixel : frontier) {
            int score = 0;
            for (int neighbor : neighbors(pixel, width, height)) {
                if (owner[neighbor] == region) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best.clear();
                best.add(pixel);
            } else if (score == bestScore) {
                best.add(pixel);
            }
        }
        return best.get(random.nextInt(best.size()));
    }

    private static void addUnassignedNeighbors(int pixel, int region, int[] owner,
                                               List<Set<Integer>> frontiers, int width, int height) {
        for (int neighbor : neighbors(pixel, width, height)) {
            if (owner[neighbor] == -1) {
                frontiers.get(region).add(neighbor);
            }
        }
    }

    private static int[] neighbors(int pixel, int width, int height) {
        int x = pixel % width;
        int y = pixel / width;
        int[] result = new int[4];
        int size = 0;
        if (x > 0) result[size++] = pixel - 1;
        if (x + 1 < width) result[size++] = pixel + 1;
        if (y > 0) result[size++] = pixel - width;
        if (y + 1 < height) result[size++] = pixel + width;
        return Arrays.copyOf(result, size);
    }

    private static boolean isBalanced(Layout layout) {
        int minimum = layout.pieces().stream().mapToInt(piece -> piece.pixels().size()).min().orElse(1);
        int maximum = layout.pieces().stream().mapToInt(piece -> piece.pixels().size()).max().orElse(1);
        return minimum > 0 && (double) maximum / minimum <= MAXIMUM_SIZE_RATIO;
    }

    public static Piece assemble(Layout layout) {
        List<Pixel> pixels = layout.pieces().stream()
                .flatMap(piece -> piece.pixels().stream())
                .toList();
        int minX = pixels.stream().mapToInt(Pixel::x).min().orElse(0);
        int minY = pixels.stream().mapToInt(Pixel::y).min().orElse(0);
        int maxX = pixels.stream().mapToInt(Pixel::x).max().orElse(0);
        int maxY = pixels.stream().mapToInt(Pixel::y).max().orElse(0);
        return new Piece(pixels, minX, minY, maxX, maxY);
    }

    public static OutlineMask createOutline(Piece piece) {
        Set<Long> occupied = new HashSet<>();
        for (Pixel pixel : piece.pixels()) {
            occupied.add(point(pixel.x(), pixel.y()));
        }
        List<BoundaryEdge> edges = new ArrayList<>();
        for (Pixel pixel : piece.pixels()) {
            int x = pixel.x();
            int y = pixel.y();
            if (!occupied.contains(point(x, y - 1))) edges.add(new BoundaryEdge(x, y, x + 1, y));
            if (!occupied.contains(point(x + 1, y))) edges.add(new BoundaryEdge(x + 1, y, x + 1, y + 1));
            if (!occupied.contains(point(x, y + 1))) edges.add(new BoundaryEdge(x + 1, y + 1, x, y + 1));
            if (!occupied.contains(point(x - 1, y))) edges.add(new BoundaryEdge(x, y + 1, x, y));
        }
        Map<Long, List<BoundaryEdge>> byStart = new HashMap<>();
        for (BoundaryEdge edge : edges) {
            byStart.computeIfAbsent(point(edge.x0, edge.y0), ignored -> new ArrayList<>()).add(edge);
        }
        List<BoundaryEdge> ordered = new ArrayList<>();
        Set<BoundaryEdge> used = new HashSet<>();
        while (used.size() < edges.size()) {
            BoundaryEdge current = edges.stream()
                    .filter(edge -> !used.contains(edge))
                    .min((first, second) -> {
                        int yComparison = Integer.compare(first.y0, second.y0);
                        return yComparison != 0 ? yComparison : Integer.compare(first.x0, second.x0);
                    })
                    .orElseThrow();
            while (current != null && used.add(current)) {
                ordered.add(current);
                List<BoundaryEdge> candidates = byStart.getOrDefault(
                        point(current.x1, current.y1), List.of());
                current = chooseNextEdge(current, candidates, used);
            }
        }
        LinkedHashSet<OutlinePixel> pixels = new LinkedHashSet<>();
        for (BoundaryEdge edge : ordered) {
            pixels.add(edge.outsidePixel());
        }
        int minX = pixels.stream().mapToInt(OutlinePixel::x).min().orElse(piece.minX());
        int minY = pixels.stream().mapToInt(OutlinePixel::y).min().orElse(piece.minY());
        int maxX = pixels.stream().mapToInt(OutlinePixel::x).max().orElse(piece.maxX());
        int maxY = pixels.stream().mapToInt(OutlinePixel::y).max().orElse(piece.maxY());
        return new OutlineMask(List.copyOf(pixels), minX, minY, maxX, maxY);
    }

    public static List<OutlinePixel> startOutlineNear(List<OutlinePixel> outline, Piece piece) {
        if (outline.isEmpty()) {
            return outline;
        }
        int nearestIndex = 0;
        int nearestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < outline.size(); index++) {
            OutlinePixel outlinePixel = outline.get(index);
            for (Pixel piecePixel : piece.pixels()) {
                int distance = Math.abs(outlinePixel.x - piecePixel.x())
                        + Math.abs(outlinePixel.y - piecePixel.y());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestIndex = index;
                }
            }
        }
        List<OutlinePixel> reordered = new ArrayList<>(outline.size());
        reordered.addAll(outline.subList(nearestIndex, outline.size()));
        reordered.addAll(outline.subList(0, nearestIndex));
        return List.copyOf(reordered);
    }

    private static BoundaryEdge chooseNextEdge(BoundaryEdge previous, List<BoundaryEdge> candidates,
                                               Set<BoundaryEdge> used) {
        int previousDirection = previous.direction();
        BoundaryEdge best = null;
        int bestTurn = Integer.MAX_VALUE;
        for (BoundaryEdge candidate : candidates) {
            if (used.contains(candidate)) {
                continue;
            }
            int turn = (candidate.direction() - previousDirection + 4) % 4;
            int priority = switch (turn) {
                case 1 -> 0;
                case 0 -> 1;
                case 3 -> 2;
                default -> 3;
            };
            if (priority < bestTurn) {
                bestTurn = priority;
                best = candidate;
            }
        }
        return best;
    }

    private static long point(int x, int y) {
        return (long) x << 32 | y & 0xFFFFFFFFL;
    }

    private static int manhattan(int first, int second, int width) {
        return Math.abs(first % width - second % width) + Math.abs(first / width - second / width);
    }

    private static int alpha(int color) {
        return color >>> 24;
    }

    private static long mix(long value) {
        value = (value ^ value >>> 30) * 0xbf58476d1ce4e5b9L;
        value = (value ^ value >>> 27) * 0x94d049bb133111ebL;
        return value ^ value >>> 31;
    }

    public record Layout(int width, int height, List<Piece> pieces) {
        public int pieceCount() {
            return pieces.size();
        }
    }

    public record Piece(List<Pixel> pixels, int minX, int minY, int maxX, int maxY) {
    }

    public record Pixel(int x, int y, int color) {
    }

    public record OutlinePixel(int x, int y) {
    }

    public record OutlineMask(List<OutlinePixel> pixels, int minX, int minY, int maxX, int maxY) {
        public int width() {
            return maxX - minX + 1;
        }

        public int height() {
            return maxY - minY + 1;
        }
    }

    private record ImagePixels(int width, int height, List<Integer> opaquePixels,
                               List<List<Integer>> components, Map<Integer, Integer> colors) {
    }

    private record Key(String relic, int frameHeight, long seed) {
    }

    private record BoundaryEdge(int x0, int y0, int x1, int y1) {
        private int direction() {
            if (x1 > x0) return 0;
            if (y1 > y0) return 1;
            if (x1 < x0) return 2;
            return 3;
        }

        private OutlinePixel outsidePixel() {
            return switch (direction()) {
                case 0 -> new OutlinePixel(x0, y0 - 1);
                case 1 -> new OutlinePixel(x0, y0);
                case 2 -> new OutlinePixel(x1, y0);
                default -> new OutlinePixel(x0 - 1, y1);
            };
        }
    }
}
