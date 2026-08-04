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
        return definition == null ? null : create(definition, seed);
    }

    private static Layout create(RelicFragmentDefinitions.Definition definition, long seed) {
        Key key = new Key(definition.relicId().toString(), seed);
        synchronized (CACHE) {
            Layout cached = CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }

        ImagePixels image = load(definition);
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

    private static ImagePixels load(RelicFragmentDefinitions.Definition definition) {
        var textureId = definition.textureId();
        String path = "/assets/" + textureId.getNamespace() + "/textures/" + textureId.getPath() + ".png";
        try (InputStream stream = RelicFragmenter.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing relic texture " + path);
            }
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                throw new IllegalStateException("Unable to read relic texture " + path);
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

    private record ImagePixels(int width, int height, List<Integer> opaquePixels,
                               List<List<Integer>> components, Map<Integer, Integer> colors) {
    }

    private record Key(String relic, long seed) {
    }
}
