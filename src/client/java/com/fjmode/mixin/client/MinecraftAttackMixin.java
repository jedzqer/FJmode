package com.fjmode.mixin.client;

import com.fjmode.flight.MyriadSwordsClient;
import com.fjmode.flight.MyriadSwordsController;
import com.fjmode.flight.SwordFlightController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftAttackMixin {
	@Shadow
	private LocalPlayer player;

	@Shadow
	private HitResult hitResult;

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void fjmode$preventSwordFlightClientAttack(CallbackInfoReturnable<Boolean> cir) {
		if (this.player == null || this.hitResult == null) {
			return;
		}

		if (this.hitResult.getType() != HitResult.Type.ENTITY) {
			return;
		}

		boolean hasMyriadTargeting = MyriadSwordsClient.hasActiveSwords(this.player.getUUID());
		if (SwordFlightController.hasSwordFlightWeapon(this.player) && !hasMyriadTargeting) {
			cir.setReturnValue(false);
		}
	}
}
