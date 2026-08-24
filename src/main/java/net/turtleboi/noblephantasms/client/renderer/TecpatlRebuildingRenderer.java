package net.turtleboi.noblephantasms.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
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
import net.turtleboi.noblephantasms.item.custom.TecpatlOfTheFifthSunItem;
import net.turtleboi.noblephantasms.relic.RelicFragmenter;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class TecpatlRebuildingRenderer implements SpecialModelRenderer<Integer> {
    private static final Identifier ITEM_ID = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "tecpatl_of_the_fifth_sun");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/item/tecpatl_of_the_fifth_sun.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);
    private static final RelicFragmenter.Layout LAYOUT = RelicFragmenter.createExact(
            ITEM_ID, TecpatlOfTheFifthSunItem.SHATTER_SEED,
            TecpatlOfTheFifthSunItem.SHARD_COUNT);

    public static void register(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.fromNamespaceAndPath(
                NoblePhantasms.MOD_ID, "tecpatl_rebuilding"), Unbaked.MAP_CODEC);
    }

    @Override
    public void submit(@Nullable Integer returnedShards, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector,
                       int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        if (returnedShards == null || returnedShards == 0 || LAYOUT == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            for (int index = 0; index < LAYOUT.pieceCount(); index++) {
                if ((returnedShards & 1 << index) != 0) {
                    EyeShardRenderer.drawAssembledPiece(
                            pose, buffer, LAYOUT, LAYOUT.pieces().get(index), lightCoords);
                }
            }
        });
        if (hasFoil) {
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.glint(), (pose, buffer) -> {
                SheetedDecalTextureGenerator glintBuffer =
                        new SheetedDecalTextureGenerator(buffer, pose, 0.0078125F);
                for (int index = 0; index < LAYOUT.pieceCount(); index++) {
                    if ((returnedShards & 1 << index) != 0) {
                        EyeShardRenderer.drawAssembledPiece(
                                pose, glintBuffer, LAYOUT, LAYOUT.pieces().get(index), lightCoords);
                    }
                }
            });
        }
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0.0F, 0.0F, 0.475F));
        output.accept(new Vector3f(1.0F, 1.0F, 0.525F));
    }

    @Override
    public @Nullable Integer extractArgument(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.TECPATL_RETURNED_SHARDS.get(), 0);
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Integer> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public TecpatlRebuildingRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new TecpatlRebuildingRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
