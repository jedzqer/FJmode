package com.fjmode.mixin;

import com.fjmode.flight.SwordFlightController;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerAttackMixin {
	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	private void fjmode$preventSwordFlightAttacks(Entity target, CallbackInfo ci) {
		Player player = (Player) (Object) this;
		if (SwordFlightController.getSwordFlightLevel(player) > 0) {
			ci.cancel();
		}
	}
}
