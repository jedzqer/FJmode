package com.fjmode.mixin;

import com.fjmode.flight.MyriadSwordsController;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {
	@Inject(method = "onHitEntity", at = @At("HEAD"))
	private void fjmode$assignMyriadTargetOnProjectileHit(EntityHitResult entityHitResult, CallbackInfo ci) {
		Projectile projectile = (Projectile) (Object) this;
		Entity hitEntity = entityHitResult.getEntity();
		if (hitEntity == null) {
			return;
		}

		MyriadSwordsController.tryAssignProjectileTarget(projectile.getOwner(), hitEntity);
	}
}
