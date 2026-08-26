package net.turtleboi.noblephantasms.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.renderer.states.YasakaniGuardianRenderState;

public final class YasakaniGuardianModel extends HumanoidModel<YasakaniGuardianRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "yasakani_guardian"), "main");

    public YasakaniGuardianModel(ModelPart root) {
        super(root);
    }

    public static void registerLayerDefinition(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER_LOCATION, YasakaniGuardianModel::createBodyLayer);
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64);
    }

    @Override
    public void setupAnim(YasakaniGuardianRenderState state) {
        super.setupAnim(state);
        float drift = Mth.sin(state.ageInTicks * 0.12F);
        rightArm.zRot += 0.12F + drift * 0.025F;
        leftArm.zRot -= 0.12F + drift * 0.025F;
        if (state.attackTime <= 0.001F) {
            rightArm.xRot = rightArm.xRot * 0.3F - 0.22F + drift * 0.06F;
            leftArm.xRot = leftArm.xRot * 0.3F - 0.22F - drift * 0.06F;
        }
        rightLeg.xRot = 0.18F + drift * 0.06F;
        leftLeg.xRot = 0.18F - drift * 0.06F;
        rightLeg.zRot = 0.035F;
        leftLeg.zRot = -0.035F;
    }
}
