package net.turtleboi.noblephantasms.screens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
import net.turtleboi.noblephantasms.screens.menus.custom.ReliquaryStationMenu;
import net.turtleboi.noblephantasms.network.ReliquaryStationCompletePayload;
import net.turtleboi.noblephantasms.relic.RelicFragmenter;

public final class ReliquaryStationScreen extends AbstractContainerScreen<ReliquaryStationMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/ui/reliquary_station.png");
    private static final int BACKGROUND_WIDTH = 214;
    private static final int BACKGROUND_HEIGHT = 207;
    private static final int RELIC_CENTER_X = 139;
    private static final int RELIC_CENTER_Y = 55;
    private static final int RELIC_AREA_WIDTH = 126;
    private static final int RELIC_AREA_HEIGHT = 90;
    private static final int FRAGMENT_TRAY_X = 8;
    private static final int FRAGMENT_TRAY_Y = 4;
    private static final int FRAGMENT_TRAY_WIDTH = 54;
    private static final int FRAGMENT_TRAY_HEIGHT = 102;
    private static final float RELIC_SCALE_FACTOR = 0.75F;
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

    public ReliquaryStationScreen(ReliquaryStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        int previousLeft = leftPos;
        int previousTop = topPos;
        boolean preservePuzzle = layout != null && assembledState != null;
        super.init();
        if (preservePuzzle) {
            movePuzzle(leftPos - previousLeft, topPos - previousTop);
        } else {
            initializePuzzle();
        }
        createForgeButton();
    }

    private void initializePuzzle() {
        layout = RelicFragmenter.createForStation(menu.relicId(), menu.seed());
        int largestPieceWidth = layout.pieces().stream()
                .mapToInt(piece -> piece.maxX() - piece.minX() + 1)
                .max()
                .orElse(1);
        int largestPieceHeight = layout.pieces().stream()
                .mapToInt(piece -> piece.maxY() - piece.minY() + 1)
                .max()
                .orElse(1);
        int fittedScale = Math.max(1, Math.min(5, Math.min(
                Math.min(RELIC_AREA_WIDTH / layout.width(), RELIC_AREA_HEIGHT / layout.height()),
                Math.min(FRAGMENT_TRAY_WIDTH / largestPieceWidth,
                        FRAGMENT_TRAY_HEIGHT / largestPieceHeight))));
        scale = Math.max(1, Math.round(fittedScale * RELIC_SCALE_FACTOR));
        targetX = leftPos + RELIC_CENTER_X - scaledOffset(layout.width()) / 2;
        targetY = topPos + RELIC_CENTER_Y - scaledOffset(layout.height()) / 2;
        pieces.clear();
        Random random = new Random(menu.seed() ^ 0x72656c6963466f72L);
        List<RelicFragmenter.Piece> distributedPieces = new ArrayList<>(layout.pieces());
        Collections.shuffle(distributedPieces, random);
        for (int index = 0; index < distributedPieces.size(); index++) {
            RelicFragmenter.Piece piece = distributedPieces.get(index);
            int pieceWidth = scaledOffset(piece.maxX() - piece.minX() + 1);
            int pieceHeight = scaledOffset(piece.maxY() - piece.minY() + 1);
            int availableWidth = Math.max(1, FRAGMENT_TRAY_WIDTH - pieceWidth + 1);
            int availableHeight = Math.max(1, FRAGMENT_TRAY_HEIGHT - pieceHeight + 1);
            int x = leftPos + FRAGMENT_TRAY_X + random.nextInt(availableWidth);
            int yOffset = distributedPieces.size() == 1
                    ? (availableHeight - 1) / 2
                    : Math.round(index * (availableHeight - 1.0F) / (distributedPieces.size() - 1));
            int y = topPos + FRAGMENT_TRAY_Y + yOffset;
            PieceState state = new PieceState(piece, x, y, 3 + random.nextInt(5));
            cacheOutlineTexture(state, pieces.size());
            pieces.add(state);
        }
        RelicFragmenter.Piece assembledPiece = RelicFragmenter.assemble(layout);
        assembledState = new PieceState(assembledPiece,
                targetX + scaledOffset(assembledPiece.minX()),
                targetY + scaledOffset(assembledPiece.minY()), 2);
        cacheOutlineTexture(assembledState, pieces.size());
        assemblyAnimationStarted = 0L;
    }

    private void movePuzzle(int offsetX, int offsetY) {
        targetX += offsetX;
        targetY += offsetY;
        for (PieceState piece : pieces) {
            piece.x += offsetX;
            piece.y += offsetY;
        }
        assembledState.x += offsetX;
        assembledState.y += offsetY;
        if (dragging != null) {
            dragging.height = 5;
            dragging = null;
        }
    }

    private void createForgeButton() {
        forgeButton = addRenderableWidget(Button.builder(
                Component.translatable("menu.noblephantasms.reliquary_station.forge"), button -> forge())
                .bounds(leftPos + 166, topPos + 88, 42, 18)
                .build());
        boolean ready = !pieces.isEmpty()
                && pieces.stream().allMatch(piece -> piece.locked)
                && assemblyAnimationStarted == 0L;
        forgeButton.active = ready && !completionSent;
        forgeButton.visible = ready;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
                leftPos, topPos, 0, 0,
                BACKGROUND_WIDTH, BACKGROUND_HEIGHT, 256, 256);
        for (RelicFragmenter.Piece piece : layout.pieces()) {
            for (RelicFragmenter.Pixel pixel : piece.pixels()) {
                fillScaledPixel(graphics, targetX, targetY, pixel.x(), pixel.y(),
                        0x28000000 | pixel.color() & 0x00FFFFFF);
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
        int offsetX = Math.max(1, Math.round(state.height * scale / 8.0F));
        int offsetY = Math.max(1, Math.round(state.height * scale / 4.0F));
        int alpha = Math.min(128, 40 + state.height * 7);
        for (RelicFragmenter.Pixel pixel : state.piece.pixels()) {
            int sourceAlpha = pixel.color() >>> 24;
            int color = sourceAlpha * alpha / 255 << 24;
            fillScaledPixel(graphics, state.x + offsetX, state.y + offsetY,
                    pixel.x() - state.piece.minX(), pixel.y() - state.piece.minY(), color);
        }
    }

    private void drawPiece(GuiGraphicsExtractor graphics, PieceState state) {
        for (RelicFragmenter.Pixel pixel : state.piece.pixels()) {
            fillScaledPixel(graphics, state.x, state.y,
                    pixel.x() - state.piece.minX(), pixel.y() - state.piece.minY(), pixel.color());
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
            RelicFragmenter.OutlinePixel pixel = state.tracePixels.get(index);
            fillScaledPixel(graphics, state.x, state.y,
                    pixel.x() - state.piece.minX(), pixel.y() - state.piece.minY(), color);
        }
    }

    private void drawCachedOutline(GuiGraphicsExtractor graphics, PieceState state, int color) {
        if (state.outlineTexture == null) {
            return;
        }
        int sourceX = state.outline.minX() - state.piece.minX();
        int sourceY = state.outline.minY() - state.piece.minY();
        int x = state.x + scaledOffset(sourceX);
        int y = state.y + scaledOffset(sourceY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, state.outlineTexture, x, y, 0.0F, 0.0F,
                scaledSpan(sourceX, state.outline.width()), scaledSpan(sourceY, state.outline.height()),
                state.outline.width(), state.outline.height(),
                state.outline.width(), state.outline.height(), color);
    }

    private void cacheOutlineTexture(PieceState state, int index) {
        NativeImage image = new NativeImage(state.outline.width(), state.outline.height(), true);
        for (RelicFragmenter.OutlinePixel pixel : state.outline.pixels()) {
            image.setPixel(pixel.x() - state.outline.minX(), pixel.y() - state.outline.minY(), 0xFFFFFFFF);
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
            int correctX = targetX + scaledOffset(dragging.piece.minX());
            int correctY = targetY + scaledOffset(dragging.piece.minY());
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
                    assembledState.tracePixels = RelicFragmenter.startOutlineNear(
                            assembledState.outline.pixels(), dragging.piece);
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
            int relativeX = pixel.x() - state.piece.minX();
            int relativeY = pixel.y() - state.piece.minY();
            int x = state.x + scaledOffset(relativeX);
            int y = state.y + scaledOffset(relativeY);
            int right = state.x + scaledOffset(relativeX + 1);
            int bottom = state.y + scaledOffset(relativeY + 1);
            if (mouseX >= x && mouseX < right && mouseY >= y && mouseY < bottom) {
                return true;
            }
        }
        return false;
    }

    private void forge() {
        if (!completionSent && pieces.stream().allMatch(piece -> piece.locked)) {
            completionSent = true;
            forgeButton.active = false;
            ClientPacketDistributor.sendToServer(new ReliquaryStationCompletePayload(menu.containerId, menu.seed()));
        }
    }

    private int scaledOffset(int sourcePixels) {
        return sourcePixels * scale;
    }

    private int scaledSpan(int sourceStart, int sourceLength) {
        return scaledOffset(sourceStart + sourceLength) - scaledOffset(sourceStart);
    }

    private void fillScaledPixel(GuiGraphicsExtractor graphics, int originX, int originY,
                                 int sourceX, int sourceY, int color) {
        graphics.fill(originX + scaledOffset(sourceX), originY + scaledOffset(sourceY),
                originX + scaledOffset(sourceX + 1), originY + scaledOffset(sourceY + 1), color);
    }

    private static final class PieceState {
        private final RelicFragmenter.Piece piece;
        private final RelicFragmenter.OutlineMask outline;
        private List<RelicFragmenter.OutlinePixel> tracePixels;
        private Identifier outlineTexture;
        private int x;
        private int y;
        private int height;
        private long lockAnimationStarted;
        private boolean locked;

        private PieceState(RelicFragmenter.Piece piece, int x, int y, int height) {
            this.piece = piece;
            this.outline = RelicFragmenter.createOutline(piece);
            this.tracePixels = outline.pixels();
            this.x = x;
            this.y = y;
            this.height = height;
        }
    }

}
