package net.turtleboi.noblephantasms.screens;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.screens.menus.custom.ReliquaryStationMenu;
import net.turtleboi.noblephantasms.network.ReliquaryStationCompletePayload;
import net.turtleboi.noblephantasms.network.ReliquaryStationForgeFinishPayload;
import net.turtleboi.noblephantasms.network.MythicalReliquarySelectPayload;
import net.turtleboi.noblephantasms.relic.RelicFragmentArchive;
import net.turtleboi.noblephantasms.relic.RelicFragmentDefinitions;
import net.turtleboi.noblephantasms.relic.RelicFragmenter;

public final class ReliquaryStationScreen extends AbstractContainerScreen<ReliquaryStationMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/ui/reliquary_station.png");
    private static final Identifier BOOK_TEXTURE = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/item/mythic_reliquary.png");
    private static final Identifier HAMMER_TEXTURE = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/item/forge_hammer.png");
    private static final int BACKGROUND_WIDTH = 214;
    private static final int BACKGROUND_HEIGHT = 231;
    private static final int CONTENT_Y = 24;
    private static final int RELIC_CENTER_X = 139;
    private static final int RELIC_CENTER_Y = 55 + CONTENT_Y;
    private static final int RELIC_AREA_WIDTH = 126;
    private static final int RELIC_AREA_HEIGHT = 90;
    private static final int FRAGMENT_TRAY_X = 8;
    private static final int FRAGMENT_TRAY_Y = 4 + CONTENT_Y;
    private static final int FRAGMENT_TRAY_WIDTH = 54;
    private static final int FRAGMENT_TRAY_HEIGHT = 64;
    private static final int BOOK_X = 189;
    private static final int BOOK_Y = 18;
    private static final int HAMMER_X = 193;
    private static final int HAMMER_Y = 112;
    private static final int CONTROL_SIZE = 32;
    private static final int CAROUSEL_X = 4;
    private static final int CAROUSEL_Y = 4;
    private static final int CAROUSEL_WIDTH = 192;
    private static final int CAROUSEL_HEIGHT = 16;
    private static final int CAROUSEL_ARROW_WIDTH = 8;
    private static final int CAROUSEL_ARROW_HEIGHT = 10;
    private static final int CAROUSEL_EDGE_RESERVE = 12;
    private static final int CAROUSEL_CELL_WIDTH = 21;
    private static final int CAROUSEL_ITEM_OFFSET = 2;
    private static final int LIGHT_SEPIA = 0xFFDED1BA;
    private static final int MID_SEPIA = 0xFFAC9D87;
    private static final int DARK_SEPIA = 0xFF776853;
    private static final float RELIC_SCALE_FACTOR = 0.75F;
    private static final long LOCK_MILLISECONDS_PER_PIXEL = 10L;
    private static final long LOCK_FLASH_DURATION = 90L;
    private static final long LOCK_FADE_DURATION = 260L;
    private static final long FORGE_SHRINK_DURATION = 450L;
    private static final long FORGE_DUST_DURATION = 360L;
    private static final long FORGE_FLIGHT_DURATION = 600L;
    private static final long FORGE_NO_SLOT_HOLD_DURATION = 180L;
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
    private PieceState assembledState;
    private long assemblyAnimationStarted;
    private Identifier renderedRelic;
    private long renderedSeed;
    private int renderedMask = -1;
    private long ejectionAnimationStarted;
    private boolean relicPickerOpen;
    private int relicPickerOffset;
    private int selectionOriginX;
    private int selectionOriginY;
    private Identifier bookOutlineTexture;
    private Identifier hammerOutlineTexture;
    private final Map<Identifier, BarTexture> relicBarTextures = new HashMap<>();
    private final Map<Identifier, BarTexture> sepiaRelicBarTextures = new HashMap<>();
    private boolean leftArrowPressed;
    private boolean rightArrowPressed;
    private ForgeAnimation forgeAnimation;

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
        cacheControlOutlineTextures();
        if (preservePuzzle) {
            movePuzzle(leftPos - previousLeft, topPos - previousTop);
        } else {
            initializePuzzle();
        }
    }

    private void initializePuzzle() {
        releaseOutlineTextures();
        RelicFragmentArchive.RelicSet set = menu.selectedSet();
        renderedRelic = menu.relicId();
        renderedSeed = set == null ? 0L : set.seed();
        renderedMask = set == null ? 0 : set.discoveredMask();
        pieces.clear();
        assembledState = null;
        assemblyAnimationStarted = 0L;
        dragging = null;
        if (renderedRelic == null || set == null) {
            layout = renderedRelic == null ? null
                    : RelicFragmenter.createForStation(renderedRelic, 0L);
        } else {
            layout = RelicFragmenter.createForStation(renderedRelic, set.seed(), set.pieceCount());
        }
        if (layout == null) {
            selectionOriginX = 0;
            selectionOriginY = 0;
            return;
        }
        scale = stationScale(layout);
        targetX = leftPos + RELIC_CENTER_X - scaledOffset(layout.width()) / 2;
        targetY = topPos + RELIC_CENTER_Y - scaledOffset(layout.height()) / 2;
        Random random = new Random(renderedSeed ^ 0x72656c6963466f72L);
        List<RelicFragmenter.Piece> distributedPieces = new ArrayList<>();
        if (set != null) {
            for (int index = 0; index < layout.pieces().size(); index++) {
                if ((set.discoveredMask() & 1 << index) != 0) {
                    distributedPieces.add(layout.pieces().get(index));
                }
            }
        }
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
            PieceState state = new PieceState(piece,
                    selectionOriginX == 0 ? leftPos + FRAGMENT_TRAY_X + FRAGMENT_TRAY_WIDTH / 2 : selectionOriginX,
                    selectionOriginY == 0 ? topPos + FRAGMENT_TRAY_Y + FRAGMENT_TRAY_HEIGHT / 2 : selectionOriginY,
                    3 + random.nextInt(5));
            state.restX = x;
            state.restY = y;
            cacheOutlineTexture(state, pieces.size());
            pieces.add(state);
        }
        RelicFragmenter.Piece assembledPiece = RelicFragmenter.assemble(layout);
        assembledState = new PieceState(assembledPiece,
                targetX + scaledOffset(assembledPiece.minX()),
                targetY + scaledOffset(assembledPiece.minY()), 2);
        cacheOutlineTexture(assembledState, pieces.size());
        ejectionAnimationStarted = Util.getMillis();
        completionSent = false;
        selectionOriginX = 0;
        selectionOriginY = 0;
    }

    private int stationScale(RelicFragmenter.Layout sourceLayout) {
        int largestPieceWidth = sourceLayout.pieces().stream()
                .mapToInt(piece -> piece.maxX() - piece.minX() + 1)
                .max()
                .orElse(1);
        int largestPieceHeight = sourceLayout.pieces().stream()
                .mapToInt(piece -> piece.maxY() - piece.minY() + 1)
                .max()
                .orElse(1);
        int fittedScale = Math.max(1, Math.min(5, Math.min(
                Math.min(RELIC_AREA_WIDTH / sourceLayout.width(),
                        RELIC_AREA_HEIGHT / sourceLayout.height()),
                Math.min(FRAGMENT_TRAY_WIDTH / largestPieceWidth,
                        FRAGMENT_TRAY_HEIGHT / largestPieceHeight))));
        return Math.max(1, Math.round(fittedScale * RELIC_SCALE_FACTOR));
    }

    private void movePuzzle(int offsetX, int offsetY) {
        targetX += offsetX;
        targetY += offsetY;
        for (PieceState piece : pieces) {
            piece.x += offsetX;
            piece.y += offsetY;
            piece.spawnX += offsetX;
            piece.spawnY += offsetY;
            piece.restX += offsetX;
            piece.restY += offsetY;
        }
        if (assembledState != null) {
            assembledState.x += offsetX;
            assembledState.y += offsetY;
        }
        if (forgeAnimation != null) {
            forgeAnimation.sourceX += offsetX;
            forgeAnimation.sourceY += offsetY;
        }
        if (dragging != null) {
            dragging.height = 5;
            dragging = null;
        }
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
        refreshPuzzleIfNeeded();
        updateEjection();
        drawRelicPicker(graphics, mouseX, mouseY);
        if (forgeAnimation != null) {
            drawForgeAnimation(graphics);
        } else if (layout != null) {
            for (RelicFragmenter.Piece piece : layout.pieces()) {
                for (RelicFragmenter.Pixel pixel : piece.pixels()) {
                    fillScaledPixel(graphics, targetX, targetY, pixel.x(), pixel.y(),
                            0x50000000 | pixel.color() & 0x00FFFFFF);
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
        drawBookControl(graphics, mouseX, mouseY);
        if (forgeAnimation == null) {
            drawHammerControl(graphics, mouseX, mouseY);
        }
    }

    public void beginForgeAnimation(Identifier relicId, long seed,
                                    int targetMenuSlot, int pieceCount) {
        RelicFragmenter.Layout animationLayout = layout != null
                && relicId.equals(renderedRelic) && renderedSeed == seed
                && layout.pieces().size() == pieceCount ? layout
                : RelicFragmenter.createForStation(relicId, seed, pieceCount);
        if (animationLayout == null) {
            ClientPacketDistributor.sendToServer(new ReliquaryStationForgeFinishPayload(
                    menu.containerId, relicId, seed));
            return;
        }
        int animationScale = layout == animationLayout ? scale : stationScale(animationLayout);
        int animationTargetX = leftPos + RELIC_CENTER_X
                - animationLayout.width() * animationScale / 2;
        int animationTargetY = topPos + RELIC_CENTER_Y
                - animationLayout.height() * animationScale / 2;
        RelicFragmenter.Piece display = RelicFragmenter.assemble(animationLayout);
        forgeAnimation = new ForgeAnimation(relicId, seed, targetMenuSlot, display,
                animationTargetX + display.minX() * animationScale,
                animationTargetY + display.minY() * animationScale,
                animationScale, Util.getMillis());
        completionSent = true;
        dragging = null;
        relicPickerOpen = false;
        ejectionAnimationStarted = 0L;
    }

    private void refreshPuzzleIfNeeded() {
        RelicFragmentArchive.RelicSet set = menu.selectedSet();
        Identifier relicId = menu.relicId();
        long seed = set == null ? 0L : set.seed();
        int mask = set == null ? 0 : set.discoveredMask();
        if (!java.util.Objects.equals(renderedRelic, relicId)
                || renderedSeed != seed || renderedMask != mask) {
            initializePuzzle();
        }
    }

    private void updateEjection() {
        if (ejectionAnimationStarted == 0L) {
            return;
        }
        long elapsedTotal = Util.getMillis() - ejectionAnimationStarted;
        boolean running = false;
        for (int index = 0; index < pieces.size(); index++) {
            PieceState piece = pieces.get(index);
            float progress = Math.clamp((elapsedTotal - index * 35L) / 360.0F, 0.0F, 1.0F);
            float eased = 1.0F - (float) Math.pow(1.0F - progress, 4.0);
            piece.x = Math.round(piece.spawnX + (piece.restX - piece.spawnX) * eased);
            piece.y = Math.round(piece.spawnY + (piece.restY - piece.spawnY) * eased);
            running |= progress < 1.0F;
        }
        if (!running) {
            ejectionAnimationStarted = 0L;
        }
    }

    private void drawBookControl(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = leftPos + BOOK_X;
        int y = topPos + BOOK_Y;
        boolean hasBook = !menu.reliquary().isEmpty();
        boolean hovered = isInside(mouseX, mouseY, x, y, CONTROL_SIZE, CONTROL_SIZE);
        int color = hasBook ? 0xFFFFFFFF : 0x60776853;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_TEXTURE, x, y, 0.0F, 0.0F,
                CONTROL_SIZE, CONTROL_SIZE, 16, 16, 16, 16, color);
        if (hasBook && hovered && bookOutlineTexture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, bookOutlineTexture,
                    x - 1, y - 1, 0.0F, 0.0F, CONTROL_SIZE + 2, CONTROL_SIZE + 2,
                    34, 34, 34, 34, LOCK_GOLD);
            graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_TEXTURE, x, y, 0.0F, 0.0F,
                    CONTROL_SIZE, CONTROL_SIZE, 16, 16, 16, 16, color);
        }
        if (hovered) {
            Component tooltip = Component.translatable(hasBook
                    ? "menu.noblephantasms.reliquary_station.choose_relic"
                    : "menu.noblephantasms.reliquary_station.book_missing");
            graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    private void drawForgeAnimation(GuiGraphicsExtractor graphics) {
        ForgeAnimation animation = forgeAnimation;
        if (animation == null) {
            return;
        }
        long elapsed = Util.getMillis() - animation.started;
        float centerX = animation.sourceX + animation.width() * 0.5F;
        float centerY = animation.sourceY + animation.height() * 0.5F;
        float spriteX = centerX;
        float spriteY = centerY;
        float finalDisplayScale = Math.min(1.0F,
                16.0F / Math.max(animation.width(), animation.height()));
        if (elapsed < FORGE_SHRINK_DURATION) {
            float progress = smoothStep(elapsed / (float) FORGE_SHRINK_DURATION);
            drawForgeDisplay(graphics, animation,
                    1.0F + (finalDisplayScale - 1.0F) * progress);
            return;
        }

        long dustElapsed = elapsed - FORGE_SHRINK_DURATION;
        if (dustElapsed < FORGE_DUST_DURATION) {
            if (dustElapsed >= FORGE_DUST_DURATION / 2) {
                drawForgeSprite(graphics, animation, spriteX, spriteY);
            } else {
                drawForgeDisplay(graphics, animation, finalDisplayScale);
            }
            drawForgeDust(graphics, animation, centerX, centerY,
                    dustElapsed / (float) FORGE_DUST_DURATION);
            return;
        }

        long travelElapsed = dustElapsed - FORGE_DUST_DURATION;
        if (animation.targetMenuSlot >= 0 && animation.targetMenuSlot < menu.slots.size()) {
            float progress = smoothStep(travelElapsed / (float) FORGE_FLIGHT_DURATION);
            SlotTarget target = slotTarget(animation.targetMenuSlot);
            float travelX = centerX + (target.x() - centerX) * progress;
            float travelY = centerY + (target.y() - centerY) * progress
                    - (float) Math.sin(progress * Math.PI) * 12.0F;
            drawForgeSprite(graphics, animation, travelX, travelY);
            if (travelElapsed >= FORGE_FLIGHT_DURATION) {
                finishForgeAnimation(animation);
            }
        } else {
            drawForgeSprite(graphics, animation, centerX, centerY);
            if (travelElapsed >= FORGE_NO_SLOT_HOLD_DURATION) {
                finishForgeAnimation(animation);
            }
        }
    }

    private void drawForgeDisplay(GuiGraphicsExtractor graphics,
                                  ForgeAnimation animation, float animationScale) {
        float centerX = animation.sourceX + animation.width() * 0.5F;
        float centerY = animation.sourceY + animation.height() * 0.5F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(animationScale, animationScale);
        graphics.pose().translate(-centerX, -centerY);
        for (RelicFragmenter.Pixel pixel : animation.display.pixels()) {
            int relativeX = pixel.x() - animation.display.minX();
            int relativeY = pixel.y() - animation.display.minY();
            int x = animation.sourceX + relativeX * animation.sourceScale;
            int y = animation.sourceY + relativeY * animation.sourceScale;
            graphics.fill(x, y, x + animation.sourceScale, y + animation.sourceScale,
                    pixel.color());
        }
        graphics.pose().popMatrix();
    }

    private void drawForgeSprite(GuiGraphicsExtractor graphics,
                                 ForgeAnimation animation, float centerX, float centerY) {
        RelicFragmentDefinitions.Definition definition = RelicFragmentDefinitions.get(
                animation.relicId);
        ItemStack stack = definition == null ? ItemStack.EMPTY
                : new ItemStack(definition.relic().get());
        int x = Math.round(centerX) - 8;
        int y = Math.round(centerY) - 8;
        BarTexture texture = definition == null ? null
                : getRelicBarTexture(definition.inventoryTextureId(), false);
        if (texture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture.id(), x, y, 0.0F, 0.0F,
                    16, 16, texture.width(), texture.frameHeight(),
                    texture.width(), texture.height());
        } else if (!stack.isEmpty()) {
            graphics.item(stack, x, y);
        }
    }

    private void drawForgeDust(GuiGraphicsExtractor graphics, ForgeAnimation animation,
                               float centerX, float centerY, float progress) {
        Random random = new Random(animation.seed ^ 0x4465736572744475L);
        float eased = smoothStep(progress);
        for (int index = 0; index < 36; index++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            float startRadius = random.nextFloat() * 8.0F;
            float radius = startRadius + eased * (5.0F + random.nextFloat() * 11.0F);
            int size = 3 + random.nextInt(4);
            int x = Math.round(centerX + (float) Math.cos(angle) * radius) - size / 2;
            int y = Math.round(centerY + (float) Math.sin(angle) * radius * 0.7F) - size / 2;
            int alpha = Math.clamp(Math.round((1.0F - eased) *
                    (170.0F + random.nextFloat() * 85.0F)), 0, 255);
            int base = switch (index % 3) {
                case 0 -> LIGHT_SEPIA;
                case 1 -> MID_SEPIA;
                default -> DARK_SEPIA;
            };
            graphics.fill(x, y, x + size, y + size,
                    ARGB.color(alpha, ARGB.red(base), ARGB.green(base), ARGB.blue(base)));
        }
    }

    private SlotTarget slotTarget(int menuSlot) {
        return new SlotTarget(leftPos + menu.slots.get(menuSlot).x + 8,
                topPos + menu.slots.get(menuSlot).y + 8);
    }

    private void finishForgeAnimation(ForgeAnimation animation) {
        if (animation.finishSent) {
            return;
        }
        animation.finishSent = true;
        ClientPacketDistributor.sendToServer(new ReliquaryStationForgeFinishPayload(
                menu.containerId, animation.relicId, animation.seed));
        if (animation.targetMenuSlot >= 0) {
            forgeAnimation = null;
        }
    }

    private static float smoothStep(float value) {
        float clamped = Math.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private void drawHammerControl(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = leftPos + HAMMER_X;
        int y = topPos + HAMMER_Y;
        boolean ready = forgeReady();
        boolean hovered = ready && isInside(mouseX, mouseY, x, y, CONTROL_SIZE, CONTROL_SIZE);
        int expansion = hovered ? 2 : 0;
        int color = ready ? 0xFFFFFFFF : 0x70404040;
        graphics.blit(RenderPipelines.GUI_TEXTURED, HAMMER_TEXTURE,
                x - expansion, y - expansion, 0.0F, 0.0F,
                CONTROL_SIZE + expansion * 2, CONTROL_SIZE + expansion * 2,
                16, 16, 16, 16, color);
        if (hovered && hammerOutlineTexture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, hammerOutlineTexture,
                    x - expansion - 1, y - expansion - 1, 0.0F, 0.0F,
                    CONTROL_SIZE + expansion * 2 + 2, CONTROL_SIZE + expansion * 2 + 2,
                    34, 34, 34, 34, LOCK_GOLD);
            graphics.blit(RenderPipelines.GUI_TEXTURED, HAMMER_TEXTURE,
                    x - expansion, y - expansion, 0.0F, 0.0F,
                    CONTROL_SIZE + expansion * 2, CONTROL_SIZE + expansion * 2,
                    16, 16, 16, 16, color);
        }
        if (isInside(mouseX, mouseY, x, y, CONTROL_SIZE, CONTROL_SIZE)) {
            Component tooltip = Component.translatable(ready
                    ? "menu.noblephantasms.reliquary_station.forge"
                    : "menu.noblephantasms.reliquary_station.forge_unavailable");
            graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    private void drawRelicPicker(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!relicPickerOpen || menu.reliquary().isEmpty()) {
            return;
        }
        List<RelicFragmentDefinitions.Definition> definitions = RelicFragmentDefinitions.definitions();
        int visible = visibleRelicCount();
        int pickerX = pickerX();
        int pickerY = pickerY();
        RelicFragmentArchive archive = menu.archive();
        drawCarouselArrow(graphics, false, mouseX, mouseY);
        drawCarouselArrow(graphics, true, mouseX, mouseY);
        for (int visibleIndex = 0; visibleIndex < visible; visibleIndex++) {
            int definitionIndex = relicPickerOffset + visibleIndex;
            if (definitionIndex >= definitions.size()) {
                break;
            }
            RelicFragmentDefinitions.Definition definition = definitions.get(definitionIndex);
            int cellX = pickerX + visibleIndex * CAROUSEL_CELL_WIDTH;
            boolean hovered = isInside(mouseX, mouseY, cellX, pickerY,
                    CAROUSEL_CELL_WIDTH, CAROUSEL_HEIGHT);
            ItemStack relic = new ItemStack(definition.relic().get());
            RelicFragmentArchive.RelicSet set = archive.get(definition.relicId());
            int discovered = set == null ? 0 : set.discoveredCount();
            int total = set == null ? 0 : set.pieceCount();
            drawRelicBarIcon(graphics, definition, cellX + CAROUSEL_ITEM_OFFSET,
                    pickerY, set != null && set.complete(), relic);
            if (hovered) {
                Component progress = Component.translatable(
                        "menu.noblephantasms.mythical_reliquary.fragments",
                        discovered, set == null ? "?" : total);
                graphics.setTooltipForNextFrame(font, Component.literal(
                        relic.getHoverName().getString() + " — ").append(progress), mouseX, mouseY);
            }
        }
    }

    private void drawCarouselArrow(GuiGraphicsExtractor graphics, boolean right,
                                   int mouseX, int mouseY) {
        int x = carouselArrowX(right);
        int y = carouselArrowY();
        boolean hovered = isInside(mouseX, mouseY, x, y,
                CAROUSEL_ARROW_WIDTH, CAROUSEL_ARROW_HEIGHT);
        boolean pressed = right ? rightArrowPressed : leftArrowPressed;
        int sourceX = right ? 222 : 214;
        int sourceY = pressed ? 20 : hovered ? 10 : 0;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y,
                sourceX, sourceY, CAROUSEL_ARROW_WIDTH, CAROUSEL_ARROW_HEIGHT, 256, 256);
    }

    private void drawRelicBarIcon(GuiGraphicsExtractor graphics,
                                  RelicFragmentDefinitions.Definition definition,
                                  int x, int y, boolean forgeable, ItemStack fallback) {
        BarTexture texture = getRelicBarTexture(definition.inventoryTextureId(), !forgeable);
        if (texture == null) {
            graphics.item(fallback, x, y);
            return;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture.id(), x, y, 0.0F, 0.0F,
                16, 16, texture.width(), texture.frameHeight(),
                texture.width(), texture.height());
    }

    private int visibleRelicCount() {
        return Math.max(1, Math.min(RelicFragmentDefinitions.definitions().size(),
                (CAROUSEL_WIDTH - CAROUSEL_EDGE_RESERVE * 2) / CAROUSEL_CELL_WIDTH));
    }

    private int pickerX() {
        return leftPos + CAROUSEL_X + CAROUSEL_EDGE_RESERVE;
    }

    private int pickerY() {
        return topPos + CAROUSEL_Y;
    }

    private int carouselArrowX(boolean right) {
        return leftPos + CAROUSEL_X + (right ? CAROUSEL_WIDTH - CAROUSEL_ARROW_WIDTH : 0);
    }

    private int carouselArrowY() {
        return topPos + CAROUSEL_Y + (CAROUSEL_HEIGHT - CAROUSEL_ARROW_HEIGHT) / 2;
    }

    private void cacheControlOutlineTextures() {
        releaseControlOutlineTextures();
        bookOutlineTexture = createControlOutlineTexture(
                "mythic_reliquary", "book_outline", "Mythical Reliquary hover outline");
        hammerOutlineTexture = createControlOutlineTexture(
                "forge_hammer", "hammer_outline", "Forge hammer hover outline");
    }

    private BarTexture getRelicBarTexture(Identifier source, boolean sepia) {
        Map<Identifier, BarTexture> textures = sepia ? sepiaRelicBarTextures : relicBarTextures;
        if (textures.containsKey(source)) {
            return textures.get(source);
        }
        String path = "/assets/" + source.getNamespace() + "/textures/"
                + source.getPath() + ".png";
        try (InputStream stream = ReliquaryStationScreen.class.getResourceAsStream(path)) {
            if (stream == null) {
                textures.put(source, null);
                return null;
            }
            NativeImage image = NativeImage.read(stream);
            int width = image.getWidth();
            int height = image.getHeight();
            int frameHeight = height > width && height % width == 0 ? width : height;
            if (!sepia) {
                image.close();
                Identifier texture = Identifier.fromNamespaceAndPath(source.getNamespace(),
                        "textures/" + source.getPath() + ".png");
                BarTexture result = new BarTexture(texture, width, height, frameHeight, false);
                textures.put(source, result);
                return result;
            }
            for (int imageY = 0; imageY < height; imageY++) {
                for (int imageX = 0; imageX < width; imageX++) {
                    image.setPixel(imageX, imageY, sepia(image.getPixel(imageX, imageY)));
                }
            }
            Identifier texture = Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID,
                    "dynamic/reliquary_station/sepia_" + menu.containerId + "_"
                            + source.getNamespace() + "_" + source.getPath().replace('/', '_'));
            minecraft.getTextureManager().register(texture,
                    new DynamicTexture(() -> "Sepia reliquary station relic", image));
            BarTexture result = new BarTexture(texture, width, height, frameHeight, true);
            textures.put(source, result);
            return result;
        } catch (IOException exception) {
            textures.put(source, null);
            return null;
        }
    }

    private static int sepia(int color) {
        int alpha = ARGB.alpha(color);
        if (alpha == 0) {
            return color;
        }
        float luminance = (ARGB.red(color) * 0.2126F + ARGB.green(color) * 0.7152F
                + ARGB.blue(color) * 0.0722F) / 255.0F;
        int low = luminance < 0.5F ? DARK_SEPIA : MID_SEPIA;
        int high = luminance < 0.5F ? MID_SEPIA : LIGHT_SEPIA;
        float amount = luminance < 0.5F ? luminance * 2.0F : (luminance - 0.5F) * 2.0F;
        int red = Math.round(ARGB.red(low) + (ARGB.red(high) - ARGB.red(low)) * amount);
        int green = Math.round(ARGB.green(low) + (ARGB.green(high) - ARGB.green(low)) * amount);
        int blue = Math.round(ARGB.blue(low) + (ARGB.blue(high) - ARGB.blue(low)) * amount);
        return ARGB.color(alpha, red, green, blue);
    }

    private Identifier createControlOutlineTexture(String textureName, String dynamicName,
                                                   String debugName) {
        String path = "/assets/" + NoblePhantasms.MOD_ID
                + "/textures/item/" + textureName + ".png";
        try (InputStream stream = ReliquaryStationScreen.class.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (NativeImage source = NativeImage.read(stream)) {
                NativeImage outline = new NativeImage(source.getWidth() * 2 + 2,
                        source.getHeight() * 2 + 2, true);
                for (int y = 0; y < outline.getHeight(); y++) {
                    for (int x = 0; x < outline.getWidth(); x++) {
                        int scaledX = x - 1;
                        int scaledY = y - 1;
                        if (!opaqueScaled(source, scaledX, scaledY)
                                && adjacentToOpaqueScaled(source, scaledX, scaledY)) {
                            outline.setPixel(x, y, 0xFFFFFFFF);
                        }
                    }
                }
                Identifier texture = Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID,
                        "dynamic/reliquary_station/" + dynamicName + "_" + menu.containerId);
                minecraft.getTextureManager().register(texture,
                        new DynamicTexture(() -> debugName, outline));
                return texture;
            }
        } catch (IOException exception) {
            return null;
        }
    }

    private static boolean adjacentToOpaqueScaled(NativeImage image, int x, int y) {
        for (int offsetY = -1; offsetY <= 1; offsetY++) {
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                if ((offsetX != 0 || offsetY != 0)
                        && opaqueScaled(image, x + offsetX, y + offsetY)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean opaqueScaled(NativeImage image, int x, int y) {
        return x >= 0 && x < image.getWidth() * 2 && y >= 0 && y < image.getHeight() * 2
                && ARGB.alpha(image.getPixel(x / 2, y / 2)) > 0;
    }

    private void releaseControlOutlineTextures() {
        if (bookOutlineTexture != null && minecraft != null) {
            minecraft.getTextureManager().release(bookOutlineTexture);
            bookOutlineTexture = null;
        }
        if (hammerOutlineTexture != null && minecraft != null) {
            minecraft.getTextureManager().release(hammerOutlineTexture);
            hammerOutlineTexture = null;
        }
        for (BarTexture texture : sepiaRelicBarTextures.values()) {
            if (texture != null && texture.dynamic() && minecraft != null) {
                minecraft.getTextureManager().release(texture.id());
            }
        }
        sepiaRelicBarTextures.clear();
        relicBarTextures.clear();
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean selectedSetComplete() {
        RelicFragmentArchive.RelicSet set = menu.selectedSet();
        return set != null && set.complete();
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
                "dynamic/relic_outline/" + menu.containerId + "_" + Long.toUnsignedString(renderedSeed) + "_" + index);
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
        releaseControlOutlineTextures();
        super.removed();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (forgeAnimation != null) {
            return true;
        }
        if (event.button() == 0) {
            int bookX = leftPos + BOOK_X;
            int bookY = topPos + BOOK_Y;
            if (isInside(event.x(), event.y(), bookX, bookY, CONTROL_SIZE, CONTROL_SIZE)) {
                if (!menu.reliquary().isEmpty()) {
                    relicPickerOpen = !relicPickerOpen;
                    int maximumOffset = Math.max(0,
                            RelicFragmentDefinitions.definitions().size() - visibleRelicCount());
                    relicPickerOffset = Math.clamp(relicPickerOffset, 0, maximumOffset);
                }
                return true;
            }
            if (relicPickerOpen && !menu.reliquary().isEmpty()) {
                if (isInside(event.x(), event.y(), carouselArrowX(false), carouselArrowY(),
                        CAROUSEL_ARROW_WIDTH, CAROUSEL_ARROW_HEIGHT)) {
                    leftArrowPressed = true;
                    relicPickerOffset = Math.max(0, relicPickerOffset - 1);
                    return true;
                }
                if (isInside(event.x(), event.y(), carouselArrowX(true), carouselArrowY(),
                        CAROUSEL_ARROW_WIDTH, CAROUSEL_ARROW_HEIGHT)) {
                    rightArrowPressed = true;
                    int maximumOffset = Math.max(0,
                            RelicFragmentDefinitions.definitions().size() - visibleRelicCount());
                    relicPickerOffset = Math.min(maximumOffset, relicPickerOffset + 1);
                    return true;
                }
            }
            if (relicPickerOpen && !menu.reliquary().isEmpty()
                    && isInside(event.x(), event.y(), pickerX(), pickerY(),
                    visibleRelicCount() * CAROUSEL_CELL_WIDTH, CAROUSEL_HEIGHT)) {
                int visibleIndex = ((int) event.x() - pickerX()) / CAROUSEL_CELL_WIDTH;
                int definitionIndex = relicPickerOffset + visibleIndex;
                List<RelicFragmentDefinitions.Definition> definitions = RelicFragmentDefinitions.definitions();
                if (definitionIndex >= 0 && definitionIndex < definitions.size()) {
                    RelicFragmentDefinitions.Definition definition = definitions.get(definitionIndex);
                    selectionOriginX = pickerX() + visibleIndex * CAROUSEL_CELL_WIDTH
                            + CAROUSEL_CELL_WIDTH / 2;
                    selectionOriginY = pickerY() + CAROUSEL_HEIGHT / 2;
                    menu.selectRelic(minecraft.player, definition.relicId());
                    ClientPacketDistributor.sendToServer(new MythicalReliquarySelectPayload(
                            menu.containerId, definition.relicId()));
                    relicPickerOpen = false;
                }
                return true;
            }
            if (isInside(event.x(), event.y(), leftPos + HAMMER_X, topPos + HAMMER_Y,
                    CONTROL_SIZE, CONTROL_SIZE)) {
                if (forgeReady()) {
                    forge();
                }
                return true;
            }
            if (ejectionAnimationStarted != 0L) {
                return true;
            }
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (forgeAnimation != null) {
            return true;
        }
        if (relicPickerOpen && isInside(mouseX, mouseY,
                leftPos + CAROUSEL_X, topPos + CAROUSEL_Y,
                CAROUSEL_WIDTH, CAROUSEL_HEIGHT)) {
            int maximumOffset = Math.max(0,
                    RelicFragmentDefinitions.definitions().size() - visibleRelicCount());
            int direction = scrollY > 0.0 ? -1 : scrollY < 0.0 ? 1 : 0;
            relicPickerOffset = Math.clamp(relicPickerOffset + direction, 0, maximumOffset);
            return direction != 0;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (forgeAnimation != null) {
            return true;
        }
        if (event.button() == 0 && dragging != null) {
            dragging.x = (int) event.x() - dragOffsetX;
            dragging.y = (int) event.y() - dragOffsetY;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (forgeAnimation != null) {
            return true;
        }
        if (event.button() == 0 && (leftArrowPressed || rightArrowPressed)) {
            leftArrowPressed = false;
            rightArrowPressed = false;
            return true;
        }
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
                boolean completed = selectedSetComplete() && layout != null
                        && pieces.size() == layout.pieces().size()
                        && pieces.stream().allMatch(piece -> piece.locked);
                if (completed) {
                    dragging.lockAnimationStarted = 0L;
                    assembledState.tracePixels = RelicFragmenter.startOutlineNear(
                            assembledState.outline.pixels(), dragging.piece);
                    assemblyAnimationStarted = now;
                    assembledState.lockAnimationStarted = now;
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
        RelicFragmentArchive.RelicSet set = menu.selectedSet();
        Identifier relicId = menu.relicId();
        if (forgeReady() && relicId != null && set != null) {
            completionSent = true;
            ClientPacketDistributor.sendToServer(new ReliquaryStationCompletePayload(
                    menu.containerId, relicId, set.seed()));
        }
    }

    private boolean forgeReady() {
        return forgeAnimation == null && !completionSent
                && assemblyAnimationStarted == 0L && layout != null
                && selectedSetComplete() && pieces.size() == layout.pieces().size()
                && pieces.stream().allMatch(piece -> piece.locked);
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

    private record BarTexture(Identifier id, int width, int height, int frameHeight,
                              boolean dynamic) {
    }

    private record SlotTarget(int x, int y) {
    }

    private static final class ForgeAnimation {
        private final Identifier relicId;
        private final long seed;
        private final int targetMenuSlot;
        private final RelicFragmenter.Piece display;
        private final int sourceScale;
        private final long started;
        private int sourceX;
        private int sourceY;
        private boolean finishSent;

        private ForgeAnimation(Identifier relicId, long seed, int targetMenuSlot,
                               RelicFragmenter.Piece display, int sourceX, int sourceY,
                               int sourceScale, long started) {
            this.relicId = relicId;
            this.seed = seed;
            this.targetMenuSlot = targetMenuSlot;
            this.display = display;
            this.sourceX = sourceX;
            this.sourceY = sourceY;
            this.sourceScale = sourceScale;
            this.started = started;
        }

        private int width() {
            return (display.maxX() - display.minX() + 1) * sourceScale;
        }

        private int height() {
            return (display.maxY() - display.minY() + 1) * sourceScale;
        }
    }

    private static final class PieceState {
        private final RelicFragmenter.Piece piece;
        private final RelicFragmenter.OutlineMask outline;
        private List<RelicFragmenter.OutlinePixel> tracePixels;
        private Identifier outlineTexture;
        private int x;
        private int y;
        private int spawnX;
        private int spawnY;
        private int restX;
        private int restY;
        private int height;
        private long lockAnimationStarted;
        private boolean locked;

        private PieceState(RelicFragmenter.Piece piece, int x, int y, int height) {
            this.piece = piece;
            this.outline = RelicFragmenter.createOutline(piece);
            this.tracePixels = outline.pixels();
            this.x = x;
            this.y = y;
            this.spawnX = x;
            this.spawnY = y;
            this.restX = x;
            this.restY = y;
            this.height = height;
        }
    }

}
