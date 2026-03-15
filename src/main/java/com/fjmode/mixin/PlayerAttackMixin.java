package com.fjmode.mixin;

import com.fjmode.flight.MyriadSwordsController;
import com.fjmode.flight.SwordFlightController;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerAttackMixin {
	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	private void fjmode$preventSwordFlightAttacks(Entity target, CallbackInfo ci) {
		Player player = (Player) (Object) this;
		boolean assignedTarget = MyriadSwordsController.tryAssignTarget(player, target);
		if (SwordFlightController.hasSwordFlightWeapon(player)) {
			if (assignedTarget || MyriadSwordsController.hasActiveSwords(player)) {
				ci.cancel();
				return;
			}
			ci.cancel();
		}
	}

	@Inject(method = "getCurrentItemAttackStrengthDelay", at = @At("RETURN"), cancellable = true)
	private void fjmode$disableSwordFlightSweepCharge(CallbackInfoReturnable<Float> cir) {
		Player player = (Player) (Object) this;
		if (SwordFlightController.hasSwordFlightWeapon(player)) {
			cir.setReturnValue(Float.MAX_VALUE);
		}
	}
}
