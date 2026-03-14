package com.fjmode.mixin.client;

import com.fjmode.client.render.SwordFlightRenderStateAccess;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class PlayerModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void fjmode$forceStandingPose(AvatarRenderState renderState, CallbackInfo ci) {
		if (!((SwordFlightRenderStateAccess) renderState).fjmode$isSwordFlightActive()) {
			return;
		}

		PlayerModel model = (PlayerModel) (Object) this;
		model.body.xRot = 0.0F;
		model.body.yRot = 0.0F;
		model.body.zRot = 0.0F;
		model.rightArm.xRot = 0.0F;
		model.rightArm.yRot = 0.0F;
		model.rightArm.zRot = 0.0F;
		model.leftArm.xRot = 0.0F;
		model.leftArm.yRot = 0.0F;
		model.leftArm.zRot = 0.0F;
		model.rightLeg.xRot = 0.0F;
		model.rightLeg.yRot = 0.0F;
		model.rightLeg.zRot = 0.0F;
		model.leftLeg.xRot = 0.0F;
		model.leftLeg.yRot = 0.0F;
		model.leftLeg.zRot = 0.0F;
	}
}
