package net.turtleboi.noblephantasms.screens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.screens.menus.custom.RelicForgeMenu;
import net.turtleboi.noblephantasms.network.RelicForgeCompletePayload;
import net.turtleboi.noblephantasms.relic.RelicFragmenter;

public final class RelicForgeScreen extends AbstractContainerScreen<RelicForgeMenu> {
    private static final long LOCK_MILLISECONDS_PER_PIXEL = 10L;
    private static final long LOCK_FLASH_DURATION = 90L;
    private static final long LOCK_FADE_DURATION = 260L;
    private static final int LOCK_GOLD = 0xFFFFC83D;
    private RelicFragmenter.Layout layout;
    private final List<PieceState> pieces = new ArrayList<>();
    private int scale;
    private int targetX;
    private int targetY;
    private PieceState dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private boolean completionSent;
    private Button forgeButton;
    private PieceState assembledState;
    private long assemblyAnimationStarted;

    public RelicForgeScreen(RelicForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 360, 240);
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        releaseOutlineTextures();
        layout = RelicFragmenter.create(menu.relicId(), menu.seed());
        scale = Math.max(1, Math.min(5, Math.min(130 / layout.width(), 150 / layout.height())));
        targetX = leftPos + (imageWidth - layout.width() * scale) / 2;
        targetY = topPos + 18 + (160 - layout.height() * scale) / 2;
        pieces.clear();
        Random random = new Random(menu.seed() ^ 0x72656c6963466f72L);
        for (RelicFragmenter.Piece piece : layout.pieces()) {
            int pieceWidth = (piece.maxX() - piece.minX() + 1) * scale;
            int pieceHeight = (piece.maxY() - piece.minY() + 1) * scale;
            int availableWidth = Math.max(1, 98 - pieceWidth);
            int availableHeight = Math.max(1, 190 - pieceHeight);
            int x = leftPos + 5 + random.nextInt(availableWidth);
            int y = topPos + 8 + random.nextInt(availableHeight);
            PieceState state = new PieceState(piece, x, y, 3 + random.nextInt(5));
            cacheOutlineTexture(state, pieces.size());
            pieces.add(state);
        }
        RelicFragmenter.Piece assembledPiece = createAssembledPiece(layout);
        assembledState = new PieceState(assembledPiece,
                targetX + assembledPiece.minX() * scale,
                targetY + assembledPiece.minY() * scale, 2);
        cacheOutlineTexture(assembledState, pieces.size());
        assemblyAnimationStarted = 0L;
        forgeButton = addRenderableWidget(Button.builder(
                Component.translatable("menu.noblephantasms.relic_forge.forge"), button -> forge())
                .bounds(leftPos + imageWidth / 2 - 40, topPos + imageHeight - 26, 80, 20)
                .build());
        forgeButton.active = false;
        forgeButton.visible = false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        for (RelicFragmenter.Piece piece : layout.pieces()) {
            for (RelicFragmenter.Pixel pixel : piece.pixels()) {
                int x = targetX + pixel.x() * scale;
                int y = targetY + pixel.y() * scale;
                graphics.fill(x, y, x + scale, y + scale, 0x28000000 | pixel.color() & 0x00FFFFFF);
            }
        }
        Map<Integer, List<PieceState>> heightLayers = new TreeMap<>();
        for (PieceState piece : pieces) {
            heightLayers.computeIfAbsent(piece.height, ignored -> new ArrayList<>()).add(piece);
        }
        for (List<PieceState> layer : heightLayers.values()) {
            for (PieceState piece : layer) {
                drawShadow(graphics, piece);
            }
            for (PieceState piece : layer) {
                drawPiece(graphics, piece);
            }
        }
        for (PieceState piece : pieces) {
            drawLockAnimation(graphics, piece);
        }
        drawAssemblyAnimation(graphics);
    }

    private void drawShadow(GuiGraphicsExtractor graphics, PieceState state) {
        int offsetX = Math.max(1, state.height / 2);
        int offsetY = Math.max(1, state.height);
        int alpha = Math.min(128, 40 + state.height * 7);
        for (RelicFragmenter.Pixel pixel : state.piece.pixels()) {
            int sourceAlpha = pixel.color() >>> 24;
            int color = sourceAlpha * alpha / 255 << 24;
            int x = state.x + (pixel.x() - state.piece.minX()) * scale + offsetX;
            int y = state.y + (pixel.y() - state.piece.minY()) * scale + offsetY;
            graphics.fill(x, y, x + scale, y + scale, color);
        }
    }

    private void drawPiece(GuiGraphicsExtractor graphics, PieceState state) {
        for (RelicFragmenter.Pixel pixel : state.piece.pixels()) {
            int x = state.x + (pixel.x() - state.piece.minX()) * scale;
            int y = state.y + (pixel.y() - state.piece.minY()) * scale;
            graphics.fill(x, y, x + scale, y + scale, pixel.color());
        }
    }

    private void drawLockAnimation(GuiGraphicsExtractor graphics, PieceState state) {
        if (state.lockAnimationStarted == 0L) {
            return;
        }
        long elapsed = Util.getMillis() - state.lockAnimationStarted;
        if (elapsed < 0L) {
            return;
        }
        long traceDuration = traceDuration(state);
        long flashEnd = traceDuration + LOCK_FLASH_DURATION;
        long animationEnd = flashEnd + LOCK_FADE_DURATION;
        if (elapsed >= animationEnd) {
            state.lockAnimationStarted = 0L;
            return;
        }
        if (elapsed < traceDuration) {
            double progress = elapsed / (double) traceDuration;
            drawOutline(graphics, state, progress, LOCK_GOLD);
            return;
        }
        if (elapsed < flashEnd) {
            float progress = (elapsed - traceDuration) / (float) LOCK_FLASH_DURATION;
            drawCachedOutline(graphics, state, ARGB.srgbLerp(progress, LOCK_GOLD, 0xFFFFFFFF));
            return;
        }
        float fade = 1.0F - (elapsed - flashEnd) / (float) LOCK_FADE_DURATION;
        drawCachedOutline(graphics, state, ARGB.color(Math.round(255.0F * fade), 0xFFFFFF));
    }

    private static long traceDuration(PieceState state) {
        return Math.max(1L, state.tracePixels.size() * LOCK_MILLISECONDS_PER_PIXEL);
    }

    private static long animationDuration(PieceState state) {
        return traceDuration(state) + LOCK_FLASH_DURATION + LOCK_FADE_DURATION;
    }

    private void drawAssemblyAnimation(GuiGraphicsExtractor graphics) {
        if (assemblyAnimationStarted == 0L || assembledState == null) {
            return;
        }
        long now = Util.getMillis();
        drawLockAnimation(graphics, assembledState);
        if (now >= assemblyAnimationStarted + animationDuration(assembledState)) {
            assemblyAnimationStarted = 0L;
            assembledState.lockAnimationStarted = 0L;
            forgeButton.visible = true;
            forgeButton.active = true;
        }
    }

    private void drawOutline(GuiGraphicsExtractor graphics, PieceState state, double progress, int color) {
        int visiblePixels = Math.min(state.tracePixels.size(),
                (int) Math.ceil(state.tracePixels.size() * progress));
        for (int index = 0; index < visiblePixels; index++) {
            OutlinePixel pixel = state.tracePixels.get(index);
            int x = state.x + (pixel.x - state.piece.minX()) * scale;
            int y = state.y + (pixel.y - state.piece.minY()) * scale;
            graphics.fill(x, y, x + scale, y + scale, color);
        }
    }

    private void drawCachedOutline(GuiGraphicsExtractor graphics, PieceState state, int color) {
        if (state.outlineTexture == null) {
            return;
        }
        int x = state.x + (state.outline.minX - state.piece.minX()) * scale;
        int y = state.y + (state.outline.minY - state.piece.minY()) * scale;
        graphics.blit(RenderPipelines.GUI_TEXTURED, state.outlineTexture, x, y, 0.0F, 0.0F,
                state.outline.width() * scale, state.outline.height() * scale,
                state.outline.width(), state.outline.height(),
                state.outline.width(), state.outline.height(), color);
    }

    private void cacheOutlineTexture(PieceState state, int index) {
        NativeImage image = new NativeImage(state.outline.width(), state.outline.height(), true);
        for (OutlinePixel pixel : state.outline.pixels) {
            image.setPixel(pixel.x - state.outline.minX, pixel.y - state.outline.minY, 0xFFFFFFFF);
        }
        state.outlineTexture = Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID,
                "dynamic/relic_outline/" + menu.containerId + "_" + Long.toUnsignedString(menu.seed()) + "_" + index);
        minecraft.getTextureManager().register(state.outlineTexture,
                new DynamicTexture(() -> "Relic fragment outline", image));
    }

    private void releaseOutlineTextures() {
        if (minecraft == null) {
            return;
        }
        for (PieceState state : pieces) {
            if (state.outlineTexture != null) {
                minecraft.getTextureManager().release(state.outlineTexture);
                state.outlineTexture = null;
            }
        }
        if (assembledState != null && assembledState.outlineTexture != null) {
            minecraft.getTextureManager().release(assembledState.outlineTexture);
            assembledState.outlineTexture = null;
        }
    }

    @Override
    public void removed() {
        releaseOutlineTextures();
        super.removed();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            PieceState selected = null;
            for (int index = pieces.size() - 1; index >= 0; index--) {
                PieceState piece = pieces.get(index);
                if (!piece.locked && contains(piece, event.x(), event.y())
                        && (selected == null || piece.height > selected.height)) {
                    selected = piece;
                }
            }
            if (selected != null) {
                dragging = selected;
                dragging.height = 10;
                dragOffsetX = (int) event.x() - selected.x;
                dragOffsetY = (int) event.y() - selected.y;
                pieces.remove(selected);
                pieces.add(selected);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && dragging != null) {
            dragging.x = (int) event.x() - dragOffsetX;
            dragging.y = (int) event.y() - dragOffsetY;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && dragging != null) {
            int correctX = targetX + dragging.piece.minX() * scale;
            int correctY = targetY + dragging.piece.minY() * scale;
            int distanceX = dragging.x - correctX;
            int distanceY = dragging.y - correctY;
            if (distanceX * distanceX + distanceY * distanceY <= 100) {
                long now = Util.getMillis();
                dragging.x = correctX;
                dragging.y = correctY;
                dragging.locked = true;
                dragging.height = 2;
                boolean completed = pieces.stream().allMatch(piece -> piece.locked);
                if (completed) {
                    dragging.lockAnimationStarted = 0L;
                    assembledState.tracePixels = startOutlineNear(
                            assembledState.outline.pixels, dragging.piece);
                    assemblyAnimationStarted = now;
                    assembledState.lockAnimationStarted = now;
                    forgeButton.visible = false;
                    forgeButton.active = false;
                } else {
                    dragging.lockAnimationStarted = now;
                }
            } else {
                dragging.height = 5;
            }
            dragging = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    private boolean contains(PieceState state, double mouseX, double mouseY) {
        for (RelicFragmenter.Pixel pixel : state.piece.pixels()) {
            int x = state.x + (pixel.x() - state.piece.minX()) * scale;
            int y = state.y + (pixel.y() - state.piece.minY()) * scale;
            if (mouseX >= x && mouseX < x + scale && mouseY >= y && mouseY < y + scale) {
                return true;
            }
        }
        return false;
    }

    private void forge() {
        if (!completionSent && pieces.stream().allMatch(piece -> piece.locked)) {
            completionSent = true;
            forgeButton.active = false;
            ClientPacketDistributor.sendToServer(new RelicForgeCompletePayload(menu.containerId, menu.seed()));
        }
    }

    private static final class PieceState {
        private final RelicFragmenter.Piece piece;
        private final OutlineMask outline;
        private List<OutlinePixel> tracePixels;
        private Identifier outlineTexture;
        private int x;
        private int y;
        private int height;
        private long lockAnimationStarted;
        private boolean locked;

        private PieceState(RelicFragmenter.Piece piece, int x, int y, int height) {
            this.piece = piece;
            this.outline = createOutlineMask(piece);
            this.tracePixels = outline.pixels;
            this.x = x;
            this.y = y;
            this.height = height;
        }
    }

    private static RelicFragmenter.Piece createAssembledPiece(RelicFragmenter.Layout layout) {
        List<RelicFragmenter.Pixel> pixels = layout.pieces().stream()
                .flatMap(piece -> piece.pixels().stream())
                .toList();
        int minX = pixels.stream().mapToInt(RelicFragmenter.Pixel::x).min().orElse(0);
        int minY = pixels.stream().mapToInt(RelicFragmenter.Pixel::y).min().orElse(0);
        int maxX = pixels.stream().mapToInt(RelicFragmenter.Pixel::x).max().orElse(0);
        int maxY = pixels.stream().mapToInt(RelicFragmenter.Pixel::y).max().orElse(0);
        return new RelicFragmenter.Piece(pixels, minX, minY, maxX, maxY);
    }

    private static List<OutlinePixel> startOutlineNear(List<OutlinePixel> outline,
                                                       RelicFragmenter.Piece piece) {
        if (outline.isEmpty()) {
            return outline;
        }
        int nearestIndex = 0;
        int nearestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < outline.size(); index++) {
            OutlinePixel outlinePixel = outline.get(index);
            for (RelicFragmenter.Pixel piecePixel : piece.pixels()) {
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

    private static OutlineMask createOutlineMask(RelicFragmenter.Piece piece) {
        Set<Long> occupied = new HashSet<>();
        for (RelicFragmenter.Pixel pixel : piece.pixels()) {
            occupied.add(point(pixel.x(), pixel.y()));
        }
        List<BoundaryEdge> edges = new ArrayList<>();
        for (RelicFragmenter.Pixel pixel : piece.pixels()) {
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

    private record OutlinePixel(int x, int y) {
    }

    private record OutlineMask(List<OutlinePixel> pixels, int minX, int minY, int maxX, int maxY) {
        private int width() {
            return maxX - minX + 1;
        }

        private int height() {
            return maxY - minY + 1;
        }
    }
}
