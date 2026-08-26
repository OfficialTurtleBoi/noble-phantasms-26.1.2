package net.turtleboi.noblephantasms.entity.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.model.YasakaniGuardianModel;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.entity.custom.YasakaniGuardianEntity;
import net.turtleboi.noblephantasms.entity.renderer.states.YasakaniGuardianRenderState;

public final class YasakaniGuardianRenderer extends HumanoidMobRenderer<YasakaniGuardianEntity,
        YasakaniGuardianRenderState, YasakaniGuardianModel> {
    private static final Identifier[] TEXTURES = {
            texture("oshihomimi"),
            texture("hohi"),
            texture("amatsuhikone"),
            texture("ikutsuhikone"),
            texture("kumanokusubi")
    };
    private static final int SPIRIT_ALPHA = 0xA8000000;
    private static final int[] COLORS = {
            0xFF4E8FF1,
            0xFFED2146,
            0xFFE8C92E,
            0xFF42D654,
            0xFFDFD8B3
    };

    public YasakaniGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new YasakaniGuardianModel(context.bakeLayer(
                YasakaniGuardianModel.LAYER_LOCATION)), 0.35F);
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.YASAKANI_GUARDIAN.get(), YasakaniGuardianRenderer::new);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(
                NoblePhantasms.MOD_ID, "textures/entity/" + name + ".png");
    }

    @Override
    public YasakaniGuardianRenderState createRenderState() {
        return new YasakaniGuardianRenderState();
    }

    @Override
    public void extractRenderState(YasakaniGuardianEntity entity,
                                   YasakaniGuardianRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.spirit = entity.getSpirit().ordinal();
    }

    @Override
    public Identifier getTextureLocation(YasakaniGuardianRenderState state) {
        return TEXTURES[Math.clamp(state.spirit, 0, TEXTURES.length - 1)];
    }

    @Override
    protected int getBlockLightLevel(YasakaniGuardianEntity entity, BlockPos position) {
        return 15;
    }

    @Override
    protected int getSkyLightLevel(YasakaniGuardianEntity entity, BlockPos position) {
        return 15;
    }

    @Override
    protected RenderType getRenderType(YasakaniGuardianRenderState state, boolean bodyVisible,
                                       boolean translucent, boolean glowing) {
        if (bodyVisible || translucent) {
            return RenderTypes.entityTranslucent(getTextureLocation(state));
        }
        return glowing ? RenderTypes.outline(getTextureLocation(state)) : null;
    }

    @Override
    protected int getModelTint(YasakaniGuardianRenderState state) {
        int color = state.spirit == 0
                ? 0xFFFFFF
                : COLORS[Math.clamp(state.spirit, 0, COLORS.length - 1)] & 0xFFFFFF;
        return SPIRIT_ALPHA | color;
    }
}
