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
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;

public final class OniModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "oni"), "main");

    public OniModel(ModelPart root) {
        super(root);
    }

    public static void registerLayerDefinition(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER_LOCATION, OniModel::createBodyLayer);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -2.0F, 0.0F));
        head.addOrReplaceChild("features", CubeListBuilder.create()
                .texOffs(0, 2).addBox(-2.5F, -33.0F, 9.0F, 2.0F, 4.0F, 2.0F,
                        new CubeDeformation(0.1F))
                .texOffs(24, 0).addBox(-0.5F, -33.0F, 15.0F, 4.0F, 4.0F, 4.0F,
                        new CubeDeformation(0.1F))
                .texOffs(0, 2).mirror().addBox(3.5F, -33.0F, 9.0F, 2.0F, 4.0F, 2.0F,
                        new CubeDeformation(0.1F)).mirror(false)
                .texOffs(24, 1).addBox(-1.5F, -26.0F, 8.0F, 1.0F, 2.0F, 1.0F,
                        new CubeDeformation(0.1F)), PartPose.offset(-1.5F, 23.0F, -13.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                        new CubeDeformation(0.5F)), PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        body.addOrReplaceChild("upper_body", CubeListBuilder.create()
                .texOffs(28, 39).addBox(-6.0F, -24.0F, -3.0F, 12.0F, 5.0F, 6.0F,
                        new CubeDeformation(0.3F))
                .texOffs(28, 50).addBox(-6.0F, -24.0F, -3.0F, 12.0F, 8.0F, 6.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));
        body.addOrReplaceChild("lower_body", CubeListBuilder.create()
                .texOffs(34, 25).addBox(-5.0F, -18.0F, -3.0F, 10.0F, 6.0F, 5.0F)
                .texOffs(34, 16).addBox(-5.0F, -16.0F, -3.0F, 10.0F, 4.0F, 5.0F,
                        new CubeDeformation(0.5F))
                .texOffs(31, 18).addBox(-2.0F, -11.5F, -3.5F, 4.0F, 3.0F, 0.0F),
                PartPose.offset(0.0F, 24.0F, 0.5F));

        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(0, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 5.0F, 13.0F, 5.0F)
                .mirror(false)
                .texOffs(18, 31).mirror().addBox(-1.0F, 6.0F, -2.0F, 5.0F, 3.0F, 5.0F,
                        new CubeDeformation(0.25F)).mirror(false),
                PartPose.offset(7.0F, 0.0F, -0.5F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4.0F, -2.0F, -2.0F, 5.0F, 13.0F, 5.0F)
                .texOffs(18, 31).addBox(-4.0F, 6.0F, -2.0F, 5.0F, 3.0F, 5.0F,
                        new CubeDeformation(0.25F)), PartPose.offset(-7.0F, 0.0F, -0.5F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 47).mirror().addBox(-1.9F, 0.0F, -3.0F, 5.0F, 12.0F, 5.0F)
                .mirror(false)
                .texOffs(0, 36).mirror().addBox(-1.9F, 0.0F, -3.0F, 5.0F, 5.0F, 5.0F,
                        new CubeDeformation(0.25F)).mirror(false),
                PartPose.offset(1.9F, 12.0F, 0.5F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 47).addBox(-3.1F, 0.0F, -3.0F, 5.0F, 12.0F, 5.0F)
                .texOffs(0, 36).addBox(-3.1F, 0.0F, -3.0F, 5.0F, 5.0F, 5.0F,
                        new CubeDeformation(0.25F)), PartPose.offset(-1.9F, 12.0F, 0.5F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
