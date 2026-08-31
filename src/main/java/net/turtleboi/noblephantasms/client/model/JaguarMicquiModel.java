package net.turtleboi.noblephantasms.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.renderer.JaguarMicquiRenderState;

public final class JaguarMicquiModel extends HumanoidModel<JaguarMicquiRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "jaguar_micqui"), "main");
    public static final ModelLayerLocation EYES_LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "jaguar_micqui"), "eyes");

    public JaguarMicquiModel(ModelPart root) {
        super(root);
    }

    public static void registerLayerDefinition(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER_LOCATION, JaguarMicquiModel::createBodyLayer);
        event.registerLayerDefinition(EYES_LAYER_LOCATION, JaguarMicquiModel::createEyesLayer);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        PartDefinition features = head.addOrReplaceChild("features", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-7.0F, -33.0F, 0.0F, 3.0F, 3.0F, 2.0F)
                .texOffs(56, 5).addBox(-2.0F, -34.0F, 2.0F, 4.0F, 3.0F, 3.0F)
                .texOffs(56, 0).mirror().addBox(4.0F, -33.0F, 0.0F, 3.0F, 3.0F, 2.0F).mirror(false),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        features.addOrReplaceChild("feather_base_r1", CubeListBuilder.create()
                .texOffs(70, 0).addBox(-5.0F, -10.0F, -1.0F, 10.0F, 10.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, -33.5F, 4.5F, -0.4363F, 0.0F, 0.0F));
        features.addOrReplaceChild("eyes", CubeListBuilder.create()
                .texOffs(64, 10).addBox(-4.0F, -32.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                        new CubeDeformation(0.1F)), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4.0F, -32.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                        new CubeDeformation(0.5F)), PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)
                .texOffs(32, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 6.0F, 4.0F,
                        new CubeDeformation(0.25F))
                .texOffs(32, 10).addBox(-4.0F, 10.0F, -2.0F, 8.0F, 2.0F, 4.0F,
                        new CubeDeformation(0.4F)), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(40, 32).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F)
                .texOffs(0, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                        new CubeDeformation(0.25F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(32, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F)
                .texOffs(24, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F,
                        new CubeDeformation(0.25F))
                .texOffs(56, 32).addBox(-3.0F, 2.0F, -2.0F, 4.0F, 8.0F, 4.0F,
                        new CubeDeformation(0.125F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(32, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F)
                .texOffs(48, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                        new CubeDeformation(0.25F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F)
                .texOffs(48, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                        new CubeDeformation(0.25F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition createEyesLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition features = head.addOrReplaceChild("features", CubeListBuilder.create(),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        features.addOrReplaceChild("eyes", CubeListBuilder.create()
                .texOffs(64, 10).addBox(-4.0F, -32.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                        new CubeDeformation(0.1F)), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }
}
