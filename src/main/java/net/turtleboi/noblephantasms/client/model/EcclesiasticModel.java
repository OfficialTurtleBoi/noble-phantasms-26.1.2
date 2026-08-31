package net.turtleboi.noblephantasms.client.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;

public final class EcclesiasticModel extends IllagerModel<IllagerRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "ecclesiastic"), "main");

    public EcclesiasticModel(ModelPart root) {
        super(root);
    }

    public static void registerLayerDefinition(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER_LOCATION, EcclesiasticModel::createBodyLayer);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(28, 13).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(28, 31).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F)
                .texOffs(0, 13).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 24.0F, 6.0F,
                        new CubeDeformation(0.5F))
                .texOffs(0, 0).addBox(-7.0F, 24.0F, -5.0F, 14.0F, 0.0F, 13.0F), PartPose.ZERO);
        PartDefinition arms = root.addOrReplaceChild("arms", CubeListBuilder.create()
                .texOffs(16, 49).addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F)
                .texOffs(40, 49).addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 2.95F, -1.05F, -0.7505F, 0.0F, 0.0F));
        arms.addOrReplaceChild("mirrored", CubeListBuilder.create()
                        .texOffs(40, 49).mirror().addBox(4.0F, -23.05F, -3.05F, 4.0F, 8.0F, 4.0F)
                        .mirror(false),
                PartPose.offset(0.0F, 21.05F, 1.05F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(0, 43).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F)
                .mirror(false), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 43).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-2.0F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
