package net.turtleboi.noblephantasms.client.ui;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.CurioRelicItem;
import net.turtleboi.noblephantasms.item.custom.EyeOfHorusItem;
import net.turtleboi.noblephantasms.network.EyeAssemblyPayload;
import net.turtleboi.noblephantasms.relic.RelicFragmenter;

public final class EyeOfHorusHud {
    private static final double TWO_PI = Math.PI * 2.0;
    private static final double ORBIT_PERIOD_MILLISECONDS = 6000.0;
    private static final long ORBIT_REPOSITION_DURATION = 400L;
    private static final long COMPLETION_POSITION_DURATION = 400L;
    private static final long COMPLETION_RAMP_DURATION = 1000L;
    private static final long COMPLETION_SLOW_DURATION = 500L;
    private static final long COMPLETION_DASH_DURATION_PER_PIECE = 110L;
    private static final double COMPLETION_EXTRA_ROTATIONS = 2.0;
    private static final double COMPLETION_RADIUS_INCREASE = 8.0;
    private static final long MAXIMUM_COMPLETION_FRAME_STEP = 50L;
    private static final long OUTLINE_MILLISECONDS_PER_PIXEL = 10L;
    private static final long OUTLINE_FLASH_DURATION = 90L;
    private static final long OUTLINE_FADE_DURATION = 260L;
    private static final int OUTLINE_GOLD = 0xFFFFC83D;
    private static UUID activePlayer;
    private static long activeSeed;
    private static int displayedMask;
    private static long orbitTransitionStarted;
    private static final Map<Integer, OrbitSlot> orbitTransitionStarts = new LinkedHashMap<>();
    private static final Map<Integer, OrbitSlot> orbitTransitionTargets = new LinkedHashMap<>();
    private static boolean completing;
    private static long completionElapsed;
    private static long completionLastFrame;
    private static double completionStartAngle;
    private static final Map<Integer, OrbitSlot> completionStarts = new LinkedHashMap<>();
    private static final Map<Integer, OrbitSlot> completionTargets = new LinkedHashMap<>();
    private static RelicFragmenter.Layout layout;
    private static RelicFragmenter.Piece assembledPiece;
    private static RelicFragmenter.OutlineMask outline;
    private static List<RelicFragmenter.OutlinePixel> tracePixels = List.of();
    private static Identifier outlineTexture;

    public static void tick() {
        synchronizeState();
    }

    public static boolean render(GuiGraphicsExtractor graphics) {
        synchronizeState();
        Minecraft minecraft = Minecraft.getInstance();
        if (layout == null || displayedMask == 0 || minecraft.options.hideGui
                || !minecraft.options.getCameraType().isFirstPerson()) {
            completionLastFrame = 0L;
            return false;
        }

        graphics.nextStratum();
        long now = Util.getMillis();
        if (completing) {
            advanceCompletion(now);
            drawCompletion(graphics);
            return completionElapsed >= completionDashStart();
        }
        if (orbitTransitionStarted != 0L) {
            drawOrbitTransition(graphics, now);
            return false;
        }
        if (Integer.bitCount(displayedMask) < layout.pieceCount()) {
            drawOrbitingPieces(graphics, now);
            return false;
        }

        drawAssembledEye(graphics);
        return true;
    }

    private static void synchronizeState() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || !CurioRelicItem.isEquipped(minecraft.player, ModItems.EYE_OF_HORUS.get())) {
            reset();
            return;
        }

        int mask = EyeOfHorusItem.getCollectedPieceMask(minecraft.player);
        long seed = EyeOfHorusItem.getFragmentSeed(minecraft.player);
        if (mask == 0 || seed == 0L) {
            reset();
            return;
        }

        UUID playerId = minecraft.player.getUUID();
        if (!playerId.equals(activePlayer) || seed != activeSeed || layout == null) {
            initialize(minecraft, playerId, seed, mask);
            return;
        }

        finishOrbitTransition(Util.getMillis());
        if (mask != displayedMask) {
            int previousMask = displayedMask;
            int previousCount = Integer.bitCount(previousMask);
            int currentCount = Integer.bitCount(mask);
            int newBits = mask & ~previousMask;
            int newPieceIndex = newBits == 0 ? 0 : Integer.numberOfTrailingZeros(newBits);
            long now = Util.getMillis();
            if (previousCount < layout.pieceCount() && currentCount >= layout.pieceCount()) {
                startCompletion(previousMask, mask, newPieceIndex, now);
            } else {
                startOrbitTransition(previousMask, mask, now);
                completing = false;
                completionElapsed = 0L;
                completionLastFrame = 0L;
            }
            displayedMask = mask;
        }
    }

    private static void initialize(Minecraft minecraft, UUID playerId, long seed, int mask) {
        releaseOutlineTexture();
        activePlayer = playerId;
        activeSeed = seed;
        displayedMask = mask;
        layout = EyeOfHorusItem.createFragmentLayout(seed);
        assembledPiece = RelicFragmenter.assemble(layout);
        outline = RelicFragmenter.createOutline(assembledPiece);
        tracePixels = outline.pixels();
        cacheOutlineTexture(minecraft);
        clearOrbitTransition();
        completionStarts.clear();
        completionTargets.clear();
        completionElapsed = 0L;
        completionLastFrame = 0L;
        boolean complete = Integer.bitCount(mask) >= layout.pieceCount();
        boolean assembled = complete && EyeOfHorusItem.isAssembled(minecraft.player);
        completing = complete && !assembled;
        if (completing) {
            startCompletion(mask, mask, 0, Util.getMillis());
        }
    }

    private static void startOrbitTransition(int previousMask, int newMask, long now) {
        Map<Integer, OrbitSlot> currentSlots = currentOrbitSlots(previousMask, now);
        Map<Integer, OrbitSlot> targetSlots = createOrbitSlots(newMask, orbitRadius(Integer.bitCount(newMask)));
        orbitTransitionStarts.clear();
        orbitTransitionTargets.clear();
        for (Map.Entry<Integer, OrbitSlot> entry : targetSlots.entrySet()) {
            OrbitSlot target = entry.getValue();
            orbitTransitionStarts.put(entry.getKey(), currentSlots.getOrDefault(
                    entry.getKey(), new OrbitSlot(target.angle(), 0.0)));
            orbitTransitionTargets.put(entry.getKey(), target);
        }
        orbitTransitionStarted = now;
    }

    private static void finishOrbitTransition(long now) {
        if (orbitTransitionStarted != 0L
                && now - orbitTransitionStarted >= ORBIT_REPOSITION_DURATION) {
            clearOrbitTransition();
        }
    }

    private static void startCompletion(int previousMask, int completeMask, int lastPieceIndex, long now) {
        Map<Integer, OrbitSlot> currentSlots = currentOrbitSlots(previousMask, now);
        Map<Integer, OrbitSlot> targetSlots = createOrbitSlots(
                completeMask, orbitRadius(Integer.bitCount(completeMask)));
        completionStarts.clear();
        completionTargets.clear();
        for (Map.Entry<Integer, OrbitSlot> entry : targetSlots.entrySet()) {
            OrbitSlot target = entry.getValue();
            completionStarts.put(entry.getKey(), currentSlots.getOrDefault(
                    entry.getKey(), new OrbitSlot(target.angle(), 0.0)));
            completionTargets.put(entry.getKey(), target);
        }
        clearOrbitTransition();
        completionStartAngle = orbitAngle(now);
        completionElapsed = 0L;
        completionLastFrame = 0L;
        completing = true;
        RelicFragmenter.Piece lastPiece = layout.pieces().get(
                Math.clamp(lastPieceIndex, 0, layout.pieceCount() - 1));
        tracePixels = RelicFragmenter.startOutlineNear(outline.pixels(), lastPiece);
    }

    private static void advanceCompletion(long now) {
        if (completionLastFrame != 0L) {
            completionElapsed += Math.clamp(
                    now - completionLastFrame, 0L, MAXIMUM_COMPLETION_FRAME_STEP);
        }
        completionLastFrame = now;
    }

    private static void drawCompletion(GuiGraphicsExtractor graphics) {
        if (completionElapsed < COMPLETION_POSITION_DURATION) {
            drawCompletionPositioning(graphics);
            return;
        }
        if (completionElapsed < completionSlowStart()) {
            drawCompletionRamp(graphics);
            return;
        }
        if (completionElapsed < completionDashStart()) {
            drawCompletionSlowdown(graphics);
            return;
        }
        if (completionElapsed < completionOutlineStart()) {
            drawCompletionDash(graphics);
            return;
        }

        drawAssembledEye(graphics);
        long outlineElapsed = completionElapsed - completionOutlineStart();
        long traceDuration = tracePixels.size() * OUTLINE_MILLISECONDS_PER_PIXEL;
        long flashEnd = traceDuration + OUTLINE_FLASH_DURATION;
        long animationEnd = flashEnd + OUTLINE_FADE_DURATION;
        if (outlineElapsed < traceDuration) {
            int visiblePixels = Math.min(tracePixels.size(),
                    (int) (outlineElapsed / OUTLINE_MILLISECONDS_PER_PIXEL) + 1);
            drawTracingOutline(graphics, visiblePixels);
        } else if (outlineElapsed < flashEnd) {
            float progress = (outlineElapsed - traceDuration) / (float) OUTLINE_FLASH_DURATION;
            drawCachedOutline(graphics, ARGB.srgbLerp(progress, OUTLINE_GOLD, 0xFFFFFFFF));
        } else if (outlineElapsed < animationEnd) {
            float fade = 1.0F - (outlineElapsed - flashEnd) / (float) OUTLINE_FADE_DURATION;
            drawCachedOutline(graphics, ARGB.color(Math.round(255.0F * fade), 0xFFFFFF));
        } else {
            finishCompletion();
        }
    }

    private static void drawCompletionPositioning(GuiGraphicsExtractor graphics) {
        double progress = completionElapsed / (double) COMPLETION_POSITION_DURATION;
        drawOrbitSlots(graphics,
                interpolateSlots(completionStarts, completionTargets, smoothStep(progress)),
                completionStartAngle + baseAngularSpeed() * completionElapsed);
    }

    private static void drawCompletionRamp(GuiGraphicsExtractor graphics) {
        long elapsed = completionElapsed - COMPLETION_POSITION_DURATION;
        double progress = elapsed / (double) COMPLETION_RAMP_DURATION;
        double radius = orbitRadius(layout.pieceCount())
                + COMPLETION_RADIUS_INCREASE * smoothStep(progress);
        drawOrbitSlots(graphics, withRadius(completionTargets, radius),
                completionRampStartAngle() + rampAngularDistance(elapsed));
    }

    private static void drawCompletionSlowdown(GuiGraphicsExtractor graphics) {
        long elapsed = completionElapsed - completionSlowStart();
        drawOrbitSlots(graphics,
                withRadius(completionTargets,
                        orbitRadius(layout.pieceCount()) + COMPLETION_RADIUS_INCREASE),
                completionSlowStartAngle() + slowdownAngularDistance(elapsed));
    }

    private static void drawCompletionDash(GuiGraphicsExtractor graphics) {
        long elapsed = completionElapsed - completionDashStart();
        double stoppedAngle = completionStopAngle();
        double centerX = graphics.guiWidth() / 2.0;
        double centerY = graphics.guiHeight() / 2.0;
        double targetX = eyeOriginX(graphics);
        double targetY = eyeOriginY(graphics);
        int ordinal = 0;
        for (Map.Entry<Integer, OrbitSlot> entry : completionTargets.entrySet()) {
            RelicFragmenter.Piece piece = layout.pieces().get(entry.getKey());
            OrbitSlot slot = new OrbitSlot(entry.getValue().angle(),
                    orbitRadius(layout.pieceCount()) + COMPLETION_RADIUS_INCREASE);
            double pieceAngle = stoppedAngle + slot.angle();
            double startX = centerX + Math.cos(pieceAngle) * slot.radius() - pieceCenterX(piece);
            double startY = centerY + Math.sin(pieceAngle) * slot.radius() - pieceCenterY(piece);
            double progress = Math.clamp(
                    (elapsed - ordinal * COMPLETION_DASH_DURATION_PER_PIECE)
                            / (double) COMPLETION_DASH_DURATION_PER_PIECE,
                    0.0, 1.0);
            progress = smoothStep(progress);
            drawPiece(graphics, piece,
                    startX + (targetX - startX) * progress,
                    startY + (targetY - startY) * progress);
            ordinal++;
        }
    }

    private static void finishCompletion() {
        if (!completing) {
            return;
        }
        completing = false;
        completionLastFrame = 0L;
        completionStarts.clear();
        completionTargets.clear();
        ClientPacketDistributor.sendToServer(new EyeAssemblyPayload(activeSeed));
    }

    private static void drawOrbitingPieces(GuiGraphicsExtractor graphics, long now) {
        drawOrbitSlots(graphics,
                createOrbitSlots(displayedMask, orbitRadius(Integer.bitCount(displayedMask))),
                orbitAngle(now));
    }

    private static void drawOrbitTransition(GuiGraphicsExtractor graphics, long now) {
        double progress = Math.clamp(
                (now - orbitTransitionStarted) / (double) ORBIT_REPOSITION_DURATION, 0.0, 1.0);
        drawOrbitSlots(graphics,
                interpolateSlots(orbitTransitionStarts, orbitTransitionTargets, smoothStep(progress)),
                orbitAngle(now));
    }

    private static void drawOrbitSlots(GuiGraphicsExtractor graphics, Map<Integer, OrbitSlot> slots,
                                       double angle) {
        double centerX = graphics.guiWidth() / 2.0;
        double centerY = graphics.guiHeight() / 2.0;
        for (Map.Entry<Integer, OrbitSlot> entry : slots.entrySet()) {
            RelicFragmenter.Piece piece = layout.pieces().get(entry.getKey());
            OrbitSlot slot = entry.getValue();
            double pieceAngle = angle + slot.angle();
            drawPiece(graphics, piece,
                    centerX + Math.cos(pieceAngle) * slot.radius() - pieceCenterX(piece),
                    centerY + Math.sin(pieceAngle) * slot.radius() - pieceCenterY(piece));
        }
    }

    private static void drawAssembledEye(GuiGraphicsExtractor graphics) {
        drawPiece(graphics, assembledPiece, eyeOriginX(graphics), eyeOriginY(graphics));
    }

    private static void drawPiece(GuiGraphicsExtractor graphics, RelicFragmenter.Piece piece,
                                  double originX, double originY) {
        for (RelicFragmenter.Pixel pixel : piece.pixels()) {
            int x = (int) Math.round(originX + pixel.x());
            int y = (int) Math.round(originY + pixel.y());
            graphics.fill(x, y, x + 1, y + 1, pixel.color());
        }
    }

    private static void drawTracingOutline(GuiGraphicsExtractor graphics, int visiblePixels) {
        int originX = eyeOriginX(graphics);
        int originY = eyeOriginY(graphics);
        for (int index = 0; index < visiblePixels; index++) {
            RelicFragmenter.OutlinePixel pixel = tracePixels.get(index);
            int x = originX + pixel.x();
            int y = originY + pixel.y();
            graphics.fill(x, y, x + 1, y + 1, OUTLINE_GOLD);
        }
    }

    private static void drawCachedOutline(GuiGraphicsExtractor graphics, int color) {
        if (outlineTexture == null) {
            return;
        }
        int x = eyeOriginX(graphics) + outline.minX();
        int y = eyeOriginY(graphics) + outline.minY();
        graphics.blit(RenderPipelines.GUI_TEXTURED, outlineTexture, x, y, 0.0F, 0.0F,
                outline.width(), outline.height(), outline.width(), outline.height(),
                outline.width(), outline.height(), color);
    }

    private static void cacheOutlineTexture(Minecraft minecraft) {
        NativeImage image = new NativeImage(outline.width(), outline.height(), true);
        for (RelicFragmenter.OutlinePixel pixel : outline.pixels()) {
            image.setPixel(pixel.x() - outline.minX(), pixel.y() - outline.minY(), 0xFFFFFFFF);
        }
        outlineTexture = Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID,
                "dynamic/eye_of_horus_outline/" + activePlayer + "_" + Long.toUnsignedString(activeSeed));
        minecraft.getTextureManager().register(outlineTexture,
                new DynamicTexture(() -> "Eye of Horus outline", image));
    }

    private static Map<Integer, OrbitSlot> createOrbitSlots(int mask, double radius) {
        List<Integer> pieceIndices = collectedPieceIndices(mask);
        Map<Integer, OrbitSlot> slots = new LinkedHashMap<>();
        for (int ordinal = 0; ordinal < pieceIndices.size(); ordinal++) {
            slots.put(pieceIndices.get(ordinal),
                    new OrbitSlot(ordinal * TWO_PI / pieceIndices.size(), radius));
        }
        return slots;
    }

    private static Map<Integer, OrbitSlot> currentOrbitSlots(int mask, long now) {
        if (orbitTransitionStarted == 0L) {
            return createOrbitSlots(mask, orbitRadius(Integer.bitCount(mask)));
        }
        double progress = Math.clamp(
                (now - orbitTransitionStarted) / (double) ORBIT_REPOSITION_DURATION, 0.0, 1.0);
        return interpolateSlots(orbitTransitionStarts, orbitTransitionTargets, smoothStep(progress));
    }

    private static Map<Integer, OrbitSlot> interpolateSlots(Map<Integer, OrbitSlot> starts,
                                                             Map<Integer, OrbitSlot> targets,
                                                             double progress) {
        Map<Integer, OrbitSlot> slots = new LinkedHashMap<>();
        for (Map.Entry<Integer, OrbitSlot> entry : targets.entrySet()) {
            OrbitSlot start = starts.get(entry.getKey());
            if (start != null) {
                slots.put(entry.getKey(), interpolateSlot(start, entry.getValue(), progress));
            }
        }
        return slots;
    }

    private static Map<Integer, OrbitSlot> withRadius(Map<Integer, OrbitSlot> source, double radius) {
        Map<Integer, OrbitSlot> slots = new LinkedHashMap<>();
        for (Map.Entry<Integer, OrbitSlot> entry : source.entrySet()) {
            slots.put(entry.getKey(), new OrbitSlot(entry.getValue().angle(), radius));
        }
        return slots;
    }

    private static OrbitSlot interpolateSlot(OrbitSlot start, OrbitSlot target, double progress) {
        double angleDifference = Math.atan2(
                Math.sin(target.angle() - start.angle()),
                Math.cos(target.angle() - start.angle()));
        return new OrbitSlot(
                start.angle() + angleDifference * progress,
                start.radius() + (target.radius() - start.radius()) * progress);
    }

    private static List<Integer> collectedPieceIndices(int mask) {
        List<Integer> indices = new ArrayList<>(Integer.bitCount(mask));
        for (int index = 0; index < layout.pieceCount(); index++) {
            if ((mask & 1 << index) != 0) {
                indices.add(index);
            }
        }
        return indices;
    }

    private static double orbitRadius(int pieceCount) {
        return 6.0 + pieceCount * 1.5;
    }

    private static double orbitAngle(long time) {
        return time % (long) ORBIT_PERIOD_MILLISECONDS / ORBIT_PERIOD_MILLISECONDS * TWO_PI;
    }

    private static long completionSlowStart() {
        return COMPLETION_POSITION_DURATION + COMPLETION_RAMP_DURATION;
    }

    private static long completionDashStart() {
        return completionSlowStart() + COMPLETION_SLOW_DURATION;
    }

    private static long completionOutlineStart() {
        return completionDashStart() + layout.pieceCount() * COMPLETION_DASH_DURATION_PER_PIECE;
    }

    private static double baseAngularSpeed() {
        return TWO_PI / ORBIT_PERIOD_MILLISECONDS;
    }

    private static double peakAngularSpeed() {
        return baseAngularSpeed()
                + 2.0 * COMPLETION_EXTRA_ROTATIONS * TWO_PI / COMPLETION_RAMP_DURATION;
    }

    private static double completionRampStartAngle() {
        return completionStartAngle + baseAngularSpeed() * COMPLETION_POSITION_DURATION;
    }

    private static double rampAngularDistance(long elapsed) {
        double progress = Math.clamp(elapsed / (double) COMPLETION_RAMP_DURATION, 0.0, 1.0);
        double integratedRamp = progress * progress * progress
                - 0.5 * progress * progress * progress * progress;
        double addedSpeed = peakAngularSpeed() - baseAngularSpeed();
        return baseAngularSpeed() * elapsed
                + addedSpeed * COMPLETION_RAMP_DURATION * integratedRamp;
    }

    private static double completionSlowStartAngle() {
        return completionRampStartAngle() + rampAngularDistance(COMPLETION_RAMP_DURATION);
    }

    private static double slowdownAngularDistance(long elapsed) {
        double progress = Math.clamp(elapsed / (double) COMPLETION_SLOW_DURATION, 0.0, 1.0);
        double integratedSlowdown = progress
                - progress * progress * progress
                + 0.5 * progress * progress * progress * progress;
        return peakAngularSpeed() * COMPLETION_SLOW_DURATION * integratedSlowdown;
    }

    private static double completionStopAngle() {
        return completionSlowStartAngle() + slowdownAngularDistance(COMPLETION_SLOW_DURATION);
    }

    private static double smoothStep(double value) {
        return value * value * (3.0 - 2.0 * value);
    }

    private static double pieceCenterX(RelicFragmenter.Piece piece) {
        return (piece.minX() + piece.maxX() + 1) / 2.0;
    }

    private static double pieceCenterY(RelicFragmenter.Piece piece) {
        return (piece.minY() + piece.maxY() + 1) / 2.0;
    }

    private static int eyeOriginX(GuiGraphicsExtractor graphics) {
        return (int) Math.round((graphics.guiWidth() - layout.width()) / 2.0);
    }

    private static int eyeOriginY(GuiGraphicsExtractor graphics) {
        return (int) Math.round((graphics.guiHeight() - layout.height()) / 2.0);
    }

    private static void clearOrbitTransition() {
        orbitTransitionStarted = 0L;
        orbitTransitionStarts.clear();
        orbitTransitionTargets.clear();
    }

    private static void reset() {
        releaseOutlineTexture();
        activePlayer = null;
        activeSeed = 0L;
        displayedMask = 0;
        clearOrbitTransition();
        completing = false;
        completionElapsed = 0L;
        completionLastFrame = 0L;
        completionStarts.clear();
        completionTargets.clear();
        layout = null;
        assembledPiece = null;
        outline = null;
        tracePixels = List.of();
    }

    private static void releaseOutlineTexture() {
        if (outlineTexture != null) {
            Minecraft.getInstance().getTextureManager().release(outlineTexture);
            outlineTexture = null;
        }
    }

    private record OrbitSlot(double angle, double radius) {
    }
}
