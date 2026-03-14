package com.fjmode.mixin;

import com.fjmode.flight.SwordFlightController;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "canGlide", at = @At("HEAD"), cancellable = true)
	private void fjmode$allowSwordFlight(CallbackInfoReturnable<Boolean> cir) {
		Player player = (Player) (Object) this;
		if (SwordFlightController.canUseSwordFlight(player)) {
			cir.setReturnValue(true);
		}
	}
}
