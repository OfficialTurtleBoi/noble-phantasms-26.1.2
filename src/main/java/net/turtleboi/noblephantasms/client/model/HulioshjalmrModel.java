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

public class HulioshjalmrModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "hulioshjalmr"), "main");

    public HulioshjalmrModel(ModelPart root) {
        super(root);
    }

    public static void registerLayerDefinition(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER_LOCATION, HulioshjalmrModel::createBodyLayer);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = meshDefinition.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 17)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.0F)),
                PartPose.ZERO);
        head.addOrReplaceChild("top_ornament", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -3.0F, -7.0F, 4.0F, 3.0F, 14.0F, new CubeDeformation(-0.0001F))
                .texOffs(36, 13)
                .addBox(-2.0F, 0.0F, -7.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(-0.0001F))
                .texOffs(48, 14)
                .addBox(-2.0F, 2.0F, 5.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(-0.0001F))
                .texOffs(36, 13)
                .addBox(-2.0F, 0.0F, 5.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(-0.0001F))
                .texOffs(48, 14)
                .addBox(-2.0F, 2.0F, -6.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(-0.0001F)),
                PartPose.offset(0.0F, -9.0F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(meshDefinition, 64, 64);
    }
}
