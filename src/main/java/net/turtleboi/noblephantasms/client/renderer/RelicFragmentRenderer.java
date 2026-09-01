package net.turtleboi.noblephantasms.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.entity.renderer.EyeShardRenderer;
import net.turtleboi.noblephantasms.relic.RelicFragmentData;
import net.turtleboi.noblephantasms.relic.RelicFragmentDefinitions;
import net.turtleboi.noblephantasms.relic.RelicFragmenter;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class RelicFragmentRenderer implements SpecialModelRenderer<RelicFragmentData> {
    public static void register(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.fromNamespaceAndPath(
                NoblePhantasms.MOD_ID, "relic_fragment"), Unbaked.MAP_CODEC);
    }

    @Override
    public void submit(@Nullable RelicFragmentData fragment, PoseStack poseStack,
                       SubmitNodeCollector collector, int lightCoords, int overlayCoords,
                       boolean hasFoil, int outlineColor) {
        if (fragment == null || fragment.pieceIndex() < 0) {
            return;
        }
        RelicFragmentDefinitions.Definition definition = RelicFragmentDefinitions.get(fragment.relicId());
        RelicFragmenter.Layout layout = RelicFragmenter.createExact(
                fragment.relicId(), fragment.seed(), fragment.pieceCount());
        if (definition == null || layout == null || fragment.pieceIndex() >= layout.pieceCount()) {
            return;
        }
        Identifier source = definition.textureId();
        Identifier texture = Identifier.fromNamespaceAndPath(source.getNamespace(),
                "textures/" + source.getPath() + ".png");
        RenderType renderType = RenderTypes.entityCutout(texture);
        RelicFragmenter.Piece piece = layout.pieces().get(fragment.pieceIndex());
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> EyeShardRenderer.drawStandalonePiece(
                        pose, buffer, layout, piece, lightCoords));
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(-1.5F, -1.5F, 0.45F));
        output.accept(new Vector3f(2.5F, 2.5F, 0.55F));
    }

    @Override
    public @Nullable RelicFragmentData extractArgument(ItemStack stack) {
        return stack.get(ModDataComponents.RELIC_FRAGMENT.get());
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<RelicFragmentData> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public RelicFragmentRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new RelicFragmentRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
