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
import net.turtleboi.noblephantasms.entity.renderer.DraugrRenderState;

public final class DraugrModel extends HumanoidModel<DraugrRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "draugr"), "main");

    public DraugrModel(ModelPart root) {
        super(root);
    }

    public static void registerLayerDefinition(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER_LOCATION, DraugrModel::createBodyLayer);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                        new CubeDeformation(-0.1F)), PartPose.ZERO);
        head.addOrReplaceChild("eyes", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-4.0F, -32.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                        new CubeDeformation(-0.25F)), PartPose.offset(0.0F, 24.0F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, 8.0F, -2.0F, 8.0F, 4.0F, 4.0F)
                .texOffs(56, 59).addBox(-1.0F, 6.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(32, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 6.0F, 4.0F)
                .texOffs(4, 9).addBox(-4.0F, 9.0F, -2.0F, 8.0F, 3.0F, 4.0F,
                        new CubeDeformation(0.5F))
                .texOffs(32, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 8.0F, 4.0F,
                        new CubeDeformation(0.3F)), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F)
                .texOffs(72, 0).addBox(-1.0F, 4.0F, -2.0F, 4.0F, 6.0F, 4.0F)
                .texOffs(32, 32).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F,
                        new CubeDeformation(0.25F))
                .texOffs(56, 57).addBox(0.0F, 2.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(72, 10).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F)
                .texOffs(0, 48).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                        new CubeDeformation(0.25F))
                .texOffs(56, 0).addBox(-3.0F, 2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
                .texOffs(56, 63).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(16, 48).addBox(-1.9F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F)
                .texOffs(32, 48).addBox(-1.9F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                        new CubeDeformation(0.25F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(48, 48).addBox(-2.1F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F)
                .texOffs(48, 32).addBox(-2.1F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                        new CubeDeformation(0.25F))
                .texOffs(56, 24).addBox(-2.1F, 9.0F, -2.0F, 4.0F, 3.0F, 4.0F)
                .texOffs(48, 57).addBox(-1.1F, 3.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }
}
