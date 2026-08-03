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

public class EagleKnightTalonsModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "eagle_knight_talons"), "main");

    public EagleKnightTalonsModel(ModelPart root) {
        super(root);
        head.visible = false;
        hat.visible = false;
        body.visible = false;
        rightArm.visible = false;
        leftArm.visible = false;
    }

    public static void registerLayerDefinition(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER_LOCATION, EagleKnightTalonsModel::createBodyLayer);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = meshDefinition.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));

        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(1.0F)),
                PartPose.offset(-1.9F, 11.0F, 0.0F));
        PartDefinition rightFur = rightLeg.addOrReplaceChild("right_fur", CubeListBuilder.create(),
                PartPose.offset(0.0F, 9.0F, -2.5F));
        rightFur.addOrReplaceChild("fur_r1", CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-3.0F, 1.9429F, -4.2021F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.8316F, 2.5545F, 0.0F, 1.5708F, -2.618F));
        rightFur.addOrReplaceChild("fur_r2", CubeListBuilder.create().texOffs(-2, 16)
                        .addBox(-3.0F, -2.2148F, -4.0204F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.8316F, 2.5545F, 0.0F, 1.5708F, -0.6545F));
        rightFur.addOrReplaceChild("fur_r3", CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-3.0F, 0.0F, -2.5F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 5.0F, 2.618F, 0.0F, 0.0F));
        rightFur.addOrReplaceChild("fur_r4", CubeListBuilder.create().texOffs(-2, 16)
                        .addBox(-3.0F, 0.0F, -2.5F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.6545F, 0.0F, 0.0F));
        rightFur.addOrReplaceChild("fur_r5", CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-3.0F, 0.0F, -2.5F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -2.0F, 5.0F, 2.618F, 0.0F, 0.0F));
        rightFur.addOrReplaceChild("fur_r6", CubeListBuilder.create().texOffs(-2, 16)
                        .addBox(-3.0F, -2.2148F, -4.0204F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.0F, -4.6684F, 2.5545F, 0.0F, 1.5708F, -2.7053F));
        rightFur.addOrReplaceChild("fur_r7", CubeListBuilder.create().texOffs(-2, 16)
                        .addBox(-3.0F, -2.2148F, -4.0204F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -1.1684F, 2.5545F, 0.0F, 1.5708F, -0.6545F));
        rightFur.addOrReplaceChild("fur_r8", CubeListBuilder.create().texOffs(-2, 16)
                        .addBox(-3.0F, 0.0F, -2.5F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6545F, 0.0F, 0.0F));

        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(0, 0).mirror()
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(1.0F))
                        .mirror(false),
                PartPose.offset(1.9F, 11.0F, 0.0F));
        PartDefinition leftFur = leftLeg.addOrReplaceChild("left_fur", CubeListBuilder.create(),
                PartPose.offset(0.0F, 7.8316F, 0.0545F));
        leftFur.addOrReplaceChild("fur_r9", CubeListBuilder.create().texOffs(0, 16).mirror()
                        .addBox(-3.0F, 1.9429F, -4.2021F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, 0.0F, -1.5708F, 2.618F));
        leftFur.addOrReplaceChild("fur_r10", CubeListBuilder.create().texOffs(-2, 16).mirror()
                        .addBox(-3.0F, -2.2148F, -4.0204F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, 0.0F, -1.5708F, 0.6545F));
        leftFur.addOrReplaceChild("fur_r11", CubeListBuilder.create().texOffs(0, 16).mirror()
                        .addBox(-3.0F, 0.0F, -2.5F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, 1.1684F, 2.4455F, 2.618F, 0.0F, 0.0F));
        leftFur.addOrReplaceChild("fur_r12", CubeListBuilder.create().texOffs(-2, 16).mirror()
                        .addBox(-3.0F, 0.0F, -2.5F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, 1.1684F, -2.5545F, 0.6545F, 0.0F, 0.0F));
        leftFur.addOrReplaceChild("fur_r13", CubeListBuilder.create().texOffs(0, 16).mirror()
                        .addBox(-3.0F, 0.0F, -2.5F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, -0.8316F, -2.5545F, 0.6545F, 0.0F, 0.0F));
        leftFur.addOrReplaceChild("fur_r14", CubeListBuilder.create().texOffs(0, 16).mirror()
                        .addBox(-3.0F, 0.0F, -2.5F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, -0.8316F, 2.4455F, 2.618F, 0.0F, 0.0F));
        leftFur.addOrReplaceChild("fur_r15", CubeListBuilder.create().texOffs(0, 16).mirror()
                        .addBox(-3.0F, -2.2148F, -4.0204F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.6545F));
        leftFur.addOrReplaceChild("fur_r16", CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-3.0F, 1.9429F, -4.2021F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 2.618F));

        return LayerDefinition.create(meshDefinition, 32, 32);
    }
}
