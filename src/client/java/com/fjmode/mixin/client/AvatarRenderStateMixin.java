package com.fjmode.mixin.client;

import com.fjmode.client.render.SwordFlightRenderStateAccess;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements SwordFlightRenderStateAccess {
	@Unique
	private final ItemStackRenderState fjmode$swordFlightSwordState = new ItemStackRenderState();

	@Unique
	private boolean fjmode$swordFlightActive;

	@Override
	public boolean fjmode$isSwordFlightActive() {
		return this.fjmode$swordFlightActive;
	}

	@Override
	public void fjmode$setSwordFlightActive(boolean active) {
		this.fjmode$swordFlightActive = active;
	}

	@Override
	public ItemStackRenderState fjmode$getSwordFlightSwordState() {
		return this.fjmode$swordFlightSwordState;
	}
}
