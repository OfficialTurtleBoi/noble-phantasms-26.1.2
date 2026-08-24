package net.turtleboi.noblephantasms.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.renderer.states.XiuhcoatlProjectileRenderState;

public final class XiuhcoatlModel extends EntityModel<XiuhcoatlProjectileRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "xiuhcoatl"), "main");

    private static final float ROOT_X_ROTATION = Mth.PI;
    private static final float ROOT_Y_ROTATION = Mth.HALF_PI;
    private static final float MAX_HORIZONTAL_BEND = 45.0F;
    private static final float MAX_VERTICAL_BEND = 30.0F;
    private static final float WAG_SPEED = 0.55F;
    private static final float WAG_DELAY = 0.8F;

    private final ModelPart body1;
    private final ModelPart body2;
    private final ModelPart tail;

    public XiuhcoatlModel(ModelPart root) {
        super(root);
        ModelPart serpent = root.getChild("fire_serpent");
        ModelPart head = serpent.getChild("head");
        this.body1 = head.getChild("body1");
        this.body2 = body1.getChild("body2");
        this.tail = body2.getChild("tail");
    }

    public static void registerLayerDefinition(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER_LOCATION, XiuhcoatlModel::createBodyLayer);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        PartDefinition serpent = root.addOrReplaceChild("fire_serpent", CubeListBuilder.create(),
                PartPose.rotation(ROOT_X_ROTATION, ROOT_Y_ROTATION, 0.0F));

        PartDefinition head = serpent.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.5F, -3.5F, -4.5F, 7.0F, 7.0F, 9.0F, CubeDeformation.NONE)
                        .texOffs(32, 24)
                        .addBox(3.75F, -4.5F, 0.5F, 0.0F, 3.0F, 6.0F, CubeDeformation.NONE)
                        .texOffs(32, 24)
                        .addBox(-3.75F, -4.5F, 0.5F, 0.0F, 3.0F, 6.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-3.5F, -3.5F, -9.0F, 7.0F, 7.0F, 9.0F,
                                new CubeDeformation(0.15F)),
                PartPose.offset(0.0F, 0.0F, 4.5F));

        PartDefinition body1 = head.addOrReplaceChild("body1", CubeListBuilder.create()
                        .texOffs(0, 32)
                        .addBox(-3.5F, -3.0F, 0.0F, 7.0F, 6.0F, 9.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.5F, 4.5F));

        PartDefinition body2 = body1.addOrReplaceChild("body2", CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 7.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.5F, 9.0F));

        body2.addOrReplaceChild("tail", CubeListBuilder.create()
                        .texOffs(32, 12)
                        .addBox(-1.5F, -2.0F, 0.0F, 3.0F, 4.0F, 6.0F, CubeDeformation.NONE)
                        .texOffs(32, 14)
                        .addBox(0.0F, -6.0F, 0.0F, 0.0F, 4.0F, 8.0F, CubeDeformation.NONE)
                        .texOffs(32, 18)
                        .addBox(0.0F, 2.0F, 0.0F, 0.0F, 4.0F, 8.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.5F, 7.0F));

        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    @Override
    public void setupAnim(XiuhcoatlProjectileRenderState state) {
        super.setupAnim(state);
        followRotation(body1, state.xRotation, state.yRotation,
                state.body1XRotation, state.body1YRotation);
        followRotation(body2, state.body1XRotation, state.body1YRotation,
                state.body2XRotation, state.body2YRotation);
        followRotation(tail, state.body2XRotation, state.body2YRotation,
                state.tailXRotation, state.tailYRotation);

        float wave = state.ageInTicks * WAG_SPEED;
        wagSegment(body1, wave, 0, 0.16F, 0.035F);
        wagSegment(body2, wave, 1, 0.23F, 0.055F);
        wagSegment(tail, wave, 2, 0.31F, 0.08F);
    }

    private static void followRotation(ModelPart segment, float parentXRotation, float parentYRotation,
                                       float delayedXRotation, float delayedYRotation) {
        float horizontalBend = Mth.clamp(Mth.wrapDegrees(parentYRotation - delayedYRotation),
                -MAX_HORIZONTAL_BEND, MAX_HORIZONTAL_BEND);
        float verticalBend = Mth.clamp(Mth.wrapDegrees(parentXRotation - delayedXRotation),
                -MAX_VERTICAL_BEND, MAX_VERTICAL_BEND);
        segment.yRot = horizontalBend * Mth.DEG_TO_RAD;
        segment.xRot = verticalBend * Mth.DEG_TO_RAD;
    }

    private static void wagSegment(ModelPart segment, float wave, int index,
                                   float horizontalAmplitude, float verticalAmplitude) {
        float phase = wave - index * WAG_DELAY;
        segment.yRot += Mth.sin(phase) * horizontalAmplitude;
        segment.xRot += Mth.cos(phase * 0.8F) * verticalAmplitude;
    }
}
