package com.fjmode.mixin.client;

import com.fjmode.client.render.SwordFlightRenderStateAccess;
import com.fjmode.client.render.SwordFlightSwordLayer;
import com.fjmode.flight.SwordFlightController;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Final;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin<AvatarlikeEntity extends Avatar & ClientAvatarEntity> {
	@Shadow
	protected abstract boolean addLayer(RenderLayer<AvatarRenderState, PlayerModel> renderLayer);

	@Shadow
	@Final
	protected ItemModelResolver itemModelResolver;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void fjmode$addSwordFlightLayer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
		this.addLayer(new SwordFlightSwordLayer((AvatarRenderer<?>) (Object) this));
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void fjmode$extractSwordFlightState(AvatarlikeEntity entity, AvatarRenderState renderState, float partialTick, CallbackInfo ci) {
		SwordFlightRenderStateAccess swordFlightState = (SwordFlightRenderStateAccess) renderState;
		swordFlightState.fjmode$getSwordFlightSwordState().clear();
		boolean active = entity.isFallFlying() && SwordFlightController.canUseSwordFlight(entity);
		swordFlightState.fjmode$setSwordFlightActive(active);

		if (active) {
			this.itemModelResolver.updateForLiving(
				swordFlightState.fjmode$getSwordFlightSwordState(),
				entity.getMainHandItem(),
				ItemDisplayContext.FIXED,
				entity
			);
		}
	}
}
