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

public final class AnubiteModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "anubite"), "main");

    public AnubiteModel(ModelPart root) {
        super(root);
    }

    public static void registerLayerDefinition(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER_LOCATION, AnubiteModel::createBodyLayer);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -2.0F, 0.0F));
        head.addOrReplaceChild("features", CubeListBuilder.create()
                .texOffs(0, 57).addBox(-1.0F, -28.0F, 1.0F, 5.0F, 4.0F, 3.0F)
                .texOffs(16, 54).addBox(-3.5F, -38.0F, 8.0F, 3.0F, 9.0F, 1.0F)
                .texOffs(16, 54).mirror().addBox(3.5F, -38.0F, 8.0F, 3.0F, 9.0F, 1.0F).mirror(false),
                PartPose.offset(-1.5F, 24.0F, -8.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                        new CubeDeformation(0.5F)), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 36).addBox(-4.0F, 6.0F, -2.0F, 8.0F, 6.0F, 4.0F)
                .texOffs(28, 50).addBox(-6.0F, -2.0F, -3.0F, 12.0F, 8.0F, 6.0F)
                .texOffs(16, 16).addBox(-4.0F, -2.0F, -3.0F, 8.0F, 4.0F, 6.0F,
                        new CubeDeformation(0.3F))
                .texOffs(20, 26).addBox(-4.0F, 8.0F, -2.0F, 8.0F, 4.0F, 4.0F,
                        new CubeDeformation(0.5F)), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(48, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F).mirror(false)
                .texOffs(48, 33).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F,
                        new CubeDeformation(0.25F)).mirror(false), PartPose.offset(7.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(48, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F)
                .texOffs(48, 33).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F,
                        new CubeDeformation(0.25F)), PartPose.offset(-7.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 16).mirror().addBox(-1.9F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F).mirror(false)
                .texOffs(0, 32).mirror().addBox(-1.9F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                        new CubeDeformation(0.25F)).mirror(false), PartPose.offset(1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2.1F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F)
                .texOffs(0, 32).addBox(-2.1F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                        new CubeDeformation(0.25F)), PartPose.offset(-1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
