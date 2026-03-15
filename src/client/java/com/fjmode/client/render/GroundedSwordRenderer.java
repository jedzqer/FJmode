package com.fjmode.client.render;

import com.fjmode.entity.GroundedSwordEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;

public final class GroundedSwordRenderer extends EntityRenderer<GroundedSwordEntity, GroundedSwordRenderer.State> {
	public GroundedSwordRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(GroundedSwordEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.item.clear();
		Minecraft.getInstance().getItemModelResolver().updateForNonLiving(
			state.item,
			entity.getSwordStack(),
			ItemDisplayContext.FIXED,
			entity
		);
		state.xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
		state.yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
		poseStack.pushPose();
		poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(state.yRot - 90.0F)));
		poseStack.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(state.xRot + 135.0F)));
		poseStack.scale(1.35F, 1.35F, 1.35F);
		state.item.submit(poseStack, submitNodeCollector, state.lightCoords, 0, 0);
		poseStack.popPose();
		super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
	}

	public static final class State extends EntityRenderState {
		private final ItemStackRenderState item = new ItemStackRenderState();
		private float xRot;
		private float yRot;
	}
}
