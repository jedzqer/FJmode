package com.fjmode.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Quaternionf;

public final class SwordFlightSwordLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	public SwordFlightSwordLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, AvatarRenderState renderState, float yRot, float xRot) {
		SwordFlightRenderStateAccess swordFlightState = (SwordFlightRenderStateAccess) renderState;
		if (!swordFlightState.fjmode$isSwordFlightActive()) {
			return;
		}

		poseStack.pushPose();
		poseStack.translate(0.0F, 1.55F, 0.0F);
		poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(180.0F - renderState.yRot)));
		poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(renderState.xRot)));
		poseStack.translate(0.0F, 0.35F, 0.0F);
		poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(90.0F)));
		poseStack.scale(1.6F, 1.6F, 1.6F);
		swordFlightState.fjmode$getSwordFlightSwordState().submit(poseStack, submitNodeCollector, packedLight, 0, 0);
		poseStack.popPose();
	}
}
